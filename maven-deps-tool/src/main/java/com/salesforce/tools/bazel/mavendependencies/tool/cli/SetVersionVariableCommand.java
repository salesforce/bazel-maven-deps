package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenConventions;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(name = "set-version-variable", description = "Set a version variable in //third_party/dependencies/*.bzl files.", scope = ScopeType.INHERIT)
public class SetVersionVariableCommand extends BaseCommandUsingDependencyCollection {

    public static void main(String[] args) {
        execute(new SetVersionVariableCommand(), args);
    }

    @Option(names = {
            "--variable-name" }, description = "Name of the version variable", required = true, scope = ScopeType.INHERIT)
    protected String versionVariableName;

    @Option(names = { "--new-version" }, description = "New version to set", required = true, scope = ScopeType.INHERIT)
    protected String newVersion;

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws Exception {
        final var bazelVersionVariableName = MavenConventions.toBazelVersionVariableName(versionVariableName);
        if (verbose) {
            out.info(format("Starlark version variable for '%s': %s", versionVariableName, bazelVersionVariableName));
        }

        if (!mavenDependenciesCollection.hasVersionVariable(bazelVersionVariableName)) {
            throw new IllegalStateException(
                    format("Version variable '%s' not found in dependencies collection!", bazelVersionVariableName));
        }

        final var changed = updateVersionVariable(out, bazelVersionVariableName, newVersion);

        if (changed) {
            final var saveResult = saveStarlarkDependenciesFile(out);
            printSaveResult(out, saveResult, null);
        }

        return 0;
    }
}
