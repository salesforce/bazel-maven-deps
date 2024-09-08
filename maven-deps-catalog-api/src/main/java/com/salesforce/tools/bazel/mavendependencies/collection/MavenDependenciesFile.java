package com.salesforce.tools.bazel.mavendependencies.collection;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact.Exclusion;
import com.salesforce.tools.bazel.mavendependencies.starlark.BazelConventions;
import com.salesforce.tools.bazel.mavendependencies.starlark.ParseException;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkFileParser;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;

import net.starlark.java.syntax.Argument;
import net.starlark.java.syntax.Argument.Keyword;
import net.starlark.java.syntax.Argument.Positional;
import net.starlark.java.syntax.AssignmentStatement;
import net.starlark.java.syntax.BinaryOperatorExpression;
import net.starlark.java.syntax.CallExpression;
import net.starlark.java.syntax.Expression;
import net.starlark.java.syntax.Expression.Kind;
import net.starlark.java.syntax.Identifier;
import net.starlark.java.syntax.ListExpression;
import net.starlark.java.syntax.StringLiteral;
import net.starlark.java.syntax.TokenKind;

/**
 * A //third_party/dependencies/*.bzl file used for defining dependencies to 3pp libraries.
 * <p>
 * The file syntax is pure Starlark. However, we expect the file to implement particular conventions and ordering.
 * </p>
 * <ol>
 * <li><code>load</code> statement for <code>//tools/build/bazel/3pp/defs.bzl</code></li>
 * <li>constant declarations for versions (eg. _FORCE_COMMONS_VERSION = "0.7.1")</li>
 * <li>a single constant declaration for BOM imports: <code>MAVEN_BOM_IMPORTS = maven.imports(...)</code></li>
 * <li>a single constant declaration for dependencies: <code>MAVEN_DEPENDENCIES = maven.dependencies(...)</code></li>
 * <li>a single constant declaration for global exclusions:
 * <code>MAVEN_EXCLUSIONS = [ "groupId:artifactId", ... ]</code></li>
 * </ol>
 * <ul>
 * <li>The <code>MAVEN_BOM_IMPORTS</code> declaration MUST use the <code>maven.imports</code> macro.</li>
 * <li>The <code>MAVEN_DEPENDENCIES</code> declaration MUST use the <code>maven.dependencies</code> macro.</li>
 * <li>The <code>MAVEN_EXCLUSIONS</code> declaration MUST be a list of string following Maven exclusion syntax
 * (<code>groupId:artifactId</code>, <code>*</code> is supported only for artifact ids).</li>
 * <li>Version constants MUST start with the <code>'_'</code> prefix and MUST end with the <code>'_VERSION'</code>
 * suffix</li>
 * </ul>
 * <p>
 * This file is intentionally package private. It should not be modified outside of {@link MavenDependenciesCollection}.
 * </p>
 */
class MavenDependenciesFile {

    static class Reader extends StarlarkFileParser<MavenDependenciesFile> {

        public Reader(Path starlarkFile) throws IOException {
            super(starlarkFile);
        }

        protected MavenArtifact parseArtifact(StringLiteral stringLiteral) throws ParseException {
            MavenArtifact artifact;
            try {
                artifact = MavenArtifact.fromCoordinatesString(stringLiteral.getValue());
            } catch (IllegalArgumentException e) {
                throw new ParseException(e.getMessage(), stringLiteral);
            }
            return artifact;
        }

        protected MavenArtifact parseArtifactWithVersionVariable(
                BinaryOperatorExpression operatorExpression,
                SortedMap<String, String> versions) throws ParseException {
            if ((operatorExpression.getOperator() != TokenKind.PLUS)
                    || (operatorExpression.getY().kind() != Kind.IDENTIFIER)
                    || (operatorExpression.getX().kind() != Kind.STRING_LITERAL)) {
                throw new ParseException("unsupported operation", operatorExpression);
            }

            var versionVariable = ((Identifier) operatorExpression.getY()).getName();
            var versionValue = versions.get(versionVariable);
            if (versionValue == null) {
                throw new ParseException("version not defined", operatorExpression.getY());
            }

            MavenArtifact artifact;
            try {
                artifact = MavenArtifact.fromCoordinatesString(
                    ((StringLiteral) operatorExpression.getX()).getValue() + versionVariable);
            } catch (IllegalArgumentException e) {
                throw new ParseException(e.getMessage(), operatorExpression);
            }
            return artifact;
        }

        protected List<Exclusion> parseExclusionList(ListExpression listExpression) throws ParseException {
            var elements = listExpression.getElements();
            if (!elements.stream()
                    .allMatch(
                        e -> (e.kind() == Kind.STRING_LITERAL) || ((e.kind() == Kind.CALL)
                                && "maven.exclusion".equals(((CallExpression) e).getFunction().prettyPrint())))) {
                throw new ParseException("expected list of string or 'maven.exclusion' calls", listExpression);
            }

            List<Exclusion> exclusions = new ArrayList<>();
            for (Expression element : elements) {
                if (element.kind() == Kind.STRING_LITERAL) {
                    exclusions.add(Exclusion.fromCoordinatesString(((StringLiteral) element).getValue()));
                    continue;
                }

                var exclusionCall = (CallExpression) element;
                var exclusionCallArguments = exclusionCall.getArguments();
                var exclusionKeywordArgumentsMap = keywordArgumentsAsMap(exclusionCallArguments);

                String group;
                String artifact;
                List<Argument> positionals =
                        exclusionCallArguments.stream().filter(Positional.class::isInstance).collect(toList());
                if (!positionals.isEmpty()) {
                    var hasNotAllowedKeywords = exclusionKeywordArgumentsMap.containsKey(KEYWORD_GROUP)
                            || exclusionKeywordArgumentsMap.containsKey(KEYWORD_ARTIFACT);
                    if ((positionals.size() != 2L) || hasNotAllowedKeywords) {
                        throw new ParseException(
                                "'maven.exclusion' call expected to have exactly two positional argument and no keyword of 'group' or 'artifact'",
                                exclusionCall);
                    }
                    group = parseStringArgument(positionals.get(0));
                    artifact = parseStringArgument(positionals.get(1));
                } else {
                    var hasAllRequiredKeywords = exclusionKeywordArgumentsMap.containsKey(KEYWORD_GROUP)
                            && exclusionKeywordArgumentsMap.containsKey(KEYWORD_ARTIFACT);
                    if (!hasAllRequiredKeywords) {
                        throw new ParseException(
                                "'maven.exclusion' call expected to have required keyword arguments for 'group' and 'artifact'",
                                exclusionCall);
                    }

                    group = parseStringArgument(exclusionKeywordArgumentsMap.get(KEYWORD_GROUP));
                    artifact = parseStringArgument(exclusionKeywordArgumentsMap.get(KEYWORD_ARTIFACT));
                }

                exclusions.add(new Exclusion(group, artifact));
            }
            return exclusions;
        }

        protected List<Exclusion> parseOptionalExclusionsListArgument(Argument argument) throws ParseException {
            if (argument == null) {
                return null;
            }

            if (argument.getValue().kind() != Kind.LIST_EXPR) {
                throw new ParseException("expected list expression", argument);
            }

            return parseExclusionList((ListExpression) argument.getValue());
        }

        protected String parseStringOrVersionIdentifierArgument(
                Argument argument,
                SortedMap<String, String> versions) throws ParseException {
            if (argument.getValue().kind() == Kind.STRING_LITERAL) {
                return ((StringLiteral) argument.getValue()).getValue();
            }

            if (argument.getValue().kind() == Kind.IDENTIFIER) {
                var versionVariable = ((Identifier) argument.getValue()).getName();
                var versionValue = versions.get(versionVariable);
                if (versionValue == null) {
                    throw new ParseException("version not defined", argument);
                }

                return versionVariable;
            }

            throw new ParseException("expected string literal or variable reference", argument);
        }

        @Override
        public MavenDependenciesFile read() throws ParseException {
            SortedMap<String, String> versions = new TreeMap<>();
            SortedSet<MavenArtifact> importedBoms = new TreeSet<>();
            SortedSet<MavenArtifact> dependencies = new TreeSet<>();
            SortedSet<Exclusion> banned = new TreeSet<>();

            // extract and process all assignments
            List<AssignmentStatement> assignments = starlarkFile.getStatements()
                    .stream()
                    .filter(AssignmentStatement.class::isInstance)
                    .map(AssignmentStatement.class::cast)
                    .collect(toList());
            for (AssignmentStatement assignment : assignments) {
                if (assignment.getLHS().kind() != Kind.IDENTIFIER) {
                    throw new ParseException("left hand side of assignment must be an identifier", assignment.getLHS());
                }

                var identifier = ((Identifier) assignment.getLHS()).getName();

                if (identifier.startsWith("_") && identifier.endsWith("_VERSION")) {
                    // version: RHS must be a string literal
                    if (assignment.getRHS().kind() != Kind.STRING_LITERAL) {
                        throw new ParseException(
                                "right hand side of a version assignment must be a string literal",
                                assignment.getRHS());
                    }

                    var value = ((StringLiteral) assignment.getRHS()).getValue();
                    if ((value == null) || value.isBlank()) {
                        throw new ParseException("a version must not be blank or empty", assignment.getRHS());
                    }

                    versions.put(identifier, value);
                } else if (CONST_MAVEN_BOM_IMPORTS.equals(identifier)) {
                    // RHS must be maven.imports call
                    if (assignment.getRHS().kind() != Kind.CALL) {
                        throw new ParseException(
                                "right hand side of MAVEN_BOM_IMPORTS assignment must be a the 'maven.imports' call",
                                assignment.getRHS());
                    }

                    var call = (CallExpression) assignment.getRHS();
                    if ((call.getFunction().kind() != Kind.DOT)
                            || !MACRO_MAVEN_IMPORTS.equals(call.getFunction().prettyPrint())) {
                        throw new ParseException(
                                "right hand side of MAVEN_BOM_IMPORTS assignment must be a the 'maven.imports' call",
                                call.getFunction());
                    }

                    var arguments = call.getArguments();
                    if ((arguments.size() != 1) || !(arguments.get(0) instanceof Positional)) {
                        throw new ParseException(
                                "'maven.imports' call only allowed with exactly 1 positional argument",
                                assignment.getRHS());
                    }

                    if (arguments.get(0).getValue().kind() != Kind.LIST_EXPR) {
                        throw new ParseException("'maven.imports' must be called with a list", arguments.get(0));
                    }

                    var elements = ((ListExpression) arguments.get(0).getValue()).getElements();
                    for (Expression element : elements) {
                        if (element instanceof StringLiteral) {
                            var artifact = parseArtifact((StringLiteral) element);
                            if (!Objects.equals(artifact.getPackaging(), "pom")) {
                                throw new ParseException("Packaging for BOM import must be 'pom'", element);
                            }
                            importedBoms.add(artifact);
                        } else if (element instanceof BinaryOperatorExpression) {
                            var artifact =
                                    parseArtifactWithVersionVariable((BinaryOperatorExpression) element, versions);
                            if (!Objects.equals(artifact.getPackaging(), "pom")) {
                                throw new ParseException("Packaging for BOM import must be 'pom'", element);
                            }
                            importedBoms.add(artifact);
                        } else if (element instanceof CallExpression) {
                            var mavenBomCall = (CallExpression) element;
                            if ((mavenBomCall.getFunction().kind() != Kind.DOT)
                                    || !MACRO_MAVEN_BOM.equals(mavenBomCall.getFunction().prettyPrint())) {
                                throw new ParseException(
                                        "only 'maven.bom' call allowed for importing boms",
                                        mavenBomCall);
                            }
                            var mavenBomArguments = mavenBomCall.getArguments();
                            Map<String, Keyword> mavenBomArgumentMap = mavenBomArguments.stream()
                                    .filter(Keyword.class::isInstance)
                                    .map(Keyword.class::cast)
                                    .collect(toMap(Keyword::getName, Function.identity()));

                            String group;
                            String artifact;
                            String version;
                            List<Argument> positionals =
                                    mavenBomArguments.stream().filter(Positional.class::isInstance).collect(toList());
                            if (!positionals.isEmpty()) {
                                var hasNotAllowedKeywords = mavenBomArgumentMap.containsKey(KEYWORD_GROUP)
                                        || mavenBomArgumentMap.containsKey(KEYWORD_ARTIFACT)
                                        || mavenBomArgumentMap.containsKey(KEYWORD_VERSION);
                                if ((positionals.size() != 3L) || hasNotAllowedKeywords) {
                                    throw new ParseException(
                                            "'maven.bom' call expected to have exactly three positional argument and no keyword of 'group', 'artifact' or 'version'",
                                            mavenBomCall);
                                }
                                group = parseStringArgument(positionals.get(0));
                                artifact = parseStringArgument(positionals.get(1));
                                version = parseStringOrVersionIdentifierArgument(positionals.get(2), versions);
                            } else {
                                var hasAllRequiredKeywords = mavenBomArgumentMap.containsKey(KEYWORD_GROUP)
                                        && mavenBomArgumentMap.containsKey(KEYWORD_ARTIFACT)
                                        && mavenBomArgumentMap.containsKey(KEYWORD_VERSION);
                                if (!hasAllRequiredKeywords) {
                                    throw new ParseException(
                                            "'maven.bom' call expected to have required keyword arguments for 'group', 'artifact' and 'version'",
                                            mavenBomCall);
                                }

                                group = parseStringArgument(mavenBomArgumentMap.get(KEYWORD_GROUP));
                                artifact = parseStringArgument(mavenBomArgumentMap.get(KEYWORD_ARTIFACT));
                                version = parseStringOrVersionIdentifierArgument(
                                    mavenBomArgumentMap.get(KEYWORD_VERSION),
                                    versions);
                            }
                            importedBoms.add(
                                new MavenArtifact(
                                        group,
                                        artifact,
                                        version,
                                        "pom",
                                        parseOptionalStringArgument(mavenBomArgumentMap.get(KEYWORD_CLASSIFIER)),
                                        parseOptionalExclusionsListArgument(
                                            mavenBomArgumentMap.get(KEYWORD_EXCLUSIONS)),
                                        parseOptionalBoolArgument(mavenBomArgumentMap.get(KEYWORD_NEVERLINK), false),
                                        parseOptionalBoolArgument(mavenBomArgumentMap.get(KEYWORD_TESTONLY), false)));
                        } else {
                            throw new ParseException("unsupported expression", element);
                        }
                    }
                } else if (CONST_MAVEN_DEPENDENCIES.equals(identifier)) {
                    // RHS must be maven.dependencies call
                    if (assignment.getRHS().kind() != Kind.CALL) {
                        throw new ParseException(
                                "right hand side of MAVEN_DEPENDENCIES assignment must be a the 'maven.dependencies' call",
                                assignment.getRHS());
                    }

                    var call = (CallExpression) assignment.getRHS();
                    if ((call.getFunction().kind() != Kind.DOT)
                            || !MACRO_MAVEN_DEPENDENCIES.equals(call.getFunction().prettyPrint())) {
                        throw new ParseException(
                                "right hand side of MAVEN_DEPENDENCIES assignment must be a the 'maven.dependencies' call",
                                call.getFunction());
                    }

                    var arguments = call.getArguments();
                    if ((arguments.size() != 1) || !(arguments.get(0) instanceof Positional)) {
                        throw new ParseException(
                                "'maven.dependencies' call only allowed with exactly 1 positional argument",
                                assignment.getRHS());
                    }

                    if (arguments.get(0).getValue().kind() != Kind.LIST_EXPR) {
                        throw new ParseException("'maven.dependencies' must be called with a list", arguments.get(0));
                    }

                    var elements = ((ListExpression) arguments.get(0).getValue()).getElements();
                    for (Expression element : elements) {
                        if (element instanceof StringLiteral) {
                            dependencies.add(parseArtifact((StringLiteral) element));
                        } else if (element instanceof BinaryOperatorExpression) {
                            dependencies.add(
                                parseArtifactWithVersionVariable((BinaryOperatorExpression) element, versions));
                        } else if (element instanceof CallExpression) {
                            var mavenArtifactCall = (CallExpression) element;
                            if ((mavenArtifactCall.getFunction().kind() != Kind.DOT)
                                    || !MACRO_MAVEN_ARTIFACT.equals(mavenArtifactCall.getFunction().prettyPrint())) {
                                throw new ParseException(
                                        "only 'maven.artifact' call allowed for defining an artificat",
                                        mavenArtifactCall);
                            }
                            var mavenArtifactArguments = mavenArtifactCall.getArguments();
                            var mavenArgumentMap = keywordArgumentsAsMap(mavenArtifactArguments);

                            String group;
                            String artifact;
                            String version;
                            List<Argument> positionals = mavenArtifactArguments.stream()
                                    .filter(Positional.class::isInstance)
                                    .collect(toList());
                            if (!positionals.isEmpty()) {
                                var hasNotAllowedKeywords = mavenArgumentMap.containsKey(KEYWORD_GROUP)
                                        || mavenArgumentMap.containsKey(KEYWORD_ARTIFACT)
                                        || mavenArgumentMap.containsKey(KEYWORD_VERSION);
                                if ((positionals.size() != 3L) || hasNotAllowedKeywords) {
                                    throw new ParseException(
                                            "'maven.artifact' call expected to have exactly three positional argument and no keyword of 'group', 'artifact' or 'version'",
                                            mavenArtifactCall);
                                }
                                group = parseStringArgument(positionals.get(0));
                                artifact = parseStringArgument(positionals.get(1));
                                version = parseStringOrVersionIdentifierArgument(positionals.get(2), versions);
                            } else {
                                var hasAllRequiredKeywords = mavenArgumentMap.containsKey(KEYWORD_GROUP)
                                        && mavenArgumentMap.containsKey(KEYWORD_ARTIFACT)
                                        && mavenArgumentMap.containsKey(KEYWORD_VERSION);
                                if (!hasAllRequiredKeywords) {
                                    throw new ParseException(
                                            "'maven.artifact' call expected to have required keyword arguments for 'group', 'artifact' and 'version'",
                                            mavenArtifactCall);
                                }

                                group = parseStringArgument(mavenArgumentMap.get(KEYWORD_GROUP));
                                artifact = parseStringArgument(mavenArgumentMap.get(KEYWORD_ARTIFACT));
                                version = parseStringOrVersionIdentifierArgument(
                                    mavenArgumentMap.get(KEYWORD_VERSION),
                                    versions);
                            }
                            var packaging = parseOptionalStringArgument(mavenArgumentMap.get(KEYWORD_PACKAGING));
                            try {
                                dependencies.add(
                                    new MavenArtifact(
                                            group,
                                            artifact,
                                            version,
                                            (packaging != null) && !packaging.isEmpty() ? packaging
                                                    : MavenArtifact.DEFAULT_PACKAGING,
                                            parseOptionalStringArgument(mavenArgumentMap.get(KEYWORD_CLASSIFIER)),
                                            parseOptionalExclusionsListArgument(
                                                mavenArgumentMap.get(KEYWORD_EXCLUSIONS)),
                                            parseOptionalBoolArgument(mavenArgumentMap.get(KEYWORD_NEVERLINK), false),
                                            parseOptionalBoolArgument(mavenArgumentMap.get(KEYWORD_TESTONLY), false)));
                            } catch (IllegalArgumentException e) {
                                throw new ParseException(e.getMessage(), mavenArtifactCall);
                            }
                        } else {
                            throw new ParseException("unsupported expression", element);
                        }
                    }
                } else if (CONST_MAVEN_EXCLUSIONS.equals(identifier)) {
                    // RHS must be string list call
                    if (assignment.getRHS().kind() != Kind.LIST_EXPR) {
                        throw new ParseException(
                                "right hand side of BANNED_DEPENDENCIES assignment must be a list of strings following exclusion syntax",
                                assignment.getRHS());
                    }

                    banned.addAll(parseExclusionList((ListExpression) assignment.getRHS()));
                } else {
                    throw new ParseException("unexpected assignment", assignment.getLHS());
                }
            }

            return new MavenDependenciesFile(versions, importedBoms, dependencies, banned, starlarkFilePath);
        }

    }

    private static final String KEYWORD_GROUP = "group";
    private static final String KEYWORD_ARTIFACT = "artifact";
    private static final String KEYWORD_VERSION = "version";
    private static final String KEYWORD_TESTONLY = "testonly";
    private static final String KEYWORD_NEVERLINK = "neverlink";
    private static final String KEYWORD_EXCLUSIONS = "exclusions";
    private static final String KEYWORD_CLASSIFIER = "classifier";
    private static final String KEYWORD_PACKAGING = "packaging";

    private static final String MACRO_MAVEN_DEPENDENCIES = "maven.dependencies";
    private static final String MACRO_MAVEN_IMPORTS = "maven.imports";
    private static final String MACRO_MAVEN_ARTIFACT = "maven.artifact";
    private static final String MACRO_MAVEN_BOM = "maven.bom";

    private static final String CONST_MAVEN_BOM_IMPORTS = "MAVEN_BOM_IMPORTS";
    private static final String CONST_MAVEN_DEPENDENCIES = "MAVEN_DEPENDENCIES";
    private static final String CONST_MAVEN_EXCLUSIONS = "MAVEN_EXCLUSIONS";

    public static MavenDependenciesFile read(Path existingFile) throws IOException {
        return new Reader(existingFile).read();
    }

    private final SortedMap<String, String> versionVariables;
    private final SortedSet<MavenArtifact> importedBoms;
    private final SortedSet<MavenArtifact> dependencies;
    private final SortedSet<Exclusion> exclusions;
    private final Path file;
    private final String group;

    public MavenDependenciesFile(SortedMap<String, String> versionVariables, SortedSet<MavenArtifact> importedBoms,
            SortedSet<MavenArtifact> dependencies, SortedSet<Exclusion> bannedDependencies, Path file) {
        this.versionVariables = versionVariables;
        this.importedBoms = importedBoms;
        this.dependencies = dependencies;
        exclusions = bannedDependencies;
        this.file = file;

        var fileName = file.getFileName().toString();
        group = fileName.substring(0, fileName.length() - 4);
        if (group.isBlank()) {
            throw new IllegalArgumentException(
                    format("Invalid dependencies file:  file '%s' results in a blank group!", fileName));
        }

        if (!BazelConventions.toTargetName(group).equals(group)) {
            throw new IllegalArgumentException(
                    format("Invalid dependencies file:  file '%s' contains not allowed characters!", fileName));
        }
    }

    private StarlarkStringBuilder appendExclusion(StarlarkStringBuilder output, Exclusion exclusion) {
        return output.append('"')
                .append(exclusion.getGroupId())
                .append(':')
                .append(exclusion.getArtifactId())
                .append('"');
    }

    private StarlarkStringBuilder appendExclusions(StarlarkStringBuilder output, MavenArtifact a) {
        if ((a.getExclusions() != null) && !a.getExclusions().isEmpty()) {
            output.append(KEYWORD_EXCLUSIONS).append(" = [");
            if (a.getExclusions().size() == 1) {
                var e = a.getExclusions().first();
                appendExclusion(output, e);
            } else {
                output.appendNewline();
                output.increaseIndention();
                for (Exclusion e : a.getExclusions()) {
                    appendExclusion(output, e).appendCommaFollowedByNewline();
                }
                output.decreaseIndention();
            }
            output.append("]").appendCommaFollowedByNewline();
        }
        return output;
    }

    public SortedSet<MavenArtifact> getDependencies() {
        return dependencies;
    }

    public SortedSet<Exclusion> getExclusions() {
        return exclusions;
    }

    public Path getFile() {
        return file;
    }

    public String getGroup() {
        return group;
    }

    public SortedSet<MavenArtifact> getImportedBoms() {
        return importedBoms;
    }

    /**
     * Returns a version variable value
     *
     * @param name
     *            the variable name
     * @return the version value (maybe <code>null</code>)
     */
    public String getVersion(String name) {
        return versionVariables.get(name);
    }

    /**
     * Provides access to the underlying variable map.
     * <p>
     * This should only be used for tests.
     * </p>
     *
     * @return the underlying variable map
     */
    SortedMap<String, String> getVersionVariables() {
        return versionVariables;
    }

    public CharSequence prettyPrint(String labelForLoadingMavenSymbol, String preamble, boolean conciseFormat) {
        var output = new StarlarkStringBuilder(4);

        output.append("load(\"")
                .append(requireNonNull(labelForLoadingMavenSymbol, "missing label for loading maven symbol"))
                .append("\", \"maven\")")
                .appendNewline();
        output.appendNewline();

        if ((preamble != null) && !preamble.isBlank()) {
            output.append(preamble);
            output.appendNewline();
        }

        for (Entry<String, String> versionVariable : versionVariables.entrySet()) {
            output.append(versionVariable.getKey())
                    .append(" = ")
                    .appendQuoted(versionVariable.getValue())
                    .appendNewline();
        }

        output.appendNewline();
        output.append(CONST_MAVEN_BOM_IMPORTS).append(" = ").append(MACRO_MAVEN_IMPORTS).append("([").appendNewline();
        output.increaseIndention();
        for (MavenArtifact a : importedBoms) {
            if (conciseFormat && (a.getExclusions() == null) && (a.getClassifier() == null)) {
                output.append(MACRO_MAVEN_BOM)
                        .append("(")
                        .appendQuoted(a.getGroupId())
                        .append(", ")
                        .appendQuoted(a.getArtifactId())
                        .append(", ");
                if (versionVariables.containsKey(a.getVersion())) {
                    output.append(a.getVersion());
                } else {
                    output.appendQuoted(a.getVersion());
                }
            } else {
                output.append(MACRO_MAVEN_BOM).append("(").appendNewline();
                output.increaseIndention();
                output.append(KEYWORD_GROUP).append(" = ").appendQuoted(a.getGroupId()).appendCommaFollowedByNewline();
                output.append(KEYWORD_ARTIFACT)
                        .append(" = ")
                        .appendQuoted(a.getArtifactId())
                        .appendCommaFollowedByNewline();
                if (a.getClassifier() != null) {
                    output.append(KEYWORD_CLASSIFIER)
                            .append(" = ")
                            .appendQuoted(a.getClassifier())
                            .appendCommaFollowedByNewline();
                }
                output.append(KEYWORD_VERSION).append(" = ");
                if (versionVariables.containsKey(a.getVersion())) {
                    output.append(a.getVersion());
                } else {
                    output.appendQuoted(a.getVersion());
                }
                output.appendCommaFollowedByNewline();
                appendExclusions(output, a);
                output.decreaseIndention();
            }
            output.append(")").appendCommaFollowedByNewline();
        }
        output.decreaseIndention();
        output.append("])").appendNewline();

        output.appendNewline();
        output.append(CONST_MAVEN_DEPENDENCIES)
                .append(" = ")
                .append(MACRO_MAVEN_DEPENDENCIES)
                .append("([")
                .appendNewline();
        output.increaseIndention();
        for (MavenArtifact a : dependencies) {
            if (conciseFormat && (a.getExclusions() == null) && (a.isJarPackaging() || (a.getClassifier() != null))
                    && !a.isNeverlink() && !a.isTestonly()) {
                output.append('"').append(a.getGroupId()).append(':').append(a.getArtifactId()).append(':');
                if (a.getClassifier() != null) {
                    // when classifier is there packaging MUST be preceeding it
                    output.append(a.getPackaging()).append(':').append(a.getClassifier()).append(':');
                } else if (!a.isJarPackaging()) {
                    // otherwise only add packaging if it's different from default
                    output.append(a.getPackaging()).append(':');
                }
                if (versionVariables.containsKey(a.getVersion())) {
                    output.append("\" + ").append(a.getVersion());
                } else {
                    output.append(a.getVersion()).append('"');
                }
                output.appendCommaFollowedByNewline();
            } else {
                output.append(MACRO_MAVEN_ARTIFACT).append("(").appendNewline();
                output.increaseIndention();
                output.append(KEYWORD_GROUP).append(" = ").appendQuoted(a.getGroupId()).appendCommaFollowedByNewline();
                output.append(KEYWORD_ARTIFACT)
                        .append(" = ")
                        .appendQuoted(a.getArtifactId())
                        .appendCommaFollowedByNewline();
                if (!a.isJarPackaging() || !conciseFormat) {
                    // when concise format is enabled we want packaging *only* when it's not "jar"
                    output.append(KEYWORD_PACKAGING)
                            .append(" = ")
                            .appendQuoted(a.getPackaging())
                            .appendCommaFollowedByNewline();
                }
                if (a.getClassifier() != null) {
                    output.append(KEYWORD_CLASSIFIER)
                            .append(" = ")
                            .appendQuoted(a.getClassifier())
                            .appendCommaFollowedByNewline();
                }
                output.append(KEYWORD_VERSION).append(" = ");
                if (versionVariables.containsKey(a.getVersion())) {
                    output.append(a.getVersion());
                } else {
                    output.appendQuoted(a.getVersion());
                }
                output.appendCommaFollowedByNewline();
                appendExclusions(output, a);
                if (a.isNeverlink()) {
                    output.appendBooleanAttribute(KEYWORD_NEVERLINK, true).appendCommaFollowedByNewline();
                }
                if (a.isTestonly()) {
                    output.appendBooleanAttribute(KEYWORD_TESTONLY, true).appendCommaFollowedByNewline();
                }
                output.decreaseIndention();
                output.append(")").appendCommaFollowedByNewline();
            }
        }
        output.decreaseIndention();
        output.append("])").appendNewline();

        output.appendNewline();
        output.append(CONST_MAVEN_EXCLUSIONS).append(" = ").append("[").appendNewline();
        output.increaseIndention();
        for (Exclusion bannedDependency : exclusions) {
            appendExclusion(output, bannedDependency).appendCommaFollowedByNewline();
        }
        output.decreaseIndention();
        output.append("]").appendNewline();

        return output.toString();
    }

    /**
     * Sets a version variable value.
     *
     * @param name
     *            the variable name
     * @param value
     *            the variable value
     * @return the previous value (maybe <code>null</code>)
     */
    public String setVersion(String name, String value) {
        return versionVariables.put(name, value);
    }

    @Override
    public String toString() {
        return "StarlarkDependenciesFile [" + file + "]";
    }
}
