package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionDelta.Modification;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact.Exclusion;

import picocli.CommandLine.Command;

@Command(
        name = "lint-dependency-collection",
        description = "Reformat and check the //third_party/dependencies/*.bzl files.")
public class LintDependencyCollectionCommand extends BaseCommandUsingDependencyCollection {

    public static void main(String[] args) {
        execute(new LintDependencyCollectionCommand(), args);
    }

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws Exception {
        // rebuild content map to enforce new grouping
        final Map<String, String> versionVariables = mavenDependenciesCollection.getVersionVariableNames()
                .collect(toMap(identity(), mavenDependenciesCollection::getVersionVariableValue));
        final SortedSet<MavenArtifact> newImportedBoms = new TreeSet<>(mavenDependenciesCollection.getImportedBoms());
        final SortedSet<MavenArtifact> newDependencies =
                new TreeSet<>(mavenDependenciesCollection.getAllDependencies());
        final SortedSet<Exclusion> newExclusions =
                mavenDependenciesCollection.getGlobalExclusions().collect(toCollection(TreeSet::new));

        out.important(format("%nNo problems found.%n"));

        var delta = mavenDependenciesCollection
                .replaceContent(versionVariables, newImportedBoms, newDependencies, newExclusions);
        if (!delta.modifications.isEmpty()) {
            out.warning(
                format(
                    "The following changes were detected:%n%s%n",
                    delta.modifications.stream()
                            .map(Modification::toString)
                            .collect(joining(System.lineSeparator() + " ", " ", ""))));
        }

        final var saveResult = saveStarlarkDependenciesFile(out);
        printSaveResult(
            out,
            saveResult,
            delta.obsoleteGroups.stream().map(mavenDependenciesCollection::getGroupFileLocation));

        return delta.modifications.isEmpty() ? 0 : 2;
    }
}
