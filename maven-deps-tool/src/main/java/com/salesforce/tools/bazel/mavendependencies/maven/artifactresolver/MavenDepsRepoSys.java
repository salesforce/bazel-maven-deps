package com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;

import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.RepositorySystemLifecycle;
import org.eclipse.aether.internal.impl.synccontext.named.NameMapper;
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapterFactory;
import org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapterFactoryImpl;
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.named.providers.NoopNamedLockFactory;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ConservativeAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.slf4j.Logger;

import com.google.devtools.build.lib.authandtls.Netrc;
import com.google.devtools.build.lib.authandtls.NetrcCredentials;
import com.google.devtools.build.lib.bazel.repository.downloader.UrlRewriter;
import com.google.devtools.build.lib.bazel.repository.downloader.UrlRewriterParseException;
import com.salesforce.tools.bazel.cli.helper.ProgressMonitor;
import com.salesforce.tools.bazel.cli.helper.UnifiedLogger;

/**
 * A rem to use Maven's artifact resolver outside of Maven.
 * <p>
 * Copied and modified from <code>org.apache.maven.resolver.internal.ant.AntRepoSys</code>
 * </p>
 *
 * @see https://github.com/apache/maven-resolver-ant-tasks/blob/5ce206cb1793ee7f4ee4593d2ac03e65c9d61ebb/src/main/java/org/apache/maven/resolver/internal/ant/AntRepoSys.java#L115
 */
@SuppressWarnings("deprecation")
public class MavenDepsRepoSys {

    private static final org.eclipse.aether.repository.RepositoryPolicy POLICY_ENABLED_NO_UPDATES_CHECKSUM_FAIL =
            new org.eclipse.aether.repository.RepositoryPolicy(
                    true,
                    org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER,
                    org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_FAIL);
    private static final org.eclipse.aether.repository.RepositoryPolicy POLICY_DISABLED =
            new org.eclipse.aether.repository.RepositoryPolicy(
                    false,
                    org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_NEVER,
                    org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_FAIL);

    private static final boolean OS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("windows");

    private static final Logger LOG = UnifiedLogger.getLogger();

    private static final SettingsBuilder SETTINGS_BUILDER = new DefaultSettingsBuilderFactory().newInstance();

    private RepositorySystem repoSys;
    private Settings settings;
    private final RepositorySystemSupplier repositorySystemSupplier;
    private final DefaultModelBuilder modelBuilder;
    private final SortedSet<String> mavenRepositories;
    private final Path downloaderConfig;
    private final String mavenCenteralUrl;
    private Netrc netrc;
    private boolean netrcLoaded;
    private UrlRewriter urlRewriter;
    private final Path localMavenRepository;

    public MavenDepsRepoSys(Path downloaderConfig, String mavenCenteralUrl, SortedSet<String> mavenRepositories,
            Path localMavenRepository) {
        this.downloaderConfig = downloaderConfig;
        this.localMavenRepository = localMavenRepository != null ? localMavenRepository
                : Path.of(System.getProperty("user.home"), ".m2", "repository");
        this.mavenCenteralUrl = Objects.requireNonNull(mavenCenteralUrl, "Please provide a Maven Central URL!");
        this.mavenRepositories = mavenRepositories != null ? mavenRepositories : Collections.emptySortedSet();
        modelBuilder = new DefaultModelBuilderFactory().newInstance();

        repositorySystemSupplier = new RepositorySystemSupplier() {
            @Override
            protected ModelBuilder getModelBuilder() {
                return modelBuilder;
            }

            @Override
            protected NamedLockFactoryAdapterFactory getNamedLockFactoryAdapterFactory(
                    Map<String, NamedLockFactory> namedLockFactories,
                    Map<String, NameMapper> nameMappers,
                    RepositorySystemLifecycle repositorySystemLifecycle) {
                return new NamedLockFactoryAdapterFactoryImpl(
                        namedLockFactories,
                        NoopNamedLockFactory.NAME,
                        nameMappers,
                        "gav",
                        repositorySystemLifecycle);
            }
        };
    }

    private void checkForCommonIssues(Throwable e) throws IllegalStateException {
        final var original = e;
        final Set<Throwable> seen = new HashSet<>();
        while ((e != null) && !seen.contains(e)) {

            if (e instanceof ArtifactTransferException) {
                var transferException = (ArtifactTransferException) e;
                if (transferException.getMessage().contains("Unauthorized")) {
                    throw new IllegalStateException(
                            format(
                                "********ERROR********%n%n%s%nPlease check your Maven settings. There was an authentication problem with repository '%s'.%n%n",
                                e.getMessage(),
                                transferException.getRepository()),
                            original);
                }

                if (transferException.getMessage().contains("repo1.maven.org")) {
                    throw new IllegalStateException(
                            format(
                                "********ERROR********%n%n%s%nPlease check your Maven settings. The default Maven repository (repo1.maven.org) might not be a good fit.%nRepository: %s%n%n",
                                e.getMessage(),
                                transferException.getRepository()),
                            original);
                }
            }

            seen.add(e);
            e = e.getCause();
        }

    }

    public CollectResult collectDependencies(
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            ProgressMonitor monitor) {
        final var session = newSession(monitor);

        final var repos = getRemoteRepositories(session);

        final var collectRequest = new CollectRequest();
        collectRequest.setRequestContext("project");

        for (final RemoteRepository repo : repos) {
            collectRequest.addRepository(repo);
        }

        collectRequest.setDependencies(dependencies);
        collectRequest.setManagedDependencies(managedDependencies);

        LOG.debug("Collecting dependencies");

        CollectResult result;
        try {
            result = getSystem().collectDependencies(session, collectRequest);
        } catch (final DependencyCollectionException e) {
            checkForCommonIssues(e);
            throw new IllegalStateException("Could not collect dependencies: " + e.getMessage(), e);
        }

        return result;
    }

    public List<ArtifactResult> downloadArtifacts(
            Collection<Artifact> artifacts,
            ProgressMonitor monitor) throws ArtifactResolutionException {
        final var session = newSession(monitor);

        final var repositories = getRemoteRepositories(session);

        return getSystem().resolveArtifacts(
            session,
            artifacts.stream().map(a -> new ArtifactRequest(a, repositories, null)).collect(toList()));
    }

    private AuthenticationSelector getAuthSelector() {
        final var selector = new DefaultAuthenticationSelector();

        final var settings = getSettings();
        for (final Server server : settings.getServers()) {
            final var auth = new AuthenticationBuilder();
            auth.addUsername(server.getUsername()).addPassword(server.getPassword());
            auth.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            selector.add(server.getId(), auth.build());
        }

        return new ConservativeAuthenticationSelector(selector);
    }

    private Properties getEnvProperties(Properties props) {
        if (props == null) {
            props = new Properties();
        }
        final var envCaseInsensitive = OS_WINDOWS;
        for (final Map.Entry<String, String> entry : System.getenv().entrySet()) {
            var key = entry.getKey();
            if (envCaseInsensitive) {
                key = key.toUpperCase(Locale.ENGLISH);
            }
            key = "env." + key;
            props.put(key, entry.getValue());
        }
        return props;
    }

    private Map<String, String> getHttpHeaders(Xpp3Dom dom) {
        final Map<String, String> headers = new HashMap<>();
        for (var i = 0; i < dom.getChildCount(); i++) {
            final var child = dom.getChild(i);
            final var name = child.getChild("name");
            final var value = child.getChild("value");
            if ((name != null) && (name.getValue() != null)) {
                headers.put(name.getValue(), (value != null) ? value.getValue() : null);
            }
        }
        return Collections.unmodifiableMap(headers);
    }

    private File getLocalRepoDir() {
        final var settings = getSettings();
        if (settings.getLocalRepository() != null) {
            return new File(settings.getLocalRepository());
        }

        return localMavenRepository.toFile();
    }

    private LocalRepositoryManager getLocalRepoMan(RepositorySystemSession session) {
        final var repo = new LocalRepository(getLocalRepoDir());

        return getSystem().newLocalRepositoryManager(session, repo);
    }

    private MirrorSelector getMirrorSelector() {
        final var selector = new DefaultMirrorSelector();

        final Set<String> serverIds = new HashSet<>();

        final var settings = getSettings();
        for (final org.apache.maven.settings.Mirror mirror : settings.getMirrors()) {
            selector.add(
                String.valueOf(mirror.getId()),
                mirror.getUrl(),
                mirror.getLayout(),
                false,
                mirror.getMirrorOf(),
                mirror.getMirrorOfLayouts());
            serverIds.add(String.valueOf(mirror.getId()));
        }

        return selector;
    }

    private Netrc getNetrc() {
        var netrc = this.netrc;
        if ((netrc != null) || netrcLoaded) {
            return netrc;
        }
        try {
            final var creds =
                    UrlRewriter.newCredentialsFromNetrc(System.getenv(), Path.of(System.getProperty("user.dir")));
            netrc = this.netrc = creds instanceof NetrcCredentials ? ((NetrcCredentials) creds).getNetrc() : null;
            netrcLoaded = true;
            return netrc;
        } catch (final UrlRewriterParseException e) {
            throw new IllegalArgumentException("Error reading .netrc file: " + e.getMessage(), e);
        }
    }

    private ProxySelector getProxySelector() {
        final var selector = new DefaultProxySelector();

        final var settings = getSettings();
        for (final org.apache.maven.settings.Proxy proxy : settings.getProxies()) {
            final var auth = new AuthenticationBuilder();
            auth.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
            selector.add(
                new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth.build()),
                proxy.getNonProxyHosts());
        }

        return selector;
    }

    private List<RemoteRepository> getRemoteRepositories(RepositorySystemSession session) {
        final List<RemoteRepository> remoteRepositories = new ArrayList<>();

        final var settings = getSettings();
        final var activeProfiles = settings.getActiveProfiles();
        for (final String profileId : activeProfiles) {
            final var profile = settings.getProfilesAsMap().get(profileId);
            if (profile == null) {
                continue;
            }

            for (final Repository repository : profile.getRepositories()) {
                final var id = repository.getId();
                // don't add duplicates
                if (!remoteRepositories.stream().anyMatch(r -> r.getId().equals(id))) {
                    final var repo = new RemoteRepository.Builder(id, repository.getLayout(), repository.getUrl());
                    repo.setId(id);
                    repo.setUrl(repository.getUrl());
                    if (repository.getReleases() != null) {
                        final var repositoryPolicy = repository.getReleases();
                        final var policy = new org.eclipse.aether.repository.RepositoryPolicy(
                                repositoryPolicy.isEnabled(),
                                repositoryPolicy.getUpdatePolicy(),
                                repositoryPolicy.getChecksumPolicy());
                        repo.setReleasePolicy(policy);
                    } else {
                        // create a strict default policy
                        repo.setReleasePolicy(POLICY_ENABLED_NO_UPDATES_CHECKSUM_FAIL);
                    }
                    if (repository.getSnapshots() != null) {
                        final var repositoryPolicy = repository.getSnapshots();
                        final var policy = new org.eclipse.aether.repository.RepositoryPolicy(
                                repositoryPolicy.isEnabled(),
                                repositoryPolicy.getUpdatePolicy(),
                                repositoryPolicy.getChecksumPolicy());
                        repo.setSnapshotPolicy(policy);
                    } else {
                        // no snapshots by default
                        repo.setSnapshotPolicy(POLICY_DISABLED);
                    }
                    remoteRepositories.add(repo.build());
                }
            }
        }

        // add 'central' if missing
        if (!remoteRepositories.stream().anyMatch(r -> "central".equals(r.getId()))) {
            LOG.debug(
                "Adding central repo using https://repo1.maven.org/maven2/ because no one is defined in settings!");
            remoteRepositories.add(
                new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/")
                        .setReleasePolicy(POLICY_ENABLED_NO_UPDATES_CHECKSUM_FAIL)
                        .setSnapshotPolicy(POLICY_DISABLED)
                        .build());
        }

        return getSystem().newResolutionRepositories(session, remoteRepositories);
    }

    private synchronized Settings getSettings() {
        if (settings == null) {
            final var request = new DefaultSettingsBuildingRequest();
            request.setUserSettingsFile(null);
            request.setGlobalSettingsFile(null);
            request.setSystemProperties(getSystemProperties());

            try {
                settings = SETTINGS_BUILDER.build(request).getEffectiveSettings();
            } catch (final SettingsBuildingException e) {
                throw new IllegalStateException("Could not process settings.xml: " + e.getMessage(), e);
            }

            final var profile = new Profile();
            profile.setId("maven-deps-repo-sys");
            final var activation = new Activation();
            activation.setActiveByDefault(true);
            profile.setActivation(activation);
            settings.getProfiles().add(profile);
            settings.setActiveProfiles(List.of("maven-deps-repo-sys"));

            /*
             * Next we need to setup repositories and mirrors.
             *
             * This is a bit tricky when you have multiple repository.
             * For example, imagine a public/central & staging repository setup.
             * Sometimes you want to publish to staging first, run build & test with staged artifacts and then promote to good.
             *
             * This works in Bazel by cheating. Both URLs can be used in Bazel's downloader configuration to override the "central"
             * URL. Bazel will simply try them all till one has the file.
             *
             * Maven allows to resolve using multiple repositories. Each repository can have mirrors.
             * Originally we used Bazel's downloader configuration to populate mirrors for central.
             * However, Maven expects each mirror to be consistent, i.e. if one mirror doesn't have the file it fails/aborts resolution.
             *
             * The cheating we do has several disadvantages. The biggest one is that it does allow building a pinned catalog with artifacts that don't
             * exist in the public repository.
             *
             * Ideally we should revisit this. This requires implementing and supporting for multiple sources per pinned artifact.
             * Only then we can be correct. But this is a lot of work and we don't know the benefit. At some point the staged artifact
             * is supposed to be promoted and the updated catalog can be checked into source control.
             *
             * For the time being, we setup all possible download URLs as separate repositories and allow Maven Aether to resolve them.
             */

            // Maven Central (if necessary)
            populateSettingsWithRepositoryAndCredentials(
                settings,
                profile,
                "central",
                mavenCenteralUrl,
                true /* setupMirrorsAsSeparateRepositories */);

            // all other repositories
            for (final String mavenRepositoryUrl : mavenRepositories) {
                populateSettingsWithRepositoryAndCredentials(
                    settings,
                    profile,
                    mavenRepositoryUrl,
                    mavenRepositoryUrl,
                    true /* setupMirrorsAsSeparateRepositories */);
            }
        }
        return settings;
    }

    public synchronized RepositorySystem getSystem() {
        if (repoSys == null) {
            repoSys = repositorySystemSupplier.get();
            if (repoSys == null) {
                throw new IllegalStateException("The repository system could not be initialized");
            }
        }
        return repoSys;
    }

    private Properties getSystemProperties() {
        final var props = new Properties();
        getEnvProperties(props);
        props.putAll(System.getProperties());

        // extracted from default.properties (to allow loading Core poms)
        props.put("maven.provisioning.plugin.version", "2.2.21");

        return props;
    }

    private UrlRewriter getUrlRewriter() {
        final var urlRewriter = this.urlRewriter;
        if ((urlRewriter != null) || (downloaderConfig == null)) {
            return urlRewriter;
        }

        try {
            return this.urlRewriter = UrlRewriter.getDownloaderUrlRewriter(downloaderConfig.toString());
        } catch (final UrlRewriterParseException e) {
            throw new IllegalArgumentException(
                    format("Error reading download config file '%s': %s", downloaderConfig, e.getMessage()),
                    e);
        }
    }

    private String getUserAgent() {
        final var buffer = new StringBuilder(128);

        buffer.append("Baze-Maven-Deps");
        buffer.append(" (");
        buffer.append("Java ").append(System.getProperty("java.version"));
        buffer.append("; ");
        buffer.append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version"));
        buffer.append(")");
        buffer.append(" Aether");

        return buffer.toString();
    }

    public ModelBuildingResult loadModel(File pomFile, boolean local, ProgressMonitor monitor) {
        final var session = newSession(monitor);

        final var repositories = getRemoteRepositories(session);

        final ModelResolver modelResolver = new MavenDepsModelResolver(session, "project", getSystem(), repositories);

        final var settings = getSettings();

        try {
            final var request = new DefaultModelBuildingRequest();
            request.setLocationTracking(true);
            request.setProcessPlugins(false);
            if (local) {
                request.setPomFile(pomFile);
                request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_STRICT);
            } else {
                request.setModelSource(new FileModelSource(pomFile));
                request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
            }
            request.setSystemProperties(getSystemProperties());
            request.setProfiles(SettingsUtils.convert(settings.getProfiles()));
            request.setActiveProfileIds(settings.getActiveProfiles());
            request.setModelResolver(modelResolver);
            request.setModelCache(MavenDepsModelCache.newInstance(session));
            return modelBuilder.build(request);
        } catch (final ModelBuildingException e) {
            checkForCommonIssues(e);
            throw new IllegalStateException("Could not load POM " + pomFile + ": " + e.getMessage(), e);
        }
    }

    private RepositorySystemSession newSession(ProgressMonitor monitor) {
        final var session = MavenRepositorySystemUtils.newSession();

        final Map<String, Object> configProps = new LinkedHashMap<>();
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());

        // I was hoping this would make a difference.
        // Turns out that RepositoryConnector.get(..) is mostly called
        // one-by-one :(
        configProps.put("aether.connector.basic.threads", 16);
        configProps.put("aether.metadataResolver.threads", 8);

        // Bazel uses SHA256 by default (keep SHA-1 for Maven, disable all others)
        // looks like NEXUS 3 no longer supports SHA-256 (:sadpanda:)
        configProps.put("aether.checksums.algorithms", "SHA-1");

        // run with verbose conflict resolution and dependency info to allow computation of transitive dependencies
        configProps.put(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
        configProps.put(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);

        processServerConfiguration(configProps);
        session.setConfigProperties(configProps);

        session.setOffline(getSettings().isOffline());
        session.setSystemProperties(System.getProperties());

        // ignore <repositories> defined in dependency pom.xml files.
        // eliminates redundant repository requests
        session.setIgnoreArtifactDescriptorRepositories(true);

        session.setProxySelector(getProxySelector());
        session.setMirrorSelector(getMirrorSelector());
        session.setAuthenticationSelector(getAuthSelector());

        session.setCache(new DefaultRepositoryCache());

        session.setRepositoryListener(new MavenDepsRepositoryListener(monitor));
        session.setTransferListener(new MavenDepsTransferListener(monitor));

        session.setLocalRepositoryManager(getLocalRepoMan(session));

        // there is no workspace here
        session.setWorkspaceReader(new WorkspaceReader() {

            @Override
            public File findArtifact(Artifact artifact) {
                return null;
            }

            @Override
            public List<String> findVersions(Artifact artifact) {
                return Collections.emptyList();
            }

            @Override
            public WorkspaceRepository getRepository() {
                return new WorkspaceRepository("bazel-maven-deps");
            }
        });

        return session;
    }

    private void populareSettingsWithRepository(Profile profile, String id, String url) {
        final var repository = new Repository();
        repository.setId(id);
        repository.setUrl(url);
        profile.getRepositories().add(repository);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Configured repository '{}' ({}).", id, url);
        }
    }

    private void populateSettingsWithBlockingMirrorForRepository(Settings settings, String id) {
        final var mirror = new Mirror();
        mirror.setId("blocked:" + id);
        mirror.setBlocked(true);
        mirror.setMirrorOf(id);
        settings.getMirrors().add(mirror);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Configured blocking mirror '{}' for '{}'.", mirror.getId(), mirror.getMirrorOf());
        }
    }

    private void populateSettingsWithCredentials(Settings settings, String id, String url) {
        try {
            final var host = new URI(url).getHost();
            final var credential = getNetrc().getCredential(host);
            if (credential != null) {
                final var server = new Server();
                server.setId(id);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Configuring server '{}' with credentials ({}).", id, credential.login());
                }
                server.setUsername(credential.login());
                server.setPassword(credential.password());

                // only add server entry when there are credentials
                settings.getServers().add(server);
            }
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(format("Invalid Maven server URL '%s': %s", url, e.getMessage()), e);
        }
    }

    private void populateSettingsWithMirror(Settings settings, String id, String url) {
        final var urlRewriter = getUrlRewriter();
        if (urlRewriter == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping mirror configuration because url rewriting is disabled.");
            }
            return;
        }

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Detecting mirrors for '{}'.", url);
            }
            final var result = urlRewriter.amend(List.of(new URL(url)));
            if (result.size() >= 1) {
                var mirrorCount = 0;
                for (var rewrittenURL : result) {
                    if (rewrittenURL.rewritten()) {
                        final var mirrorId = "mirror-" + id + "-" + ++mirrorCount;
                        final var mirrorUrl = rewrittenURL.url().toExternalForm();

                        final var mirror = new Mirror();
                        mirror.setId(mirrorId);
                        mirror.setUrl(mirrorUrl);
                        mirror.setMirrorOf(id);
                        settings.getMirrors().add(mirror);

                        // ensure there are credentials
                        populateSettingsWithCredentials(settings, mirrorId, mirrorUrl);

                        if (LOG.isDebugEnabled()) {
                            LOG.debug(
                                "Configured mirror '{}' for '{}': {}",
                                mirror.getId(),
                                mirror.getMirrorOf(),
                                mirror.getUrl());
                        }
                    } else {
                        LOG.debug("Ignoring '{}'", rewrittenURL);
                    }
                }
            } else if (result.isEmpty()) {
                populateSettingsWithBlockingMirrorForRepository(settings, id);
            }
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(format("Invalid Maven repository URL '%s': %s", url, e.getMessage()), e);
        }
    }

    private void populateSettingsWithRepositoryAndCredentials(
            Settings settings,
            Profile profile,
            String id,
            String url,
            boolean setupMirrorsAsSeparateRepositories) {

        if (setupMirrorsAsSeparateRepositories) {

            // in this mode all rewritten URLs are setup as separate repositories

            final var urlRewriter = getUrlRewriter();
            if (urlRewriter != null) {
                try {
                    var rewrittenUrls = urlRewriter.amend(List.of(new URL(url)));
                    if (rewrittenUrls.isEmpty()) {
                        // URL is blocked
                        populareSettingsWithRepository(profile, id, url);
                        populateSettingsWithBlockingMirrorForRepository(settings, id);
                    } else {
                        var count = 0;
                        for (var rewrittenUrl : rewrittenUrls) {
                            // make repository id unique
                            var repositoryId = ++count > 1 ? id + "-" + count : id;

                            // the repository url is either rewritten or not
                            String repositoryUrl;
                            if (rewrittenUrl.rewritten()) {
                                repositoryUrl = rewrittenUrl.url().toExternalForm();
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug(
                                        "Found rewritten URL for repository '{}': {} >>> {}",
                                        id,
                                        url,
                                        repositoryUrl);
                                }
                            } else {
                                repositoryUrl = url;
                            }

                            // add credentials
                            populateSettingsWithCredentials(settings, repositoryId, repositoryUrl);

                            // add repository information
                            populareSettingsWithRepository(profile, repositoryId, repositoryUrl);
                        }
                    }
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(
                            format("Invalid Maven repository URL '%s': %s", url, e.getMessage()),
                            e);
                }
                return;
            }

            // fall through to old mode
            if (LOG.isDebugEnabled()) {
                LOG.debug("Download url rewriting is disabled. Setting up repository as is.");
            }
        }

        // proceed with the old mode of configuring rewritten URLs as mirrors

        // add credentials
        populateSettingsWithCredentials(settings, id, url);

        // add repository information
        populareSettingsWithRepository(profile, id, url);

        // add mirror (if we have a download config)
        if ((downloaderConfig != null) && !setupMirrorsAsSeparateRepositories) {
            populateSettingsWithMirror(settings, id, url);
        }
    }

    private void processServerConfiguration(Map<String, Object> configProps) {
        final var settings = getSettings();
        for (final Server server : settings.getServers()) {
            if (server.getConfiguration() != null) {
                final var dom = (Xpp3Dom) server.getConfiguration();
                for (var i = dom.getChildCount() - 1; i >= 0; i--) {
                    final var child = dom.getChild(i);
                    if ("wagonProvider".equals(child.getName())) {
                        dom.removeChild(i);
                    } else if ("httpHeaders".equals(child.getName())) {
                        configProps.put(
                            ConfigurationProperties.HTTP_HEADERS + "." + server.getId(),
                            getHttpHeaders(child));
                    }
                }

                configProps.put("aether.connector.wagon.config." + server.getId(), dom);
            }

            configProps.put("aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions());
            configProps.put("aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions());
        }
    }

    public DependencyResultWithTransferInfo resolveDependencies(
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            ProgressMonitor monitor) throws DependencyResolutionException {
        final var session = newSession(monitor);

        final var repos = getRemoteRepositories(session);

        final var collectRequest = new CollectRequest();
        collectRequest.setRequestContext("project");

        for (final org.eclipse.aether.repository.RemoteRepository repo : repos) {
            collectRequest.addRepository(repo);
        }

        // note, using setRoot makes the resolution much slower; therefore we just use setDependencies
        collectRequest.setDependencies(dependencies);
        collectRequest.setManagedDependencies(managedDependencies);

        final var dependencyRequest = new DependencyRequest();
        dependencyRequest.setCollectRequest(collectRequest);

        try {
            return DependencyResultWithTransferInfo
                    .create(getSystem().resolveDependencies(session, dependencyRequest), session);
        } catch (final DependencyResolutionException e) {
            checkForCommonIssues(e);
            throw e;
        }
    }

    public DependencyResultWithTransferInfo resolveDependency(
            Dependency root,
            List<Dependency> managedDependencies,
            ProgressMonitor monitor) throws DependencyResolutionException {
        final var session = newSession(monitor);

        final var repos = getRemoteRepositories(session);

        final var collectRequest = new CollectRequest();
        collectRequest.setRequestContext("project");

        for (final org.eclipse.aether.repository.RemoteRepository repo : repos) {
            collectRequest.addRepository(repo);
        }

        collectRequest.setRoot(root);
        collectRequest.setManagedDependencies(managedDependencies);

        final var dependencyRequest = new DependencyRequest();
        dependencyRequest.setCollectRequest(collectRequest);

        try {
            return DependencyResultWithTransferInfo
                    .create(getSystem().resolveDependencies(session, dependencyRequest), session);
        } catch (final DependencyResolutionException e) {
            checkForCommonIssues(e);
            throw e;
        }
    }
}