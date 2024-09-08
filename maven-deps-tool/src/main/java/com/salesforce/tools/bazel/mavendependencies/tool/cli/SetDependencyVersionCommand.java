package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(name = "set-dependency-version", description = "Set a dependency version in //third_party/dependencies/*.bzl files.", scope = ScopeType.INHERIT)
public class SetDependencyVersionCommand extends BaseDependencyCommand {

    public static void main(String[] args) {
        execute(new SetDependencyVersionCommand(), args);
    }

    @Option(names = { "--new-version" }, description = "New version to set", required = true)
    private String newVersion;

    private MavenArtifact createNewFromExisting(MavenArtifact artifact, String newVersion) {
        return new MavenArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                newVersion,
                artifact.getPackaging(),
                artifact.getClassifier(),
                artifact.getExclusions(),
                artifact.isNeverlink(),
                artifact.isTestonly());
    }

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws Exception {
        final var coordinatesStringWithoutVersion = getArtifactFromArguments(out).toCoordinatesStringWithoutVersion();
        final var artifact = mavenDependenciesCollection.findArtifact(coordinatesStringWithoutVersion);
        if (artifact == null) {
            throw new IllegalArgumentException(
                    format(
                        "Artifact with coordinates '%s' not defined in the dependencies collection!",
                        coordinatesStringWithoutVersion));
        }

        final var version = artifact.getVersion();
        boolean changed;
        if (mavenDependenciesCollection.hasVersionVariable(version)) {
            if (verbose) {
                out.info(format("Artifact version identified as version variable '%s'", version));
            }
            changed = updateVersionVariable(out, version, newVersion);
        } else if (mavenDependenciesCollection.removeDependency(artifact)) {
            changed = true;
            final var newArtifact = createNewFromExisting(artifact, newVersion);
            mavenDependenciesCollection.addDependency(newArtifact);
            out.important(
                format(
                    "Updated dependency version '%s': '%s' -> '%s'",
                    newArtifact.toCoordinatesStringWithoutVersion(),
                    artifact.getVersion(),
                    newVersion));
        } else if (mavenDependenciesCollection.removeImportedBom(artifact)) {
            changed = true;
            final var newArtifact = createNewFromExisting(artifact, newVersion);
            mavenDependenciesCollection.addImportedBom(newArtifact);
            out.important(
                format(
                    "Updated imported BOM version '%s': '%s' -> '%s'",
                    newArtifact.toCoordinatesStringWithoutVersion(),
                    artifact.getVersion(),
                    newVersion));
        } else {
            throw new IllegalStateException(
                    format(
                        "Unable to remove '%s' from either dependencies or imported BOM list. Something is wrong!",
                        artifact));
        }

        if (changed) {
            final var saveResult = saveStarlarkDependenciesFile(out);
            printSaveResult(out, saveResult, null);
        }

        return 0;
    }
}
