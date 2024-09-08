package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.DependencyResultWithTransferInfo.toSortedStreamOfMessages;
import static com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport.TAG_NOT_IN_COLLECTION;
import static com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport.createForArtifact;
import static java.lang.String.format;
import static java.nio.file.Files.writeString;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph.Builder;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.cli.helper.ProgressMonitor;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionDelta;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionDelta.Modification;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionDelta.Modification.Type;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionSaveResult;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.maven.MavenShaComputer;
import com.salesforce.tools.bazel.mavendependencies.maven.MavenShaComputer.Algorithm;
import com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.ConsoleDependencyGraphDumper;
import com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.MavenDepsNodeListGenerator;
import com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelDependenciesCatalog;
import com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport;
import com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport.BazelJavaDependencyImportBuilder;
import com.salesforce.tools.bazel.mavendependencies.resolver.StarlarkDependenciesResolver;
import com.salesforce.tools.bazel.mavendependencies.resolver.StarlarkDependenciesResolver.ResolveAndDownloadResult;
import com.salesforce.tools.bazel.mavendependencies.visibility.ReverseDependenciesProvider;
import com.salesforce.tools.bazel.mavendependencies.visibility.StrictDepsVisibilityProvider;
import com.salesforce.tools.bazel.mavendependencies.visibility.VisibilityProvider;
import com.salesforce.tools.bazel.mavendependencies.visibility.VisibilityProviderFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
        name = "pin-dependencies",
        description = "Resolve all dependencies from the collection and generate the pinned catalog.",
        scope = ScopeType.INHERIT)
public class PinDependenciesCommand extends BaseCommandUsingDependencyCollection {

    private static DependencyFilter SKIP_REPLACED_NODES_FILTER = MavenDepsNodeListGenerator.skipReplacedNodes();

    static BazelJavaDependencyImportBuilder createNewFromExistingAndReplacingArtifact(
            BazelJavaDependencyImport existingImport,
            MavenArtifact mavenArtifact,
            boolean keepShaValues) {
        // create a new BazelDependenciesCatalogEntryBuilder, copying most data but
        // replace the artifact (because there might be a new version!)
        final var entryBuilder =
                BazelJavaDependencyImport.createWithNameAndArtifact(existingImport.getName(), mavenArtifact)
                        .setSourcesArtifact(existingImport.hasSourcesArtifact())
                        .setDeps(existingImport.getDeps())
                        .setRuntimeDeps(existingImport.getRuntimeDeps())
                        .setExports(existingImport.getExports())
                        .setLicenses(existingImport.getLicenses())
                        .setNeverlink(existingImport.isNeverlink())
                        .setTestonly(existingImport.isTestonly())
                        .setDefaultVisibility(existingImport.getDefaultVisibility())
                        .setTags(existingImport.getTags())
                        .setExtraBuildFileContent(existingImport.getExtraBuildFileContent());
        if (keepShaValues) {
            // when the version did not change we need to keep the hashes
            entryBuilder.setArtifactSha1(existingImport.getArtifactSha1())
                    .setArtifactSha256(existingImport.getArtifactSha256())
                    .setSourcesArtifactSha1(existingImport.getSourcesArtifactSha1())
                    .setSourcesArtifactSha256(existingImport.getSourcesArtifactSha256());
        }
        return entryBuilder;
    }

    public static void main(String[] args) {
        execute(new PinDependenciesCommand(), args);
    }

    private static String nullifyEmptyString(String value) {
        if ((value != null) && !value.isEmpty()) {
            return value;
        }
        return null;
    }

    private static CompletableFuture<Void> runAllAsyncAndReportProgress(
            final List<Runnable> work,
            final ExecutorService executorService,
            ProgressMonitor monitor) {
        return CompletableFuture.allOf(
            work.parallelStream()
                    .map(task -> CompletableFuture.runAsync(task, executorService).thenRun(() -> monitor.progressBy(1)))
                    .collect(toList())
                    .toArray(new CompletableFuture[work.size()]));
    }

    @Option(
            names = "--force",
            description = "re-pins and saves the catalog even if no changes were detected",
            negatable = true)
    private boolean force;

    @Option(
            names = "--unsecure-overwrite-checksums-if-mismatch",
            description = "Update hashes if mismatch detected. Use with care!",
            negatable = true,
            hidden = true)
    private boolean overwriteChecksumsOnMismatch;

    @Option(
            names = "--print-missing-sources",
            description = "Print information about missing sources",
            negatable = true)
    private boolean printMissingSources;

    @Option(
            names = "--dry-run",
            description = "does not save the catalog at the end")
    private boolean dryRun;

    @Option(
            names = "--no-vuln-check",
            description = "Skip the 3PP vulnerability check",
            scope = ScopeType.INHERIT)
    protected boolean noVulnCheck;

    @Option(
            names = { "--override-maven-server" },
            description = "one or more URLs to be put into the maven_server variables as default values (if nothing is specified the Maven Central url will be used)",
            scope = ScopeType.INHERIT)
    protected SortedSet<String> overrideMavenServers;

    @Option(
            names = "--label-for-loading-jvm_maven_import_external-symbol",
            description = "label for loading the jvm_maven_import_external symbol (defaults to '@bazel_maven_deps//bazel:jvm.bzl')",
            defaultValue = "@bazel_maven_deps//bazel:jvm.bzl",
            scope = ScopeType.INHERIT)
    protected String labelForLoadingJvmMavenExternalSymbol;

    @Option(
            names = "--catalog-preamble",
            description = "preamble text to inject into the pinned catalog .bzl files after the load statement (of none is provided a recommended default will be generated)",
            required = false,
            scope = ScopeType.INHERIT)
    protected String catalogPreamble;

    @Option(
            names = "--fail-on-changes",
            description = "fail command if changes are detected between on-disk catalog and resolved model")
    private boolean failOnChanges;

    @Option(
            names = "--strict-deps",
            description = "set visibility of transient dependency to \"//visibility:private\" (will be ignored if a custom visibility extension is used)",
            negatable = true)
    private boolean strictDeps;
    private final MavenShaComputer shaComputer = new MavenShaComputer();
    private StarlarkDependenciesResolver starlarkDependenciesResolver;

    private ResolveAndDownloadResult resolveAndDownloadResult;

    private BazelDependenciesCatalog catalog;

    private VisibilityProvider visibilityProvider;

    private void appendToMessageAndDelta(
            final StringBuilder message,
            final SortedSet<String> delta,
            final SortedSet<String> coordinatesNotInSingleRootList) {
        for (final String coordinates : coordinatesNotInSingleRootList) {
            message.append("  - ").append(coordinates).append(System.lineSeparator());
            delta.add(coordinates);
        }
    }

    private Runnable checkArtifactSha1(
            BazelJavaDependencyImportBuilder catalogEntryBuilder,
            ConcurrentMap<String, String> checksumMismatchesByCoordinates) {
        return () -> {
            final var sha1 = computeSha1(catalogEntryBuilder.getArtifact());
            // checksum are compared case insensitive
            // see https://github.com/apache/maven-resolver/blob/1091f3ae9301a07d85073b08bbffda88ab2c0574/maven-resolver-connector-basic/src/main/java/org/eclipse/aether/connector/basic/ChecksumValidator.java#L192
            if (!sha1.equalsIgnoreCase(catalogEntryBuilder.getArtifactSha1())) {
                checksumMismatchesByCoordinates.put(
                    catalogEntryBuilder.getArtifact().toCoordinatesStringWithoutVersion() + " SHA1",
                    String.format(
                        "expected '%s' <> got '%s'%s",
                        catalogEntryBuilder.getArtifactSha1(),
                        sha1,
                        overwriteChecksumsOnMismatch ? "->(repaired)" : ""));
                if (overwriteChecksumsOnMismatch) {
                    catalogEntryBuilder.setArtifactSha1(sha1);
                }
            }
        };
    }

    private Runnable checkArtifactSha256(
            BazelJavaDependencyImportBuilder catalogEntryBuilder,
            ConcurrentMap<String, String> checksumMismatchesByCoordinates) {
        return () -> {
            final var sha256 = computeSha256(catalogEntryBuilder.getArtifact());
            // checksum are compared case insensitive
            // see https://github.com/apache/maven-resolver/blob/1091f3ae9301a07d85073b08bbffda88ab2c0574/maven-resolver-connector-basic/src/main/java/org/eclipse/aether/connector/basic/ChecksumValidator.java#L192
            if (!sha256.equalsIgnoreCase(catalogEntryBuilder.getArtifactSha256())) {
                checksumMismatchesByCoordinates.put(
                    catalogEntryBuilder.getArtifact().toCoordinatesStringWithoutVersion() + " SHA256",
                    String.format(
                        "expected '%s' <> got '%s'%s",
                        catalogEntryBuilder.getArtifactSha256(),
                        sha256,
                        overwriteChecksumsOnMismatch ? "->(repaired)" : ""));
                if (overwriteChecksumsOnMismatch) {
                    catalogEntryBuilder.setArtifactSha256(sha256);
                }
            }
        };
    }

    private String checkDependencyTrees(
            ResolveAndDownloadResult resolveAndDownloadResult,
            MessagePrinter out) throws IOException {
        /*
         * We essentially compare two lists:
         *
         *    1. the first list is built from all individual nodes in the dependency collection
         *
         *    2. the second list is built from a single root node of the resolution result
         *
         * There is a common expectation that both list must be identical.
         * If any list contains less or more items it means we have an unbalanced graph.
         * An unbalanced graph is most likely caused by version conflict.
         * Thus, we have multiple versions of a single artifact, which we do not want under any circumstances!
         */
        final var multipleNodesList = collectAllArtifactIdCoordinates(
            resolveAndDownloadResult.getDependencyNodeByIndividuallyResolvedCoordinates().values());
        final var singleRootList =
                collectAllArtifactIdCoordinates(List.of(resolveAndDownloadResult.getResolveResult().getRoot()));

        final var listsArOk = multipleNodesList.equals(singleRootList);

        if (!listsArOk || verbose) {
            final var singleRootListFile =
                    mavenDependenciesCollection.getDirectory().resolve("_resolved-dependencies_merged.list");
            writeString(
                singleRootListFile,
                singleRootList.stream().collect(joining("\n")),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            out.info(format("Wrote dependency list to '%s'", singleRootListFile));

            final var multipleNodesListFile =
                    mavenDependenciesCollection.getDirectory().resolve("_resolved-dependencies_modules.list");
            writeString(
                multipleNodesListFile,
                multipleNodesList.stream().collect(joining("\n")),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            out.info(format("Wrote dependency list to '%s'", multipleNodesListFile));
        }

        if (listsArOk) {
            // content matches, all good
            return null;
        }

        final var message = new StringBuilder();
        message.append("Dependency resolution resulted in an unbalanced graph.").append(System.lineSeparator());
        message.append("This indicates an issue with missing dependency exclusions.").append(System.lineSeparator());
        message.append(System.lineSeparator());
        message.append("Please inspect the generated '_resolved-dependencies_*.list/tree' files in '")
                .append(mavenDependenciesCollection.getDirectory())
                .append("'.")
                .append(System.lineSeparator());
        message.append(System.lineSeparator());
        message.append("-> Use a diff tool to identify superfluous dependencies.").append(System.lineSeparator());
        message.append("-> Exclude/remove unwanted/unused dependencies.").append(System.lineSeparator());
        message.append("-> Use exclusions to remove unwanted dependencies.").append(System.lineSeparator());
        message.append("-> Declare additional dependencies in the collection")
                .append(System.lineSeparator())
                .append("   in order to enforce single versions.")
                .append(System.lineSeparator());
        message.append(System.lineSeparator());

        // collect & print delta
        final SortedSet<String> delta = new TreeSet<>();
        final SortedSet<String> coordinatesNotInSingleRootList = multipleNodesList.stream()
                .filter(not(singleRootList::contains))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!coordinatesNotInSingleRootList.isEmpty()) {
            message.append("Problematic dependencies not found in expected list:").append(System.lineSeparator());
            appendToMessageAndDelta(message, delta, coordinatesNotInSingleRootList);
        }
        final SortedSet<String> coordinatesNotInMultilpleNodesList = singleRootList.stream()
                .filter(not(multipleNodesList::contains))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!coordinatesNotInMultilpleNodesList.isEmpty()) {
            message.append("Problematic dependencies not found in detailed list:").append(System.lineSeparator());
            appendToMessageAndDelta(message, delta, coordinatesNotInMultilpleNodesList);
        }
        message.append(System.lineSeparator());

        // collect & print paths
        var pathFinder = new PathRecordingDependencyVisitor((node, parents) -> {
            var versionlessId = ArtifactIdUtils.toVersionlessId(node.getArtifact());
            return delta.parallelStream().anyMatch(s -> s.startsWith(versionlessId));
        });
        for (final DependencyNode node : resolveAndDownloadResult.getDependencyNodeByIndividuallyResolvedCoordinates()
                .values()) {
            node.accept(pathFinder);
        }
        var problematicPaths = pathFinder.getPaths();
        if (problematicPaths.isEmpty()) {
            message.append("No problematic paths found. This may indicate a resolution issue in Maven repository.")
                    .append(System.lineSeparator());
            message.append("The detailed result is expected to have the same or more resolved dependencies")
                    .append(System.lineSeparator());
            message.append("but not less. However, it did contain less. See above for the missing dependencies.")
                    .append(System.lineSeparator());
        } else {
            message.append("Problematic paths:").append(System.lineSeparator());
            for (final List<DependencyNode> paths : problematicPaths) {
                message.append("  - ")
                        .append(
                            paths.stream()
                                    .map(DependencyNode::getArtifact)
                                    .map(ArtifactIdUtils::toId)
                                    .collect(joining(" > ")))
                        .append(System.lineSeparator());
            }
        }
        message.append(System.lineSeparator());

        if (!resolveAndDownloadResult.getTransferFailures().isEmpty()) {
            message.append("The following transfer failures occured:");
            message.append(
                toSortedStreamOfMessages(resolveAndDownloadResult.getTransferFailures())
                        .collect(joining("\n - ", "\n - ", "\n")));
            message.append(System.lineSeparator());
        }

        return message.toString();
    }

    private Runnable checkSourcesArtifactSha1(
            BazelJavaDependencyImportBuilder catalogEntryBuilder,
            ConcurrentMap<String, String> checksumMismatchesByCoordinates) {
        return () -> {
            final var sha1 = computeSha1(catalogEntryBuilder.getSourcesArtifact());
            if (!sha1.equalsIgnoreCase(catalogEntryBuilder.getSourcesArtifactSha1())) {
                checksumMismatchesByCoordinates.put(
                    catalogEntryBuilder.getSourcesArtifact().toCoordinatesStringWithoutVersion() + " SHA1",
                    String.format(
                        "expected '%s' <> got '%s'%s",
                        catalogEntryBuilder.getSourcesArtifactSha1(),
                        sha1,
                        overwriteChecksumsOnMismatch ? "->(repaired)" : ""));
                if (overwriteChecksumsOnMismatch) {
                    catalogEntryBuilder.setSourcesArtifactSha1(sha1);
                }

            }
        };
    }

    private Runnable checkSourcesArtifactSha256(
            BazelJavaDependencyImportBuilder catalogEntryBuilder,
            ConcurrentMap<String, String> checksumMismatchesByCoordinates) {
        return () -> {
            final var sha256 = computeSha256(catalogEntryBuilder.getSourcesArtifact());
            if (!sha256.equalsIgnoreCase(catalogEntryBuilder.getSourcesArtifactSha256())) {
                checksumMismatchesByCoordinates.put(
                    catalogEntryBuilder.getSourcesArtifact().toCoordinatesStringWithoutVersion() + " SHA256",
                    String.format(
                        "expected '%s' <> got '%s'%s",
                        catalogEntryBuilder.getSourcesArtifactSha256(),
                        sha256,
                        overwriteChecksumsOnMismatch ? "->(repaired)" : ""));
                if (overwriteChecksumsOnMismatch) {
                    catalogEntryBuilder.setSourcesArtifactSha256(sha256);
                }
            }
        };
    }

    /**
     * Visit the dependency tree of each node to collect its resolved artifacts into a flat list
     *
     * @param nodes
     *            the list of nodes to visit
     * @return a sorted set of {@link ArtifactIdUtils#toId(Artifact) artifact ids}.
     */
    private SortedSet<String> collectAllArtifactIdCoordinates(Collection<DependencyNode> nodes) {
        var generator = new MavenDepsNodeListGenerator();
        for (final DependencyNode node : nodes) {
            node.accept(generator);
        }
        return generator.getArtifacts(false, MavenDepsNodeListGenerator.skipReplacedNodes())
                .stream()
                .map(ArtifactIdUtils::toId)
                .collect(toCollection(TreeSet::new));
    }

    private Runnable computeBazelVisibility(
            BazelJavaDependencyImportBuilder javaDependencyImport,
            ReverseDependenciesProvider rdepsProvider,
            VisibilityProvider visibilityProvider) {
        return () -> {
            // compute visibility
            javaDependencyImport.setDefaultVisibility(
                visibilityProvider.getVisibility(
                    javaDependencyImport.getName(),
                    javaDependencyImport.getArtifact(),
                    Collections.unmodifiableSortedSet(javaDependencyImport.getTags()),
                    rdepsProvider));
        };
    }

    private String computeSha1(MavenArtifact artifact) {
        final var download = resolveAndDownloadResult.getDownloadedArtifact(artifact.toCoordinatesString());
        if (download != null) {
            try {
                return shaComputer.getSha(download, Algorithm.SHA1);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    private String computeSha256(MavenArtifact artifact) {
        final var download = resolveAndDownloadResult.getDownloadedArtifact(artifact.toCoordinatesString());
        if (download != null) {
            try {
                return shaComputer.getSha(download, Algorithm.SHA256);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    @Override
    protected int doExecuteCommand(MessagePrinter out) throws Exception {
        // load existing catalog
        catalog = new BazelDependenciesCatalog(workspaceRoot);
        catalog.load();

        starlarkDependenciesResolver = new StarlarkDependenciesResolver(mavenDependenciesCollection, getRepoSys());

        // resolve and download all missing artifacts
        resolveAndDownloadResult = starlarkDependenciesResolver.resolveAndDownload(out);

        // dump a list of all remote repos
        if (verbose) {
            final var artifactUrlsFile = mavenDependenciesCollection.getDirectory().resolve("_artifact.urls");
            final var artifcatUrlsContent = resolveAndDownloadResult.getResolveResult()
                    .getArtifactResults()
                    .parallelStream()
                    .map(r -> format("%s -> %s", ArtifactIdUtils.toId(r.getArtifact()), r.getRepository()))
                    .sorted()
                    .collect(joining(System.lineSeparator()));
            writeString(
                artifactUrlsFile,
                artifcatUrlsContent,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            out.info(format("Wrote urls to '%s'", artifactUrlsFile));
        }

        // sanity check: the combined tree's content must match the single resolve trees
        // content
        final var checkTreesError = checkDependencyTrees(resolveAndDownloadResult, out);

        // dump tree
        if (verbose || (checkTreesError != null)) {
            final var singleRootDump = new StringBuilder();
            renderTree(resolveAndDownloadResult.getResolveResult().getRoot(), singleRootDump);
            final var singleRootDumpFile =
                    mavenDependenciesCollection.getDirectory().resolve("_resolved-dependencies_merged.tree");
            writeString(
                singleRootDumpFile,
                singleRootDump,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            out.info(format("Wrote dependency tree to '%s'", singleRootDumpFile));

            final var multipleNodesDump = new StringBuilder();
            final List<String> sortedCoordinates =
                    resolveAndDownloadResult.getDependencyNodeByIndividuallyResolvedCoordinates()
                            .keySet()
                            .stream()
                            .sorted()
                            .collect(toList());
            for (final String coordinates : sortedCoordinates) {
                renderTree(
                    resolveAndDownloadResult.getDependencyNodeByIndividuallyResolvedCoordinates().get(coordinates),
                    multipleNodesDump);
            }
            final var multipleNodesDumpFile =
                    mavenDependenciesCollection.getDirectory().resolve("_resolved-dependencies_modules.tree");
            writeString(
                multipleNodesDumpFile,
                multipleNodesDump,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            out.info(format("Wrote dependency tree to '%s'", multipleNodesDumpFile));
        }

        // abort in case of unbalanced trees
        if (checkTreesError != null) {
            throw new IllegalStateException(checkTreesError);
        }

        // print transfer errors (they indicate incomplete resolution but we tolerate them when the trees are ok)
        if (verbose && !resolveAndDownloadResult.getTransferFailures().isEmpty()) {
            out.info("");
            out.warning(
                format(
                    "The following transfer failures occured. This may lead to an incomplete resolution result.%n%s",
                    toSortedStreamOfMessages(resolveAndDownloadResult.getTransferFailures())
                            .collect(joining("\n - ", " - ", "\n"))));
        }

        // collect paths of test-only dependencies so we can flag *new* transitives
        var testOnlyCoordinatesWithoutVersion = mavenDependenciesCollection.getAllArtifacts()
                .filter(MavenArtifact::isTestonly)
                .map(MavenArtifact::toCoordinatesStringWithoutVersion)
                .collect(toSet());
        // collect paths of neverlink dependencies so we can flag *new* transitives
        var neverLinkCoordinatesWithoutVersion = mavenDependenciesCollection.getAllArtifacts()
                .filter(MavenArtifact::isNeverlink)
                .map(MavenArtifact::toCoordinatesStringWithoutVersion)
                .collect(toSet());

        // collect all managed dependencies so we can properly tag transitive
        var managedDependenciesVersionlessIds = resolveAndDownloadResult.getManagedDependencies()
                .stream()
                .map(Dependency::getArtifact)
                .map(ArtifactIdUtils::toVersionlessId)
                .collect(toSet());

        // build a map of all new entries
        final ConcurrentMap<String, BazelJavaDependencyImportBuilder> newCatalogEntriesByVersionlessCoordinates =
                new ConcurrentHashMap<>();
        var existingEntries = 0;

        // find existing entries for all dependencies;
        for (final Artifact artifact : resolveAndDownloadResult.getResolvedArtifacts()) {
            // use coordinates to locate an existing artifact: we want to preserve and
            // custom name modifications in the pinned catalog
            final var versionLessCoordinates = ArtifactIdUtils.toVersionlessId(artifact);
            final var existingImport = catalog.findImportByCoordinatesWithoutVersion(versionLessCoordinates);
            if (existingImport != null) {
                // replace it with a new object so that the change detection logic in
                // replaceContent triggers
                // BUT be careful: we do want to keep the content hashes when this is NOT a
                // version change
                final var keepShaValues =
                        Objects.equals(artifact.getVersion(), existingImport.getArtifact().getVersion());
                newCatalogEntriesByVersionlessCoordinates.put(
                    versionLessCoordinates,
                    createNewFromExistingAndReplacingArtifact(existingImport, toArtifact(artifact), keepShaValues));
                existingEntries++;
            } else {
                newCatalogEntriesByVersionlessCoordinates
                        .put(versionLessCoordinates, createForArtifact(toArtifact(artifact)));
            }
        }

        // find entries, which should be removed (again, use coordinates to locate)
        final List<BazelJavaDependencyImport> obsoleteImports = catalog.getAllImports()
                .parallel()
                .filter(
                    i -> !newCatalogEntriesByVersionlessCoordinates
                            .containsKey(i.getArtifact().toCoordinatesStringWithoutVersion()))
                .collect(toList());

        out.info(
            format(
                "Found %d existing, %d new and %d obsolete entries",
                existingEntries,
                newCatalogEntriesByVersionlessCoordinates.size() - existingEntries,
                obsoleteImports.size()));

        // collect checksum mismatches for printing at the end
        final ConcurrentMap<String, String> checksumMismatchesByCoordinates = new ConcurrentHashMap<>();

        // collect not successful source downloads for printing at the end
        final ConcurrentMap<String, String> missingSourceDownloadsByCoordinates = new ConcurrentHashMap<>();

        // populate all entries with dependency information, shas and sources
        try (var monitor = out.progressMonitor("Updating and verifying checksums")) {
            monitor.maxHint(-1);
            final List<Runnable> workForDependencyResolution = new CopyOnWriteArrayList<>();
            final List<Runnable> work = new CopyOnWriteArrayList<>();
            newCatalogEntriesByVersionlessCoordinates.entrySet().parallelStream().forEach(entry -> {
                final var catalogEntryBuilder = entry.getValue();

                // SHAs
                if (catalogEntryBuilder.getArtifactSha1() == null) {
                    work.add(updateArtifactSha1(catalogEntryBuilder));
                } else {
                    work.add(checkArtifactSha1(catalogEntryBuilder, checksumMismatchesByCoordinates));
                }
                if (catalogEntryBuilder.getArtifactSha256() == null) {
                    work.add(updateArtifactSha256(catalogEntryBuilder));
                } else {
                    work.add(checkArtifactSha256(catalogEntryBuilder, checksumMismatchesByCoordinates));
                }

                // sources
                final var potentialSourcesArtifact = catalogEntryBuilder.getArtifact().toSourcesArtifact();
                if (potentialSourcesArtifact != null) {
                    final var hasSource = resolveAndDownloadResult
                            .hasSuccessfulSourcesDownload(potentialSourcesArtifact.toCoordinatesString());
                    catalogEntryBuilder.setSourcesArtifact(hasSource);
                    if (hasSource) {
                        if (catalogEntryBuilder.getSourcesArtifactSha1() == null) {
                            work.add(updateSourcesArtifactSha1(catalogEntryBuilder));
                        } else {
                            work.add(checkSourcesArtifactSha1(catalogEntryBuilder, checksumMismatchesByCoordinates));
                        }
                        if (catalogEntryBuilder.getSourcesArtifactSha256() == null) {
                            work.add(updateSourcesArtifactSha256(catalogEntryBuilder));
                        } else {
                            work.add(checkSourcesArtifactSha256(catalogEntryBuilder, checksumMismatchesByCoordinates));
                        }
                    } else {
                        missingSourceDownloadsByCoordinates.put(
                            potentialSourcesArtifact.toCoordinatesStringWithoutVersion(),
                            String.format("no sources available, please check Maven repository or disable sources"));
                    }

                } else if (catalogEntryBuilder.hasSourcesArtifact()) {
                    // artifact does not have sources; disable source artifact downloads
                    // but do not reset any SHAs' we'll keep them in there
                    catalogEntryBuilder.setSourcesArtifact(false);
                }

                // dependency graph is tracked separately because visibility stages needs it for completion
                workForDependencyResolution
                        .add(updateDependencyGraph(catalogEntryBuilder, newCatalogEntriesByVersionlessCoordinates));

                // Bazel customization
                work.add(
                    updateBazelInfo(
                        catalogEntryBuilder,
                        testOnlyCoordinatesWithoutVersion,
                        neverLinkCoordinatesWithoutVersion,
                        managedDependenciesVersionlessIds));
            });
            final var calculations =
                    Executors.newWorkStealingPool(Math.max(8, Runtime.getRuntime().availableProcessors()));
            monitor.maxHint(
                workForDependencyResolution.size() + work.size()
                        + newCatalogEntriesByVersionlessCoordinates.size() /* to be created visibility updates */);

            // start dependency resolution first
            var dependencyResolutionFuture =
                    runAllAsyncAndReportProgress(workForDependencyResolution, calculations, monitor);

            // all other updates
            var otherWorkFuture = runAllAsyncAndReportProgress(work, calculations, monitor);

            // visibility updates based on dependency graph
            Graph<String> dependencyGraph = dependencyResolutionFuture.thenApplyAsync(v -> {
                Builder<String> builder = GraphBuilder.directed().immutable();
                for (var catalogEntryBuilder : newCatalogEntriesByVersionlessCoordinates.values()) {
                    var name = catalogEntryBuilder.getName();
                    builder.addNode(name);
                    for (var dep : catalogEntryBuilder.getDeps()) {
                        builder.putEdge(name, dep.substring(1) /* remove leading '@' */);
                    }
                    for (var dep : catalogEntryBuilder.getRuntimeDeps()) {
                        builder.putEdge(name, dep.substring(1) /* remove leading '@' */);
                    }
                }
                return builder.build();
            }, calculations).join();
            var visibilityWorkFuture = runAllAsyncAndReportProgress(
                newCatalogEntriesByVersionlessCoordinates.values()
                        .parallelStream()
                        .map(
                            catalogEntryBuilder -> computeBazelVisibility(
                                catalogEntryBuilder,
                                dependencyGraph::predecessors,
                                getVisibilityProvider(out)))
                        .toList(),
                calculations,
                monitor);

            // wait for all work to be completed
            calculations.shutdown();
            CompletableFuture.allOf(otherWorkFuture, visibilityWorkFuture).get(10, TimeUnit.MINUTES);
        }

        if (!checksumMismatchesByCoordinates.isEmpty()) {
            final var artifacts = checksumMismatchesByCoordinates.entrySet()
                    .stream()
                    .map(e -> format("%s: %s", e.getKey(), e.getValue()))
                    .sorted()
                    .collect(joining("\n - ", " - ", "\n"));
            out.info("");
            if (!overwriteChecksumsOnMismatch) {
                throw new IllegalStateException(
                        format(
                            "Discovered artifacts with checksum mis-matches. This should not happen.%nPlease confirm that download was not corrupted or the version in the Maven repository was not hijacked/replaced/redeployed.%n%nArtifacts with issues:%n%s%n",
                            artifacts));
            }
            out.warning(format("Discovered and repaired artifacts with checksum mis-matches:%n%s%n", artifacts));
        }

        if (printMissingSources && !missingSourceDownloadsByCoordinates.isEmpty()) {
            final var artifacts = missingSourceDownloadsByCoordinates.entrySet()
                    .stream()
                    .map(e -> format("%s: %s", e.getKey(), e.getValue()))
                    .sorted()
                    .collect(joining("\n - ", " - ", "\n"));
            out.info("");
            out.warning(
                format(
                    "Discovered artifacts with missing sources. This indicates a deployment problem of the artifact.%nPlease confirm that sources exists and can be downloaded.%n%nArtifacts with issues:%n%s%n",
                    artifacts));
        }

        if (verbose) {
            final var fullDependencyList =
                    mavenDependenciesCollection.getDirectory().resolve("_full-dependencies-list");
            writeString(
                fullDependencyList,
                newCatalogEntriesByVersionlessCoordinates.values()
                        .stream()
                        .map(a -> a.getArtifact().toCoordinatesString())
                        .distinct()
                        .sorted()
                        .collect(joining("\n")),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            out.notice(format("Wrote '%s'", fullDependencyList));
        }

        final var delta = catalog.replaceContent(
            newCatalogEntriesByVersionlessCoordinates.values().stream().map(BazelJavaDependencyImportBuilder::build),
            verbose);

        if (delta.modifications.isEmpty()) {
            out.important("No changes detected to calatog.");
        } else if (!noVulnCheck) {
            final var shouldAbort =
                    getVulnerabilityScanner(out).shouldAbortForModifications(delta.modifications, isInteractive());
            if (shouldAbort) {
                out.info("Skip saving catalog because user chose to cancel.");
                return 0;
            }
        }

        CollectionSaveResult saveResult = null;
        if (dryRun) {
            out.info("Skip saving catalog because '--dry-run' was specified.");
        } else if ((force || !delta.modifications.isEmpty())) {
            try (var monitor = out.progressMonitor("Saving catalog")) {
                final SortedSet<String> mavenServers = new TreeSet<>();
                if (overrideMavenServers != null) {
                    // use explicitly declared servers
                    mavenServers.addAll(overrideMavenServers);
                }
                if (mavenServers.isEmpty()) {
                    // fallback (default) to Maven Central and all its mirrors
                    mavenServers.add(requireNonNull(mavenCenteralUrl, "Please provide Maven Central URL!"));
                    if (mavenRepositories != null) {
                        mavenServers.addAll(mavenRepositories);
                    }
                }

                saveResult = catalog.save(
                    mavenServers,
                    labelForLoadingJvmMavenExternalSymbol,
                    catalogPreamble != null ? catalogPreamble : BazelDependenciesCatalog.getRecommendedPreamble(),
                    monitor,
                    getScmTool());
            }
        }

        out.important("Done");

        if (!delta.modifications.isEmpty()) {
            out.info(
                format(
                    "%nThe following changes were detected:%n%s%n",
                    delta.modifications.stream()
                            .map(Modification::toString)
                            .collect(joining(System.lineSeparator() + " ", " ", ""))));

            final var warnings = evaluateRisk(delta);
            if (!warnings.isEmpty()) {
                out.warning(
                    format(
                        "Please double check the changes to the catalog. Here are some yellow flags:%n%s%n",
                        warnings.stream().collect(joining(System.lineSeparator() + " - ", " - ", ""))));
            }
        }

        printSaveResult(out, saveResult, delta.obsoleteGroups.stream().map(catalog::getGroupFileLocation));

        if (!delta.modifications.isEmpty() && failOnChanges) {
            throw new IllegalStateException(
                    "Detected unexpected changes to the pinned catalog. Did you forget to pin dependencies?");
        }
        return 0;
    }

    private List<String> evaluateRisk(CollectionDelta delta) {
        final List<String> warnings = new ArrayList<>();
        if (delta.obsoleteGroups.size() > 1) {
            warnings.add("The number of obsolete groups is more than one. That's a lot removals!");
        }

        if (delta.modifications.size() > 5) {
            final var first = delta.modifications.first();
            final var allOfTheSameType = delta.modifications.stream()
                    .allMatch(m -> m.getTypeOfModification() == first.getTypeOfModification());
            final var firstIsVersionUpdate = first.getTypeOfModification() == Type.VERSION_UPDATE;
            final var allOfSameVersionUpdate = allOfTheSameType && firstIsVersionUpdate && delta.modifications.stream()
                    .allMatch(m -> m.getArtifactVersion().equals(first.getArtifactVersion()));
            if (allOfSameVersionUpdate) {
                // nothing to worry about(eg. Spring, Aura, Jackson, etc.)
            } else {
                if (allOfTheSameType && firstIsVersionUpdate) {
                    warnings.add("There are multiple unequal version updates.");
                } else {
                    warnings.add("The number of changes exceeds five and those changes are disjoint.");
                }
                warnings.add("Is your dependency collection up-to-date (sync'd to latest)?");
            }
        }

        return warnings;
    }

    private DependencyNode getResolvedDependenyNode(BazelJavaDependencyImportBuilder javaDependencyImport) {
        return requireNonNull(
            resolveAndDownloadResult
                    .getDependencyNodeFromMergedIndex(javaDependencyImport.getArtifact().toCoordinatesString()),
            () -> "missing resolution result: " + javaDependencyImport.getArtifact().toCoordinatesString());
    }

    protected VisibilityProvider getVisibilityProvider(MessagePrinter out) {
        if (visibilityProvider != null) {
            return visibilityProvider;
        }

        // don't create more then one, loading them could be expensive
        synchronized (this) {
            if (visibilityProvider != null) {
                return visibilityProvider;
            }

            final var factory = VisibilityProviderFactory.findSingle();
            if (factory.isPresent()) {
                try {
                    return visibilityProvider =
                            Objects.requireNonNull(factory.get().create(out, spec, verbose, workspaceRoot));
                } catch (RuntimeException | AssertionError | LinkageError e) {
                    // this is severe so we will abort
                    throw new IllegalStateException(
                            format(
                                "An error occured initializing the visibility provider extension '%s'. %s",
                                factory.get().getClass().getName(),
                                e.getMessage()),
                            e);
                }
            }
            return visibilityProvider = new StrictDepsVisibilityProvider(out, strictDeps);
        }
    }

    private void renderTree(DependencyNode node, StringBuilder sb) {
        final var os = new ByteArrayOutputStream(1024);
        node.accept(new ConsoleDependencyGraphDumper(new PrintStream(os)));
        sb.append(os.toString());
    }

    private MavenArtifact toArtifact(Artifact a) {
        return new MavenArtifact(
                a.getGroupId(),
                a.getArtifactId(),
                a.getVersion(),
                a.getExtension(),
                nullifyEmptyString(a.getClassifier()),
                null,
                false,
                false);
    }

    private Runnable updateArtifactSha1(BazelJavaDependencyImportBuilder javaDependencyImport) {
        return () -> {
            javaDependencyImport.setArtifactSha1(computeSha1(javaDependencyImport.getArtifact()));
        };
    }

    private Runnable updateArtifactSha256(BazelJavaDependencyImportBuilder javaDependencyImport) {
        return () -> {
            javaDependencyImport.setArtifactSha256(computeSha256(javaDependencyImport.getArtifact()));
        };
    }

    private Runnable updateBazelInfo(
            BazelJavaDependencyImportBuilder javaDependencyImport,
            Set<String> testOnlyCoordinatesWithoutVersion,
            Set<String> neverLinkCoordinatesWithoutVersion,
            Set<String> managedDependenciesVersionlessIds) {
        return () -> {
            var coordinatesStringWithoutVersion =
                    javaDependencyImport.getArtifact().toCoordinatesStringWithoutVersion();
            final var sourceOfTruthInDependenciesFile =
                    mavenDependenciesCollection.findArtifact(coordinatesStringWithoutVersion);

            if (sourceOfTruthInDependenciesFile != null) {
                // copy properties from the source
                javaDependencyImport.setNeverlink(sourceOfTruthInDependenciesFile.isNeverlink());
                javaDependencyImport.setTestonly(sourceOfTruthInDependenciesFile.isTestonly());
            } else {
                // lookup from path
                var dependencyNode = getResolvedDependenyNode(javaDependencyImport);
                var allParents = resolveAndDownloadResult.findAllParentVersionlessIds(dependencyNode);
                javaDependencyImport.setTestonly(
                    !allParents.isEmpty() && allParents.stream()
                            .allMatch(
                                p -> !managedDependenciesVersionlessIds.contains(p)
                                        || testOnlyCoordinatesWithoutVersion.contains(p)));
                javaDependencyImport.setNeverlink(
                    !allParents.isEmpty() && allParents.stream()
                            .allMatch(
                                p -> !managedDependenciesVersionlessIds.contains(p)
                                        || neverLinkCoordinatesWithoutVersion.contains(p)));
            }

            // update managed state
            if (managedDependenciesVersionlessIds.contains(coordinatesStringWithoutVersion)) {
                javaDependencyImport.removeTag(TAG_NOT_IN_COLLECTION);
            } else {
                javaDependencyImport.addTag(TAG_NOT_IN_COLLECTION);
            }
        };
    }

    private Runnable updateDependencyGraph(
            BazelJavaDependencyImportBuilder javaDependencyImport,
            ConcurrentMap<String, BazelJavaDependencyImportBuilder> newCatalogEntriesByVersionlessCoordinates) {
        return () -> {
            final var dependencyNode = getResolvedDependenyNode(javaDependencyImport);
            if (dependencyNode == null) {
                throw new IllegalStateException("Unresolved node: " + javaDependencyImport.getName());
            }

            // we only populate direct dependencies
            final SortedSet<String> deps = new TreeSet<>();
            final SortedSet<String> runtimeDeps = new TreeSet<>();
            final var directDependencies = dependencyNode.getChildren();
            for (final DependencyNode directDep : directDependencies) {
                if (!SKIP_REPLACED_NODES_FILTER.accept(directDep, Collections.emptyList())) {
                    continue;
                }

                // lookup name based on new catalog mappings
                final var directDepCatalogEntry = newCatalogEntriesByVersionlessCoordinates.get(
                    ArtifactIdUtils.toVersionlessId(
                        requireNonNull(directDep.getArtifact(), () -> format("Unresolved artifact: %s", directDep))));
                final var bazelName = requireNonNull(
                    directDepCatalogEntry,
                    () -> format("Missing entry in catalog for dependency: %s", directDep)).getName();
                final var scope = directDep.getDependency().getScope();
                if ("runtime".equals(scope)) {
                    runtimeDeps.add("@" + bazelName);
                } else { // treat test deps as compile deps (we don't differentiate in Bazel)
                    deps.add("@" + bazelName);
                }
            }

            javaDependencyImport.setDeps(deps.isEmpty() ? null : deps);
            javaDependencyImport.setRuntimeDeps(runtimeDeps.isEmpty() ? null : runtimeDeps);
        };
    }

    private Runnable updateSourcesArtifactSha1(BazelJavaDependencyImportBuilder javaDependencyImport) {
        return () -> {
            javaDependencyImport.setSourcesArtifactSha1(computeSha1(javaDependencyImport.getSourcesArtifact()));
        };
    }

    private Runnable updateSourcesArtifactSha256(BazelJavaDependencyImportBuilder javaDependencyImport) {
        return () -> {
            javaDependencyImport.setSourcesArtifactSha256(computeSha256(javaDependencyImport.getSourcesArtifact()));
        };
    }

}