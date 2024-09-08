package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.DependencyResultWithTransferInfo.toSortedStreamOfMessages;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.ConsoleDependencyGraphDumper;
import com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.DependencyResultWithTransferInfo;
import com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelDependenciesCatalog;
import com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport;
import com.salesforce.tools.bazel.mavendependencies.resolver.StarlarkDependenciesResolver;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "dependency-info",
        description = "Prints information about a dependency.")
public class DependencyInfoCommand extends BaseDependencyCommand {

    public static void main(String[] args) {
        execute(new DependencyInfoCommand(), args);
    }

    @Option(
            names = "--no-vuln-check",
            description = "Skip the 3PP vulnerability check")
    protected boolean skipVulnerabilityCheck;

    private BazelDependenciesCatalog dependenciesCatalog;

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws Exception {
        // load catalog first so it can be searched
        dependenciesCatalog = new BazelDependenciesCatalog(workspaceRoot);
        dependenciesCatalog.load();

        final var mavenArtifact = getArtifactFromArguments(out);

        out.info("");
        final var collectionArtifact =
                mavenDependenciesCollection.findArtifact(mavenArtifact.toCoordinatesStringWithoutVersion());
        if (collectionArtifact != null) {
            out.info(
                format(
                    "The dependency is managed in the collection with version '%s'.",
                    collectionArtifact.getVersion()));

            final var variableValue =
                    mavenDependenciesCollection.getVersionVariableValue(collectionArtifact.getVersion());
            if (variableValue != null) {
                out.info(format("'%s' is set to '%s'", collectionArtifact.getVersion(), variableValue));
            }

            final var exclusions = collectionArtifact.getExclusions();
            if ((exclusions != null) && !exclusions.isEmpty()) {
                out.info("The following exclusions are defined:");
                exclusions.stream().map(e -> " - " + e.toString()).forEach(out::info);
            } else {
                out.info("No exclusions defined.");
            }
        } else {
            out.info("The dependency is NOT managed in the collection.");
        }

        if (verbose) {
            out.notice(format("Attempting to resolve dependency '%s'", mavenArtifact.toCoordinatesString()));
        }
        DependencyResultWithTransferInfo dependencyResolutionResult;
        try {
            final var dependenciesResolver =
                    new StarlarkDependenciesResolver(mavenDependenciesCollection, getRepoSys());
            dependencyResolutionResult = dependenciesResolver.resolveDependency(mavenArtifact, out);
            if (!dependencyResolutionResult.getTransferFailures().isEmpty()) {
                out.warning(
                    format(
                        "There were transfer failures:%n - %s",
                        toSortedStreamOfMessages(dependencyResolutionResult.getTransferFailures())
                                .collect(joining("\n - "))));
            }
        } catch (final Exception e) {
            out.warning(
                format(
                    "Error resolving '%s'. Dependency tree will not be available.",
                    mavenArtifact.toCoordinatesString()));
            out.warning(e.getMessage());
            if (verbose) {
                Throwable cause = e;
                while (cause != null) {
                    out.notice(cause.toString());
                    cause = cause.getCause();
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Resolution error for: {} ", mavenArtifact.toCoordinatesString(), e);
            }
            dependencyResolutionResult = null;
        }

        // lookup from catalog (prefer collected artifact but resolved artifact is ok,
        // too)
        var catalogEntry = collectionArtifact != null ? dependenciesCatalog
                .findImportByCoordinatesWithoutVersion(collectionArtifact.toCoordinatesStringWithoutVersion()) : null;
        if (catalogEntry == null) {
            if (dependencyResolutionResult != null) {
                if (verbose) {
                    out.notice(
                        format(
                            "Trying to locate pinned artifact from resolved '%s'",
                            dependencyResolutionResult.getDependencyResult().getRoot().getArtifact()));
                }
                catalogEntry = dependenciesCatalog.findImportByCoordinatesWithoutVersion(
                    ArtifactIdUtils
                            .toVersionlessId(dependencyResolutionResult.getDependencyResult().getRoot().getArtifact()));
            }
            if (catalogEntry == null) {
                // neither collected nor resolved, try import
                if (verbose) {
                    out.notice(
                        format(
                            "Trying to locate pinned artifact from specified '%s'",
                            mavenArtifact.toCoordinatesString()));
                }
                catalogEntry = dependenciesCatalog
                        .findImportByCoordinatesWithoutVersion(mavenArtifact.toCoordinatesStringWithoutVersion());
            }
        }
        if (catalogEntry != null) {
            out.info(
                format(
                    "The dependency is listed in the pinned catalog with target name '%s'.",
                    catalogEntry.getName()));
        } else {
            out.info("The dependency is NOT listed in the pinned catalog.");
        }

        out.info("");
        if (dependencyResolutionResult != null) {
            final var dump = new StringBuilder();
            renderTree(dependencyResolutionResult.getDependencyResult().getRoot(), dump);
            out.info(dump.toString());
            if (!skipVulnerabilityCheck) {
                getVulnerabilityScanner(out)
                        .printVulnerabilities(dependencyResolutionResult.getDependencyResult().getRoot());
            }
        } else {
            out.info("n/a");
        }

        out.info("");
        final var summary = new StringBuilder();
        renderSummary(collectionArtifact, catalogEntry, summary);
        out.info(summary.toString());

        return 0;
    }

    @Override
    protected MavenArtifact getArtifactFromArguments(MessagePrinter out) {
        try {
            return super.getArtifactFromArguments(out);
        } catch (IllegalArgumentException e) {
            // this is not in the collection ... it may (however) be in the catalog as transitive
            var coordinates = getCoordinatesFromParameterOrOption();
            if (coordinates != null) {
                var imports = dependenciesCatalog.findImportsByCoordinatesWithoutVersion(s -> s.contains(coordinates));
                if (!imports.isEmpty()) {
                    out.info("");
                    out.important(
                        format(
                            "Nothing found in the collection for coordinates '%s'.%nHowever, the pinned catalog has the following candidates:",
                            coordinates));
                    imports.stream().map(i -> " - " + i.getArtifact().toCoordinatesString()).forEach(out::info);
                    out.info("");

                    var artifact = imports.get(0).getArtifact();
                    out.important(format("Continuing with: %s", artifact.toCoordinatesString()));
                    return artifact;
                }
            }

            // give up
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private void renderSummary(
            MavenArtifact collectionArtifact,
            BazelJavaDependencyImport catalogEntry,
            StringBuilder summary) {
        if (collectionArtifact != null) {
            summary.append("Collection: ")
                    .append(
                        mavenDependenciesCollection
                                .getGroupFileLocation(mavenDependenciesCollection.getGroup(collectionArtifact)))
                    .append(System.lineSeparator());
            summary.append("            ").append(collectionArtifact.toCoordinatesString());
            final var version = mavenDependenciesCollection.getVersionVariableValue(collectionArtifact.getVersion());
            if (version != null) {
                summary.append(" (").append(collectionArtifact.getVersion()).append(" = ").append(version).append(") ");
            }
            summary.append(System.lineSeparator());

        } else {
            summary.append("Collection: n/a").append(System.lineSeparator());
        }

        if (catalogEntry != null) {
            summary.append("   Catalog: ")
                    .append(dependenciesCatalog.getGroupFileLocation(dependenciesCatalog.getGroup(catalogEntry)))
                    .append(System.lineSeparator());
            summary.append("            ")
                    .append(catalogEntry.getArtifact().toCoordinatesString())
                    .append(System.lineSeparator());
            summary.append("            ").append(catalogEntry.getName()).append(System.lineSeparator());
        } else {
            summary.append("   Catalog: n/a").append(System.lineSeparator());
        }
    }

    private void renderTree(DependencyNode node, StringBuilder sb) {
        final var os = new ByteArrayOutputStream(1024);
        node.accept(new ConsoleDependencyGraphDumper(new PrintStream(os)));
        sb.append(os.toString());
    }
}