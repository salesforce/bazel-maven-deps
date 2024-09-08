package com.salesforce.tools.bazel.mavendependencies.visibility;

import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;

import picocli.CommandLine.Model.CommandSpec;

/**
 * Interface to allow submission of a service factory for creating visibility providers at runtime.
 * <p>
 * The dependency tool does not include an implementation by default but uses Java's {@link ServiceLoader} pattern to
 * discover a factory at runtime.
 * </p>
 */
public interface VisibilityProviderFactory {

    /**
     * @return the single available factory
     * @throws IllegalStateException
     *             if multiple factories are found (potential classpath issue)
     */
    static Optional<VisibilityProviderFactory> findSingle() {
        final ServiceLoader<VisibilityProviderFactory> serviceLoader =
                ServiceLoader.load(VisibilityProviderFactory.class);

        // check for more than one
        List<Provider<VisibilityProviderFactory>> allFound = serviceLoader.stream().collect(toList());
        if (allFound.size() > 1) {
            var multipleScanners = new StringBuilder();
            multipleScanners.append("There are multiple vulnerability scanners available on the classpath:")
                    .append(System.lineSeparator());
            for (Provider<VisibilityProviderFactory> provider : allFound) {
                multipleScanners.append(" - ");
                multipleScanners.append(provider.get().getClass());
                multipleScanners.append(System.lineSeparator());
            }
            multipleScanners.append(System.lineSeparator());
            multipleScanners.append("This is a deployment issue. Please cleanup the classpath!")
                    .append(System.lineSeparator());
            multipleScanners.append(System.lineSeparator());
            multipleScanners.append(System.lineSeparator());
            throw new IllegalStateException(multipleScanners.toString());
        }

        return serviceLoader.findFirst();
    }

    /**
     * Creates and returns a new visibility provider printing to the specified output.
     *
     * @param out
     *            the output the scanner shall report and findings to (never <code>null</code>)
     * @param spec
     *            PicoCli command spec for additional information
     * @param verbose
     *            <code>true</code> if additional (verbose) output was requested for the command execution
     * @param workspaceRoot
     *            the root of the Bazel workspace
     * @return the provider instances (must not be <code>null</code>)
     */
    VisibilityProvider create(MessagePrinter out, CommandSpec spec, boolean verbose, Path workspaceRoot);
}
