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
package com.google.devtools.build.lib.authandtls;

import static com.google.common.base.Predicates.not;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.authandtls.Netrc.Credential;

/**
 * A parser used to parse .netrc content.
 *
 * @see <a href="https://man.cx/netrc(4)">netrc − file for ftp remote login data</a>
 * @see <a href= "https://github.com/bazelbuild/bazel/blob/master/tools/build_defs/repo/utils.bzl#L203-L204">Starlark
 *      netrc parser</a>
 */
public class NetrcParser {
    @AutoValue
    abstract static class CommentToken implements Token {
        public static CommentToken create() {
            return new AutoValue_NetrcParser_CommentToken();
        }
    }

    @AutoValue
    abstract static class ItemToken implements Token {
        public static ItemToken create(String item) {
            return new AutoValue_NetrcParser_ItemToken(item);
        }

        abstract String item();
    }

    @AutoValue
    abstract static class NewlineToken implements Token {
        public static NewlineToken create() {
            return new AutoValue_NetrcParser_NewlineToken();
        }
    }

    interface Token {
    }

    private static class TokenStream implements Closeable {
        private final BufferedReader bufferedReader;
        private final Queue<Token> tokens = new ArrayDeque<>();

        TokenStream(InputStream inputStream) throws IOException {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
            processLine();
        }

        @Override
        public void close() throws IOException {
            bufferedReader.close();
        }

        public boolean hasNext() {
            return !tokens.isEmpty();
        }

        public Token next() throws IOException {
            final var token = tokens.poll();
            if (tokens.isEmpty()) {
                processLine();
            }
            return token;
        }

        public Token peek() {
            return tokens.peek();
        }

        private void processLine() throws IOException {
            final var line = bufferedReader.readLine();
            if (line == null) {
                return;
            }

            // Comments start with #
            if (line.startsWith("#")) {
                tokens.add(CommentToken.create());
            } else {
                Arrays.stream(line.split("\\s+"))
                        .filter(not(Strings::isNullOrEmpty))
                        .map(ItemToken::create)
                        .forEach(tokens::add);
            }

            tokens.add(NewlineToken.create());
        }
    }

    private static final String MACHINE = "machine";

    private static final String MACDEF = "macdef";

    private static final String DEFAULT = "default";

    private static final String LOGIN = "login";

    private static final String PASSWORD = "password";

    private static final String ACCOUNT = "account";

    private static String nextItem(TokenStream tokenStream) throws IOException {
        while (tokenStream.hasNext()) {
            final var token = tokenStream.next();
            if (token instanceof ItemToken) {
                return ((ItemToken) token).item();
            }
        }

        throw new IOException("Unexpected EOF");
    }

    private static Netrc parse(TokenStream tokenStream) throws IOException {
        Credential defaultCredential = null;
        final Map<String, Credential> credentialMap = new HashMap<>();

        var done = false;
        while (!done && tokenStream.hasNext()) {
            final var token = tokenStream.next();
            if (token instanceof ItemToken) {
                final var item = ((ItemToken) token).item();
                switch (item) {
                    case MACHINE:
                        final var machine = nextItem(tokenStream);
                        final var credential = parseCredentialForMachine(tokenStream, machine);
                        credentialMap.put(machine, credential);
                        break;
                    case MACDEF:
                        skipMacdef(tokenStream);
                        break;
                    case DEFAULT:
                        defaultCredential = parseCredentialForMachine(tokenStream, DEFAULT);
                        // There can be only one default token, and it must be after all machine tokens.
                        done = true;
                        break;
                    default:
                        throw new IOException(
                                String.format(
                                    "Unexpected token: %s (expecting %s, %s or %s)",
                                    item,
                                    MACHINE,
                                    MACDEF,
                                    DEFAULT));
                }
            }
        }

        return Netrc.create(defaultCredential, ImmutableMap.copyOf(credentialMap));
    }

    public static Netrc parseAndClose(InputStream inputStream) throws IOException {
        try (var tokenStream = new TokenStream(inputStream)) {
            return parse(tokenStream);
        }
    }

    /** Parse credentials for a given machine from token stream. */
    private static Credential parseCredentialForMachine(TokenStream tokenStream, String machine) throws IOException {
        final var builder = Credential.builder(machine);

        var done = false;
        while (!done && tokenStream.hasNext()) {
            // Peek rather than taking next token since we probably won't process it
            final var token = tokenStream.peek();
            if (token instanceof ItemToken) {
                final var item = ((ItemToken) token).item();
                switch (item) {
                    case LOGIN:
                        tokenStream.next();
                        builder.setLogin(nextItem(tokenStream));
                        break;
                    case PASSWORD:
                        tokenStream.next();
                        builder.setPassword(nextItem(tokenStream));
                        break;
                    case ACCOUNT:
                        tokenStream.next();
                        builder.setAccount(nextItem(tokenStream));
                        break;
                    case MACHINE:
                    case MACDEF:
                    case DEFAULT:
                        done = true;
                        break;
                    default:
                        throw new IOException(
                                String.format(
                                    "Unexpected item: %s (expecting %s, %s, %s, %s, %s or %s)",
                                    item,
                                    LOGIN,
                                    PASSWORD,
                                    ACCOUNT,
                                    MACHINE,
                                    MACDEF,
                                    DEFAULT));
                }
            } else {
                tokenStream.next();
            }
        }

        return builder.build();
    }

    /** Skip macdef section since we don't need that data currently. */
    private static void skipMacdef(TokenStream tokenStream) throws IOException {
        var numNewlines = 0;
        while (tokenStream.hasNext()) {
            final var token = tokenStream.next();
            if (token instanceof NewlineToken) {
                ++numNewlines;
            } else {
                numNewlines = 0;
            }
            if (numNewlines >= 2) {
                break;
            }
        }
    }

    private NetrcParser() {}
}