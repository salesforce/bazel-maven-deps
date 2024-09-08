package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;

import picocli.CommandLine.Command;
import picocli.CommandLine.ScopeType;

@Command(name = "remove-dependency", description = "Remove a dependency from //third_party/dependencies/*.bzl files.", scope = ScopeType.INHERIT)
public class RemoveDependencyCommand extends BaseDependencyCommand {

    public static void main(String[] args) {
        execute(new RemoveDependencyCommand(), args);
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

        if (mavenDependenciesCollection.removeDependency(artifact)) {
            out.important(format("Removed dependency '%s'", coordinatesStringWithoutVersion));
        } else if (mavenDependenciesCollection.removeImportedBom(artifact)) {
            out.important(format("Removed imported BOM '%s'", coordinatesStringWithoutVersion));
        } else {
            throw new IllegalStateException(
                    format(
                        "Unable to remove '%s' from either dependencies or imported BOM list. Something is wrong!",
                        artifact));
        }

        final var version = artifact.getVersion();
        if (mavenDependenciesCollection.hasVersionVariable(version)) {
            final var versionStillInUse =
                    mavenDependenciesCollection.getAllArtifacts().anyMatch(a -> a.getVersion().equals(version));
            if (!versionStillInUse) {
                mavenDependenciesCollection.removeVersionVariable(version);
                out.important(format("Removed version variable '%s'", version));
            } else {
                out.info(format("Keeping version variable '%s' because it's still used.", version));
            }
        }

        final var saveResult = saveStarlarkDependenciesFile(out);
        printSaveResult(out, saveResult, null);

        return 0;
    }
}
