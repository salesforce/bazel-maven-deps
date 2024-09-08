package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;

import picocli.CommandLine.Command;
import picocli.CommandLine.ScopeType;

@Command(name = "remove-exclusion", description = "Remove an exclusion from //third_party/dependencies/*.bzl files.", scope = ScopeType.INHERIT)
public class RemoveExclusionCommand extends BaseDependencyCommand {

    public static void main(String[] args) {
        execute(new RemoveExclusionCommand(), args);
    }

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws Exception {
        final var coordinates = getArtifactCoordinatesFromArguments(out, false);
        final var exclusion = new MavenArtifact.Exclusion(coordinates.groupId, coordinates.artifactId);

        if (!mavenDependenciesCollection.removeExclusion(exclusion)) {
            throw new IllegalStateException(
                    format("Unable to remove exclusion '%s'. Does it exist?", exclusion.toString()));
        }
        out.important(format("Removed exclusion '%s'", exclusion.toString()));

        final var saveResult = saveStarlarkDependenciesFile(out);
        printSaveResult(out, saveResult, null);

        return 0;
    }
}
