package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenConventions;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "get-version-variable", description = "Returns the value of a version variable in //third_party/dependencies/*.bzl files.")
public class GetVersionVariableValueCommand extends BaseCommandUsingDependencyCollection {

    public static void main(String[] args) {
        execute(new GetVersionVariableValueCommand(), args);
    }

    @Option(names = { "--variable-name" }, description = "Name of the version variable", required = true)
    private String versionVariableName;

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws Exception {
        // this is most likely used in scripts, turn of any noise
        printFeedbackNotice = false;

        final var bazelVersionVariableName = MavenConventions.toBazelVersionVariableName(versionVariableName);
        if (verbose) {
            out.info(format("Starlark version variable for '%s': %s", versionVariableName, bazelVersionVariableName));
        }

        if (!mavenDependenciesCollection.hasVersionVariable(bazelVersionVariableName)) {
            throw new IllegalStateException(
                    format("Version variable '%s' not found in dependencies collection!", bazelVersionVariableName));
        }

        out.info(mavenDependenciesCollection.getVersionVariableValue(bazelVersionVariableName));

        return 0;
    }
}
