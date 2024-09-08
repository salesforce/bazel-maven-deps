package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;

import picocli.CommandLine.Command;
import picocli.CommandLine.ScopeType;

@Command(name = "add-exclusion", description = "Add an exclusion to //third_party/dependencies/*.bzl files.", scope = ScopeType.INHERIT)
public class AddExclusionCommand extends BaseDependencyCommand {

    public static void main(String[] args) {
        execute(new AddExclusionCommand(), args);
    }

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws Exception {
        final var coordinates = getArtifactCoordinatesFromArguments(out, false);
        final var exclusion = new MavenArtifact.Exclusion(coordinates.groupId, coordinates.artifactId);

        mavenDependenciesCollection.addExclusion(exclusion);

        out.important(format("Added exclusion '%s' to dependencies collection", exclusion.toString()));

        final var saveResult = saveStarlarkDependenciesFile(out);
        printSaveResult(out, saveResult, null);

        return 0;
    }
}
