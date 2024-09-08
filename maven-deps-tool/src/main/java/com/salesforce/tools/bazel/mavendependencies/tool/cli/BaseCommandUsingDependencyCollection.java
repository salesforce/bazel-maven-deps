package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static com.salesforce.tools.bazel.mavendependencies.collection.MavenDependenciesCollection.getRecommendedPreamble;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import com.salesforce.tools.bazel.cli.BaseCommandWithWorkspaceRoot;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.cli.scm.NaiveScmTool;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionSaveResult;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenDependenciesCollection;
import com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.MavenDepsRepoSys;
import com.salesforce.tools.bazel.mavendependencies.vulnerabilities.NoOpVulnerabilityScanner;
import com.salesforce.tools.bazel.mavendependencies.vulnerabilities.VulnerabilityScanner;
import com.salesforce.tools.bazel.mavendependencies.vulnerabilities.VulnerabilityScannerFactory;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

/**
 * Base class for a command working with dependencies file
 */
public abstract class BaseCommandUsingDependencyCollection extends BaseCommandWithWorkspaceRoot {

    @Option(names = {
            "--maven-repository" }, description = "one or more URLs to additional Maven repositories", scope = ScopeType.INHERIT)
    protected SortedSet<String> mavenRepositories;

    @Option(names = {
            "--maven-central" }, description = "URLs to Maven central", defaultValue = "https://repo1.maven.org/maven2/", scope = ScopeType.INHERIT)
    protected String mavenCenteralUrl;

    @Option(names = {
            "--downloader-config" }, description = "Rewriting of download URLs (Bazel's --experimental_downloader_config option)", scope = ScopeType.INHERIT)
    protected Path downloaderConfig;

    @Option(names = {
            "--local-maven-repository" }, description = "Path to the local Maven repository (defaults to ~/.m2/repository if not set)", scope = ScopeType.INHERIT)
    protected Path localMavenRepository;

    @Option(names = "--label-for-loading-maven-symbol", description = "label for loading the maven symbol (defaults to '@bazel_maven_deps//bazel:defs.bzl')", defaultValue = "@bazel_maven_deps//bazel:defs.bzl", scope = ScopeType.INHERIT)
    protected String labelForLoadingMavenSymbol;

    @Option(names = "--collection-preamble", description = "preamble text to inject into the collection.bzl files after the load statement (of none is provided a recommended default will be generated)", required = false, scope = ScopeType.INHERIT)
    protected String collectionPreamble;

    protected MavenDependenciesCollection mavenDependenciesCollection;

    private MavenDepsRepoSys mavenRepositorySystem;
    private VulnerabilityScanner vulnerabilityScanner;

    @Spec
    CommandSpec spec;

    @Override
    @VisibleForTesting
    protected MessagePrinter createMessagePrinter() {
        // overridden for test visibility
        return super.createMessagePrinter();
    }

    /**
     * Called after the dependencies file was read
     *
     * @param out
     * @return
     */
    protected abstract int doExecuteCommand(MessagePrinter out) throws Exception;

    @Override
    protected final int executeCommand(MessagePrinter out) throws Exception {
        // load dependencies
        mavenDependenciesCollection = new MavenDependenciesCollection(workspaceRoot);
        mavenDependenciesCollection.load();

        // execute
        return doExecuteCommand(out);
    }

    protected MavenDepsRepoSys getRepoSys() {
        if (mavenRepositorySystem != null) {
            return mavenRepositorySystem;
        }
        return mavenRepositorySystem = newRepoSys();
    }

    @Override
    protected NaiveScmTool getScmTool() {
        // overridden so it becomes visible to our tests
        return super.getScmTool();
    }

    protected VulnerabilityScanner getVulnerabilityScanner(MessagePrinter out) {
        if (vulnerabilityScanner != null) {
            return vulnerabilityScanner;
        }

        final var factory = VulnerabilityScannerFactory.findSingle();
        if (factory.isPresent()) {
            return vulnerabilityScanner = Objects.requireNonNull(factory.get().create(out, spec, verbose));
        }
        return vulnerabilityScanner = new NoOpVulnerabilityScanner(out);
    }

    /**
     * Creates a new {@link MavenDepsRepoSys} instance when needed. This is called by {@link #getRepoSys()} and should
     * not be called by someone else.
     * <p>
     * Subclasses can override when they are sure they need a different {@link MavenDepsRepoSys}.
     * </p>
     *
     * @return new {@link MavenDepsRepoSys} instance
     */
    protected MavenDepsRepoSys newRepoSys() {
        // resolve download config file *within* workspace
        var absoluteDownloaderConfig = downloaderConfig;
        if ((absoluteDownloaderConfig != null) && !absoluteDownloaderConfig.isAbsolute()) {
            absoluteDownloaderConfig = workspaceRoot.resolve(absoluteDownloaderConfig);
        }

        LOG.debug("Creating repository system with download config: {}", absoluteDownloaderConfig);

        return new MavenDepsRepoSys(
                absoluteDownloaderConfig,
                mavenCenteralUrl,
                mavenRepositories,
                localMavenRepository);
    }

    protected void printSaveResult(MessagePrinter out, CollectionSaveResult saveResult, Stream<Path> obsoleteFiles) {
        final SortedSet<Path> remainingObsoleteFiles = new TreeSet<>();
        if (obsoleteFiles != null) {
            obsoleteFiles.forEach(remainingObsoleteFiles::add);
        }

        if (saveResult != null) {
            if (saveResult.writtenFiles.size() > 0) {
                out.info(
                    format(
                        "%nThe following files were updated:%n%s%n",
                        saveResult.writtenFiles.stream()
                                .map(Path::toString)
                                .collect(joining(System.lineSeparator() + " > ", " > ", ""))));
            }
            if (saveResult.deletedFiles.size() > 0) {
                out.info(format("%nThe following files were deleted:%n%s%n", saveResult.deletedFiles.stream().map(p -> {
                    remainingObsoleteFiles.remove(p);
                    return p.toString();
                }).collect(joining(System.lineSeparator() + " - ", " - ", ""))));
            }
            remainingObsoleteFiles.addAll(saveResult.obsoleteFiles);
        }

        if (!remainingObsoleteFiles.isEmpty()) {
            out.info(
                format(
                    "%nPlease delete the following obsolete files:%n%s%n",
                    remainingObsoleteFiles.stream()
                            .map(Path::toString)
                            .collect(joining(System.lineSeparator() + " > ", " > ", ""))));
        }
    }

    protected CollectionSaveResult saveStarlarkDependenciesFile(MessagePrinter out) throws Exception {
        try (var monitor = out.progressMonitor("Saving dependencies collection")) {
            return mavenDependenciesCollection.save(
                labelForLoadingMavenSymbol,
                collectionPreamble != null ? collectionPreamble : getRecommendedPreamble(),
                true /* concise */,
                false /* don't skip index */,
                monitor,
                getScmTool());
        }
    }

    /**
     * Resolves a version of an artifact contained in the collection.
     *
     * @param artifact
     *            a {@link MavenArtifact} from the {@link MavenDependenciesCollection}
     * @return the {@link MavenArtifact#toCoordinatesString()} replacing a known version variable with the value set in
     *         the collection
     */
    protected String toCoordinatesWithResolvedVersion(MavenArtifact artifact) {
        final var result = new StringBuilder();
        result.append(artifact.toCoordinatesStringWithoutVersion()).append(":");
        final var version = mavenDependenciesCollection.getVersionVariableValue(artifact.getVersion());
        if (version != null) {
            result.append(version);
        } else {
            result.append(artifact.getVersion());
        }
        return result.toString();
    }

    protected boolean updateVersionVariable(MessagePrinter out, String versionVariableName, String newVersionValue) {
        final var existingValue = mavenDependenciesCollection.getVersionVariableValue(versionVariableName);
        if (Objects.equals(existingValue, newVersionValue)) {
            out.info(
                format(
                    "No update of version variable '%s' necessary. Existing value already matches new value: '%s' == '%s'",
                    versionVariableName,
                    existingValue,
                    newVersionValue));
            return false;
        }

        final var oldValue = mavenDependenciesCollection.updateVersionVariable(versionVariableName, newVersionValue);
        out.important(
            format("Updated version variable '%s': '%s' -> '%s'", versionVariableName, oldValue, newVersionValue));

        out.info(
            format(
                "%nThe following artifacts are impacted by this change:%n%s%n",
                mavenDependenciesCollection.getAllArtifactsUsingVersionVariable(versionVariableName)
                        .stream()
                        .map(MavenArtifact::toCoordinatesStringWithoutVersion)
                        .collect(joining(System.lineSeparator() + " - ", " - ", ""))));

        return true;
    }
}
