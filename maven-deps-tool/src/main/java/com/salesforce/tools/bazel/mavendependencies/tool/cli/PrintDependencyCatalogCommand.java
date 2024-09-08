package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;
import static java.nio.file.Files.writeString;

import java.nio.file.Path;
import java.util.Comparator;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelDependenciesCatalog;
import com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "print-dependency-catalog", description = "Prints the content of the pinned catalog.")
public class PrintDependencyCatalogCommand extends BaseCommandUsingDependencyCollection {

    public static void main(String[] args) {
        execute(new PrintDependencyCatalogCommand(), args);
    }

    @Option(names = "--out", description = "path to file to write the content into")
    protected Path file;

    private BazelDependenciesCatalog catalog;

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws Exception {
        printFeedbackNotice = false; // do not print a notice by default

        // load existing catalog
        catalog = new BazelDependenciesCatalog(workspaceRoot);
        catalog.load();

        final var content = new StringBuilder();

        catalog.getAllImports().sorted(Comparator.comparing(BazelJavaDependencyImport::getName)).forEach(dep -> {
            content.append(dep.getName())
                    .append(" = ")
                    .append(dep.getArtifact().toCoordinatesString())
                    .append(System.lineSeparator());
        });

        if (file != null) {
            writeString(file, content);
            out.important(format("Wrote '%s'", file));
        } else {
            out.info(content.toString());
        }

        return 0;
    }

}