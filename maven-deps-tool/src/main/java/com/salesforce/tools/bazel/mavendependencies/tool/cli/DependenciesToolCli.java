package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import java.util.concurrent.Callable;

import com.salesforce.tools.bazel.cli.BaseCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/**
 * Uber command for the Core Build Graph tool CLI
 */
@Command(name = "dependencies-tool", synopsisSubcommandLabel = "COMMAND", subcommands = { //@formatter:off
        LintDependencyCollectionCommand.class,

        PinDependenciesCommand.class,
        PrintDependencyCatalogCommand.class,

        GetVersionVariableValueCommand.class,
        SetDependencyVersionCommand.class,
        SetVersionVariableCommand.class,
        AddDependencyCommand.class,
        RemoveDependencyCommand.class,

        AddExclusionCommand.class,
        RemoveExclusionCommand.class,

        DependencyInfoCommand.class,
}) //@formatter:on

public class DependenciesToolCli implements Callable<Integer> {

    public static void main(final String... args) {
        BaseCommand.execute(new DependenciesToolCli(), args);
    }

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Prints this help text")
    boolean helpRequested;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        throw new ParameterException(spec.commandLine(), "Something went wrong. Did you specify a subcommand?");
    }

}
