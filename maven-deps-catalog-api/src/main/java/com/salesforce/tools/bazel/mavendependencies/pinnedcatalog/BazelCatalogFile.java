package com.salesforce.tools.bazel.mavendependencies.pinnedcatalog;

import static com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport.createWithNameAndArtifact;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.starlark.BazelConventions;
import com.salesforce.tools.bazel.mavendependencies.starlark.ParseException;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkFileParser;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;

import net.starlark.java.syntax.Argument;
import net.starlark.java.syntax.Argument.Keyword;
import net.starlark.java.syntax.CallExpression;
import net.starlark.java.syntax.DefStatement;
import net.starlark.java.syntax.Expression.Kind;
import net.starlark.java.syntax.ExpressionStatement;
import net.starlark.java.syntax.Statement;
import net.starlark.java.syntax.StringLiteral;

/**
 * A single .bzl file of the {@link BazelDependenciesCatalog} .
 * <p>
 * The file syntax is pure Starlark. However, we expect the file to implement particular conventions and ordering.
 * </p>
 * <ol>
 * <li>File name using <code>GROUP.bzl</code> schema</li>
 * <li><code>load</code> statement for <code>//tools/build/bazel/sfdc/3pp:jvm.bzl</code></li>
 * <li>a single macro declaration for setting up the dependencies:
 * <code>def setup_maven_dependencies_GROUP(...)</code></li>
 * <li>one ore more <code>jvm_maven_import_external(...)</code> within the setup macro to declare Java downloads and
 * imports for the Maven dependencies</li>
 * </ol>
 * <ul>
 * <li>The <code>GROUP</code> is a grouping decided externally and maintained through the whole catalog.</li>
 * <li><code>GROUP</code> MUST user either <code>'_'</code> or lower or upper case <code>a-z</code> characters or digits
 * <code>0-9</code> suffix</li>
 * </ul>
 * <p>
 * This file is intentionally package private, it should not be used/modified outside of
 * {@link BazelDependenciesCatalog}.
 * </p>
 */
class BazelCatalogFile {

    static class Reader extends StarlarkFileParser<BazelCatalogFile> {

        private final String group;

        public Reader(Path catalogFile) throws IOException {
            super(catalogFile);

            var fileName = catalogFile.getFileName().toString();
            if (!fileName.endsWith(".bzl")) {
                throw new IllegalArgumentException(
                        format("Invalid catalog file: file '%s' does not end with .bzl!", fileName));
            }

            group = fileName.substring(0, fileName.length() - 4);
            if (group.isBlank()) {
                throw new IllegalArgumentException(
                        format("Invalid catalog file:  file '%s' results in a blank group!", fileName));
            }

            if (!BazelConventions.toTargetName(group).equals(group)) {
                throw new IllegalArgumentException(
                        format("Invalid catalog file:  file '%s' contains not allowed characters!", fileName));
            }
        }

        protected MavenArtifact parseArtifact(Argument coordinates) throws ParseException {
            var value = coordinates.getValue();
            if (value.kind() != Kind.STRING_LITERAL) {
                throw new ParseException("expected string literal with coordinates", value);
            }

            MavenArtifact artifact;
            try {
                artifact = MavenArtifact.fromCoordinatesString(((StringLiteral) value).getValue());
            } catch (IllegalArgumentException e) {
                throw new ParseException(e.getMessage(), value);
            }
            return artifact;
        }

        @Override
        public BazelCatalogFile read() throws ParseException {
            SortedSet<BazelJavaDependencyImport> javaImports = new TreeSet<>();

            // an empty file is allowed
            // this is an obsolete catalog
            if (starlarkFile.getStatements().isEmpty()) {
                return new BazelCatalogFile(group, javaImports);
            }

            // find the def statement
            var expectedDefStatementName = "setup_maven_dependencies_" + group;
            List<DefStatement> defStatements = starlarkFile.getStatements()
                    .stream()
                    .filter(DefStatement.class::isInstance)
                    .map(DefStatement.class::cast)
                    .filter(d -> d.getIdentifier().getName().equals(expectedDefStatementName))
                    .collect(toList());
            if (defStatements.isEmpty()) {
                throw new ParseException(format("missing def %s statement", expectedDefStatementName), starlarkFile);
            }
            if (defStatements.size() > 1) {
                throw new ParseException(
                        format("only one def %s statement allowed; multiple found", expectedDefStatementName),
                        starlarkFile);
            }

            var defStatement = defStatements.get(0);

            List<Statement> invalidStatements = defStatement.getBody()
                    .stream()
                    .filter(Predicate.not(ExpressionStatement.class::isInstance))
                    .collect(toList());
            if (!invalidStatements.isEmpty()) {
                throw new ParseException(
                        "setup method body contains unexpected statements: only call expressions allowed ",
                        invalidStatements.get(0));
            }

            // extract and process all dependencies
            List<CallExpression> jvmImportCalls = defStatement.getBody()
                    .stream()
                    .map(ExpressionStatement.class::cast)
                    .map(ExpressionStatement::getExpression)
                    .filter(CallExpression.class::isInstance)
                    .map(CallExpression.class::cast)
                    .collect(toList());
            for (CallExpression jvmImportCall : jvmImportCalls) {
                if ((jvmImportCall.getFunction().kind() != Kind.IDENTIFIER)
                        || !MACRO_JVM_MAVEN_IMPORT_EXTERNAL.equals(jvmImportCall.getFunction().prettyPrint())) {
                    throw new ParseException(
                            "invalid function call: only 'jvm_maven_import_external' is allowed",
                            jvmImportCall.getFunction());
                }

                if (!jvmImportCall.getArguments().stream().allMatch(Keyword.class::isInstance)) {
                    throw new ParseException("invalid function call: only keyowrd arguments allowed", jvmImportCall);
                }

                var keywordArguments = keywordArgumentsAsMap(jvmImportCall.getArguments());

                var name = parseStringArgument(keywordArguments.get(KEYWORD_NAME));
                var artifact = parseArtifact(keywordArguments.get(KEYWORD_ARTIFACT));

                var entryBuilder = createWithNameAndArtifact(name, artifact);

                entryBuilder
                        .setArtifactSha256(parseOptionalStringArgument(keywordArguments.get(KEYWORD_ARTIFACT_SHA256)));
                entryBuilder.setArtifactSha1(parseOptionalStringArgument(keywordArguments.get(KEYWORD_ARTIFACT_SHA1)));
                entryBuilder.setLicenses(parseOptionalStringListArgument(keywordArguments.get(KEYWORD_LICENSES)));

                var fetchSources = parseOptionalBoolArgument(keywordArguments.get(KEYWORD_FETCH_SOURCES), false);
                var hasSrcJarUrls = keywordArguments.containsKey(KEYWORD_SRCJAR_URLS);
                if (hasSrcJarUrls && fetchSources) {
                    throw new ParseException(
                            "it's incorrect to set both: 'fetch_sources' and 'srcjar_urls'",
                            jvmImportCall);
                }
                entryBuilder.setSourcesArtifact(hasSrcJarUrls || fetchSources);
                entryBuilder.setSourcesArtifactSha256(
                    parseOptionalStringArgument(keywordArguments.get(KEYWORD_SRCJAR_SHA256)));
                entryBuilder
                        .setSourcesArtifactSha1(parseOptionalStringArgument(keywordArguments.get(KEYWORD_SRCJAR_SHA1)));

                entryBuilder.setDeps(parseOptionalStringListArgument(keywordArguments.get(KEYWORD_DEPS)));
                entryBuilder
                        .setRuntimeDeps(parseOptionalStringListArgument(keywordArguments.get(KEYWORD_RUNTIME_DEPS)));
                entryBuilder.setExports(parseOptionalStringListArgument(keywordArguments.get(KEYWORD_EXPORTS)));
                entryBuilder.setTestonly(parseOptionalBoolArgument(keywordArguments.get(KEYWORD_TESTONLY), false));
                entryBuilder.setNeverlink(parseOptionalBoolArgument(keywordArguments.get(KEYWORD_NEVERLINK), false));
                entryBuilder.setExtraBuildFileContent(
                    parseOptionalStringArgument(keywordArguments.get(KEYWORD_EXTRA_BUILD_FILE_CONTENT)));
                entryBuilder.setDefaultVisibility(
                    parseOptionalStringListArgument(keywordArguments.get(KEYWORD_DEFAULT_VISIBILITY)));
                entryBuilder.setTags(parseOptionalStringListArgument(keywordArguments.get(KEYWORD_TAGS)));

                javaImports.add(entryBuilder.build());
            }

            return new BazelCatalogFile(group, javaImports);
        }

    }

    private static final String MACRO_JVM_MAVEN_IMPORT_EXTERNAL = "jvm_maven_import_external";
    private static final String KEYWORD_DEFAULT_VISIBILITY = "default_visibility";
    private static final String KEYWORD_EXTRA_BUILD_FILE_CONTENT = "extra_build_file_content";
    private static final String KEYWORD_NEVERLINK = "neverlink";
    private static final String KEYWORD_TESTONLY = "testonly_";
    private static final String KEYWORD_EXPORTS = "exports";
    private static final String KEYWORD_RUNTIME_DEPS = "runtime_deps";
    private static final String KEYWORD_DEPS = "deps";
    private static final String KEYWORD_SRCJAR_SHA256 = "srcjar_sha256";
    private static final String KEYWORD_SRCJAR_SHA1 = "srcjar_sha1";
    private static final String KEYWORD_SRCJAR_URLS = "srcjar_urls";
    private static final String KEYWORD_FETCH_SOURCES = "fetch_sources";
    private static final String KEYWORD_LICENSES = "licenses";
    private static final String KEYWORD_ARTIFACT = "artifact";
    private static final String KEYWORD_ARTIFACT_SHA256 = "artifact_sha256";
    private static final String KEYWORD_ARTIFACT_SHA1 = "artifact_sha1";
    private static final String KEYWORD_NAME = "name";
    private static final String KEYWORD_TAGS = "tags";

    public static BazelCatalogFile read(Path existingFile) throws IOException {
        return new Reader(existingFile).read();
    }

    private final String group;
    private final SortedSet<BazelJavaDependencyImport> javaImports;

    public BazelCatalogFile(String group, SortedSet<BazelJavaDependencyImport> javaImports) {
        this.group = group;
        this.javaImports = javaImports;
    }

    private void appendIfNonNull(StarlarkStringBuilder output, String name, String value) {
        if (value == null) {
            return;
        }
        output.appendAttribute(name, value).appendCommaFollowedByNewline();
    }

    private void appendIfNotEmpty(StarlarkStringBuilder output, String name, Collection<String> value) {
        if ((value == null) || value.isEmpty()) {
            return;
        }

        output.appendListAttribute(name, value).appendCommaFollowedByNewline();
    }

    public String getGroup() {
        return group;
    }

    public SortedSet<BazelJavaDependencyImport> getJavaImports() {
        return javaImports;
    }

    public CharSequence prettyPrint(
            SortedSet<String> mavenServers,
            String labelForLoadingJvmMavenImportExternalSymbol,
            String preamble) {
        var output = new StarlarkStringBuilder(4);

        output.append("load(\"")
                .append(
                    requireNonNull(
                        labelForLoadingJvmMavenImportExternalSymbol,
                        "missing label for loading jvm_maven_import_external symbol"))
                .append("\", \"jvm_maven_import_external\")")
                .appendNewline();
        output.appendNewline();

        if ((preamble != null) && !preamble.isBlank()) {
            output.append(preamble);
            output.appendNewline();
        }

        output.append("def setup_maven_dependencies_").append(group).append('(').appendNewline();
        output.increaseIndention().increaseIndention();
        output.append("maven_servers = ").appendListQuotedWithWrappingWhenNecessary(mavenServers);
        output.append("):").appendNewline();
        output.decreaseIndention();
        output.append("\"\"\"Defines repositories for Maven dependencies in group ")
                .append(group)
                .append(".\"\"\"")
                .appendNewline();
        output.appendNewline();

        for (BazelJavaDependencyImport javaImport : javaImports) {
            output.append(MACRO_JVM_MAVEN_IMPORT_EXTERNAL).append("(").appendNewline();
            output.increaseIndention();

            // note: maintain an order that is consistent with buildifier

            output.appendAttribute(KEYWORD_NAME, javaImport.getName()).appendCommaFollowedByNewline();

            output.appendAttribute(KEYWORD_ARTIFACT, javaImport.getArtifact().toCoordinatesString())
                    .appendCommaFollowedByNewline();
            appendIfNonNull(output, KEYWORD_ARTIFACT_SHA256, javaImport.getArtifactSha256());
            appendIfNonNull(output, KEYWORD_ARTIFACT_SHA1, javaImport.getArtifactSha1());

            output.append("server_urls = maven_servers").appendCommaFollowedByNewline();

            appendIfNotEmpty(output, KEYWORD_DEFAULT_VISIBILITY, javaImport.getDefaultVisibility());
            appendIfNonNull(output, KEYWORD_EXTRA_BUILD_FILE_CONTENT, javaImport.getExtraBuildFileContent());

            if (javaImport.isNeverlink()) {
                output.appendBooleanAttribute(KEYWORD_NEVERLINK, true).appendCommaFollowedByNewline();
            }
            if (javaImport.isTestonly()) {
                output.appendBooleanAttribute(KEYWORD_TESTONLY, true).appendCommaFollowedByNewline();
            }

            appendIfNotEmpty(output, KEYWORD_EXPORTS, javaImport.getExports());
            appendIfNotEmpty(output, KEYWORD_DEPS, javaImport.getDeps());
            appendIfNotEmpty(output, KEYWORD_RUNTIME_DEPS, javaImport.getRuntimeDeps());

            appendIfNonNull(output, KEYWORD_SRCJAR_SHA256, javaImport.getSourcesArtifactSha256());
            appendIfNonNull(output, KEYWORD_SRCJAR_SHA1, javaImport.getSourcesArtifactSha1());
            if (javaImport.getSourcesArtifact() != null) {
                // jvm_maven_import_external only supports standard "sources" qualifier
                if ("sources".equals(javaImport.getSourcesArtifact().getClassifier())) {
                    output.appendBooleanAttribute(KEYWORD_FETCH_SOURCES, true).appendCommaFollowedByNewline();
                } else {
                    // [server + "/path" for server in maven_servers] (dynamically create the list in Starlark)
                    output.append(KEYWORD_SRCJAR_URLS)
                            .append(" = [server + ")
                            .appendQuoted(javaImport.getSourcesArtifact().toRelativePath().toString())
                            .append(" for server in maven_servers]")
                            .appendCommaFollowedByNewline();
                }
            }

            appendIfNotEmpty(output, KEYWORD_LICENSES, javaImport.getLicenses());
            appendIfNotEmpty(output, KEYWORD_TAGS, javaImport.getTags());

            output.decreaseIndention();
            output.append(")").appendNewline();
        }

        output.decreaseIndention();
        output.appendNewline();

        // generate list of repository names to support use_repo fixup for bzlmod
        output.append("def maven_repo_names_").append(group).append("():").appendNewline();
        output.increaseIndention();
        output.append("\"\"\"Returns the list of repository names of all Maven dependencies in group ")
                .append(group)
                .append(".\"\"\"")
                .appendNewline();
        output.appendNewline();
        output.append("return ")
                .appendListQuotedWithWrappingWhenNecessary(
                    javaImports.stream().map(BazelJavaDependencyImport::getName).collect(toList()))
                .appendNewline();
        output.increaseIndention();

        return output.toString();
    }
}
