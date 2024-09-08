// Copyright 2020 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.bazel.repository.downloader;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.auth.Credentials;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.authandtls.NetrcCredentials;
import com.google.devtools.build.lib.authandtls.NetrcParser;
import com.google.devtools.build.lib.util.OS;

import net.starlark.java.syntax.Location;

/**
 * Helper class for taking URLs and converting them according to an optional config specified by
 * {@link com.google.devtools.build.lib.bazel.repository.RepositoryOptions#downloaderConfig}.
 *
 * <p>
 * The primary reason for doing this is to allow a bazel user to redirect particular URLs to (eg.) local mirrors without
 * needing to rewrite third party rulesets.
 */
public class UrlRewriter {

    /**
     * Holds the URL along with meta-info, such as whether URL was re-written or not.
     */
    @AutoValue
    public abstract static class RewrittenURL {
        static RewrittenURL create(URL url, boolean rewritten) {
            return new AutoValue_UrlRewriter_RewrittenURL(rewritten, url);
        }

        public abstract boolean rewritten();

        public abstract URL url();
    }

    private static final ImmutableSet<String> REWRITABLE_SCHEMES = ImmutableSet.of("http", "https");

    /**
     * Obtain a new {@code UrlRewriter} configured with the specified config file.
     *
     * @param configPath
     *            Path to the config file to use. May be null.
     */
    public static UrlRewriter getDownloaderUrlRewriter(String configPath) throws UrlRewriterParseException {
        // "empty" UrlRewriter shouldn't alter auth headers
        if (Strings.isNullOrEmpty(configPath)) {
            return new UrlRewriter("", new StringReader(""));
        }

        try (var reader = Files.newBufferedReader(Paths.get(configPath))) {
            return new UrlRewriter(configPath, reader);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isMatchingHostName(URL url, String host) {
        return host.equals(url.getHost()) || url.getHost().endsWith("." + host);
    }

    /**
     * Create a new {@link Credentials} object by parsing the .netrc file with following order to search it:
     *
     * <ol>
     * <li>If environment variable $NETRC exists, use it as the path to the .netrc file
     * <li>Fallback to $HOME/.netrc or $USERPROFILE/.netrc
     * </ol>
     *
     * @return the {@link Credentials} object or {@code null} if there is no .netrc file.
     * @throws UrlRewriterParseException
     *             in case the credentials can't be constructed.
     */
    // TODO : consider re-using RemoteModule.newCredentialsFromNetrc
    @Nullable
    public static Credentials newCredentialsFromNetrc(
            Map<String, String> clientEnv,
            Path workingDirectory) throws UrlRewriterParseException {
        final Optional<String> homeDir;
        if (OS.getCurrent() == OS.WINDOWS) {
            homeDir = Optional.ofNullable(clientEnv.get("USERPROFILE"));
        } else {
            homeDir = Optional.ofNullable(clientEnv.get("HOME"));
        }
        final var netrcFileString = Optional.ofNullable(clientEnv.get("NETRC"))
                .orElseGet(() -> homeDir.map(home -> home + "/.netrc").orElse(null));
        if (netrcFileString == null) {
            return null;
        }
        final var location = Location.fromFileLineColumn(netrcFileString, 0, 0);
        // In case Bazel is not started from a valid workspace.
        if (workingDirectory == null) {
            return null;
        }
        // Using the getRelative() method ensures:
        // - If netrcFileString is an absolute path, use as it is.
        // - If netrcFileString is a relative path, it's resolved to an absolute path
        // with the current
        // working directory.
        var netrcFile = Path.of(netrcFileString);
        if (!netrcFile.isAbsolute()) {
            netrcFile = workingDirectory.resolve(netrcFile);
        }
        if (!Files.isRegularFile(netrcFile)) {
            return null;
        }
        try {
            final var netrc = NetrcParser.parseAndClose(Files.newInputStream(netrcFile));
            return new NetrcCredentials(netrc);
        } catch (final IOException e) {
            throw new UrlRewriterParseException("Failed to parse " + netrcFile + ": " + e.getMessage(), location);
        }
    }

    /**
     * Prefixes url with protocol if not already prefixed by {@link #REWRITABLE_SCHEMES}
     */
    private static URL prefixWithProtocol(String url, String protocol) {
        try {
            for (final String schemaPrefix : REWRITABLE_SCHEMES) {
                if (url.startsWith(schemaPrefix + "://")) {
                    return new URL(url);
                }
            }
            return new URL(protocol + "://" + url);
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private final UrlRewriterConfig config;

    private final Function<URL, List<RewrittenURL>> rewriter;

    @VisibleForTesting
    UrlRewriter(String filePathForErrorReporting, Reader reader) throws UrlRewriterParseException {
        Preconditions.checkNotNull(reader, "UrlRewriterConfig source must be set");
        config = new UrlRewriterConfig(filePathForErrorReporting, reader);

        rewriter = this::rewrite;
    }

    /**
     * Rewrites {@code urls} using the configuration provided to {@link #getDownloaderUrlRewriter(String, Reporter)}.
     * The returned list of URLs may be empty if the configuration used blocks all the input URLs.
     *
     * @param urls
     *            The input list of {@link URL}s. May be empty.
     * @return The amended lists of URLs.
     */
    public ImmutableList<RewrittenURL> amend(List<URL> urls) {
        Objects.requireNonNull(urls, "URLS to check must be set but may be empty");

        return urls.stream().map(rewriter).flatMap(Collection::stream).collect(toImmutableList());
    }

    private ImmutableList<RewrittenURL> applyRewriteRules(URL url) {
        final var withoutScheme = url.toString().substring(url.getProtocol().length() + 3);

        final ImmutableSet.Builder<String> rewrittenUrls = ImmutableSet.builder();

        var matchMade = false;
        for (final Map.Entry<Pattern, Collection<String>> entry : config.getRewrites().entrySet()) {
            final var matcher = entry.getKey().matcher(withoutScheme);
            if (matcher.matches()) {
                matchMade = true;

                for (final String replacement : entry.getValue()) {
                    rewrittenUrls.add(matcher.replaceFirst(replacement));
                }
            }
        }

        if (!matchMade) {
            return ImmutableList.of(RewrittenURL.create(url, false));
        }

        return rewrittenUrls.build()
                .stream()
                .map(urlString -> prefixWithProtocol(urlString, url.getProtocol()))
                .map(plainUrl -> RewrittenURL.create(plainUrl, true))
                .collect(toImmutableList());
    }

    @Nullable
    public String getAllBlockedMessage() {
        return config.getAllBlockedMessage();
    }

    public UrlRewriterConfig getConfig() {
        return config;
    }

    private boolean isAllowMatched(URL url) {
        for (final String host : config.getAllowList()) {
            if (isMatchingHostName(url, host)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlockMatched(URL url) {
        for (final String host : config.getBlockList()) {
            // Allow a wild-card block
            if ("*".equals(host) || isMatchingHostName(url, host)) {
                return true;
            }
        }
        return false;
    }

    private ImmutableList<RewrittenURL> rewrite(URL url) {
        Preconditions.checkNotNull(url);

        // Cowardly refuse to rewrite non-HTTP(S) urls
        if (REWRITABLE_SCHEMES.stream().noneMatch(scheme -> Ascii.equalsIgnoreCase(scheme, url.getProtocol()))) {
            return ImmutableList.of(RewrittenURL.create(url, false));
        }

        final var rewrittenUrls = applyRewriteRules(url);

        final ImmutableList.Builder<RewrittenURL> toReturn = ImmutableList.builder();
        // Now iterate over the URLs
        for (final RewrittenURL consider : rewrittenUrls) {
            // If there's an allow entry, add it to the set to return and continue
            if (isAllowMatched(consider.url())) {
                toReturn.add(consider);
                continue;
            }

            // If there's no block that matches the domain, add it to the set to return and
            // continue
            if (!isBlockMatched(consider.url())) {
                toReturn.add(consider);
            }
        }

        return toReturn.build();
    }

    /**
     * Updates {@code authHeaders} using the userInfo available in the provided {@code urls}. Note that if the same url
     * is present in both {@code authHeaders} and <b>download config</b> then it will be overridden with the value from
     * <b>download config</b>.
     *
     * @param urls
     *            The input list of {@link URL}s. May be empty.
     * @param authHeaders
     *            A map of the URLs and their corresponding auth tokens.
     * @return A map of the updated authentication headers.
     */
    public Map<URI, Map<String, List<String>>> updateAuthHeaders(
            List<RewrittenURL> urls,
            Map<URI, Map<String, List<String>>> authHeaders,
            Credentials netrcCreds) {
        final Map<URI, Map<String, List<String>>> updatedAuthHeaders = new HashMap<>(authHeaders);

        for (final RewrittenURL url : urls) {
            // if URL was not re-written by UrlRewriter in first place, we should not attach
            // auth headers
            // to it
            if (!url.rewritten()) {
                continue;
            }

            final var userInfo = url.url().getUserInfo();
            if (userInfo != null) {
                try {
                    final var token = "Basic " + Base64.getEncoder().encodeToString(userInfo.getBytes(ISO_8859_1));
                    updatedAuthHeaders
                            .put(url.url().toURI(), ImmutableMap.of("Authorization", ImmutableList.of(token)));
                } catch (final URISyntaxException e) {
                    // If the credentials extraction failed, we're letting bazel try without
                    // credentials.
                }
            } else if (netrcCreds != null) {
                try {
                    final var urlAuthHeaders = netrcCreds.getRequestMetadata(url.url().toURI());
                    if ((urlAuthHeaders == null) || urlAuthHeaders.isEmpty()) {
                        continue;
                    }
                    // there could be multiple Auth headers, take the first one
                    final var firstAuthHeader = urlAuthHeaders.entrySet().stream().findFirst().get();
                    if ((firstAuthHeader.getValue() != null) && !firstAuthHeader.getValue().isEmpty()) {
                        updatedAuthHeaders.put(
                            url.url().toURI(),
                            ImmutableMap
                                    .of(firstAuthHeader.getKey(), ImmutableList.of(firstAuthHeader.getValue().get(0))));
                    }
                } catch (URISyntaxException | IOException e) {
                    // If the credentials extraction failed, we're letting bazel try without
                    // credentials.
                }
            }
        }

        return ImmutableMap.copyOf(updatedAuthHeaders);
    }
}