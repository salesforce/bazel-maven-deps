package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Locale;
import java.util.SortedSet;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact.Exclusion;
import com.salesforce.tools.bazel.mavendependencies.starlark.BazelConventions;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
        name = "add-dependency",
        description = "Add a dependency to //third_party/dependencies/*.bzl files.",
        scope = ScopeType.INHERIT)
public class AddDependencyCommand extends BaseDependencyCommand {

    public static void main(String[] args) {
        execute(new AddDependencyCommand(), args);
    }

    @Option(
            names = { "--add-exclude" },
            description = "Maven coordinates of an exclude to add to the artifact",
            converter = ExclusionConverter.class)
    private SortedSet<Exclusion> exclusions;

    @Option(
            names = { "--neverlink" },
            description = "flag this dependency as a runtime only dependency (see Bazel docs)")
    private boolean neverlink;

    @Option(
            names = { "--testonly" },
            description = "flag this dependency as a test only dependency (see Bazel docs)")
    private boolean testonly;

    @Option(
            names = { "--skip-similar-version-check-I-understand-the-consequences" },
            description = "skips the check for similar managed versions in dependencies.bzl")
    private boolean skipSimilarVersionCheck;

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws Exception {
        final var input = getMavenArtifactToAdd(out);

        // check if this is a managed version
        final String versionName, versionValue;
        if (mavenDependenciesCollection.hasVersionVariable(input.getVersion())) {
            versionName = input.getVersion();
            versionValue = mavenDependenciesCollection.getVersionVariableValue(versionName);
            out.info(format("Confirmed existing version variable '%s' (%s)", versionName, versionValue));
        } else {
            // create new managed version
            versionName =
                    "_" + BazelConventions.toTargetName(input.getArtifactId()).toUpperCase(Locale.US) + "_VERSION";
            versionValue = input.getVersion();
            out.info(format("Using version variable '%s' (%s)", versionName, versionValue));

            final var existingVersion = mavenDependenciesCollection.getVersionVariableValue(versionName);
            if ((existingVersion != null) && !existingVersion.equals(input.getVersion())) {
                throw new IllegalStateException(
                        format("There is already an existing version: '%s' = '%s'", versionName, existingVersion));
            }
            if (existingVersion != null) {
                out.info("Found existing managed version: " + versionName);
            } else if (!skipSimilarVersionCheck) {
                final List<String> similarExistingVersions = mavenDependenciesCollection.getVersionVariableNames()
                        .parallel()
                        .map(s -> s.replace("_VERSION", ""))
                        .filter(s -> versionName.startsWith(s))
                        .collect(toList());
                if (similarExistingVersions.size() > 1) {
                    throw new IllegalStateException(
                            format(
                                "There are %d similar versions: %s%nPlease review if any of these versions can be used.%nSpecifcy '--skip-similar-version-check-I-understand-the-consequences' if you want to proceed and confirmed the new version as not conflicting.",
                                similarExistingVersions.size(),
                                similarExistingVersions.stream().sorted().collect(joining(", "))));
                }
                if (similarExistingVersions.size() == 1) {
                    throw new IllegalStateException(
                            format(
                                "There is one similar version: %s%nPlease review if this version can be used.%nSpecifcy '--skip-similar-version-check-I-understand-the-consequences' if you want to proceed and confirmed the new version as not conflicting.",
                                similarExistingVersions.stream().findAny().get()));
                }
                out.info("No similar version detected");
            } else {
                out.info(
                    "Skipping similar version detection because user confirmed via command line flag that no conflict exists.");
            }
        }

        final var artifact = new MavenArtifact(
                input.getGroupId(),
                input.getArtifactId(),
                versionName,
                input.getPackaging(),
                input.getClassifier(),
                exclusions,
                neverlink,
                testonly);

        mavenDependenciesCollection.addDependencyWithManagedVersion(artifact, versionName, versionValue);

        out.important(format("Added dependency '%s' to dependencies collection", artifact.toCoordinatesString()));

        final var saveResult = saveStarlarkDependenciesFile(out);
        printSaveResult(out, saveResult, null);

        return 0;
    }

    private MavenArtifact getMavenArtifactToAdd(MessagePrinter out) {
        final var coordinates = getCoordinatesFromParameterOrOption();
        if ((coordinates != null) && coordinates.contains(":") && (coordinates.split(":").length >= 3)) {
            return MavenArtifact.fromCoordinatesString(coordinates);
        }

        try {
            return getArtifactFromArguments(out);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to figure out what artifact to add. Please specify a groupId, an artifactId and a version!");
        }
    }
}
