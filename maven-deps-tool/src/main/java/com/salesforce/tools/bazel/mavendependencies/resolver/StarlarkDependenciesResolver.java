package com.salesforce.tools.bazel.mavendependencies.resolver;

import static com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.DependencyResultWithTransferInfo.toSortedStreamOfMessages;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.slf4j.Logger;

import com.google.common.base.Suppliers;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.cli.helper.UnifiedLogger;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenConventions;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenDependenciesCollection;
import com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.DependencyResultWithTransferInfo;
import com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.MavenDepsNodeListGenerator;
import com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver.MavenDepsRepoSys;

/**
 * This dependencies resolver using Maven Dependency Resolver (Aether).
 */
public class StarlarkDependenciesResolver {

    /**
     * The resolution result.
     * <p>
     * The resolvers performs two phases of resolution:
     * <ol>
     * <li>A single root resolution of all elements in the collection. Available in {@link #getResolveResult()}</li>
     * <li>An individual element resolution for each element in the collection. Available in
     * {@link #getDependencyNodeByIndividuallyResolvedCoordinates()}</li>
     * </ol>
     * The idea is that both resolutions would result into the same tree. If there is a balance mis-match then conflict
     * resolutions must be applied by either managing more versions in the collection or by using exclusion.
     * </p>
     * <p>
     * There is one drawback: the single root resolution result lost most of its dependencies information, i.e the graph
     * is more flat. We attempt to mitigate this by building a merged index. However, do not use the merged index for
     * pinning. It should really be only used for obtaining dependency information about a particular node in the graph
     * (eg., possible parents and children). Additional information is wiped from the merged index.
     * </p>
     */
    public static class ResolveAndDownloadResult {

        private static String ensureUniqueRepositoryIds(String id1, String id2) {
            if (id1.equals(id2)) {
                return id1;
            }

            throw new IllegalStateException("Artifact downloaded twice from '%s' and '%s");
        }

        /**
         * A limited merge function keeping only children, parents and scope
         *
         * @param d1
         * @param d2
         * @return a new merged node
         */
        private static DependencyNode mergeNodesForIndex(DependencyNode d1, DependencyNode d2) {
            // build a new dependency without exclusion information and adjusted scope, i.e. if scopes are different force "compile" scope
            // we don't care about optional and exclusions here
            var newDependency = new Dependency(
                    d1.getArtifact(),
                    d1.getDependency().getScope().equals(d2.getDependency().getScope()) ? d1.getDependency().getScope()
                            : "compile");

            // create a new harmonized node with most of the data initialized to empty/null
            var newNode = new DefaultDependencyNode(newDependency);

            // we care about children for the merged node
            var newChildren = new ArrayList<DependencyNode>(d1.getChildren().size() + d2.getChildren().size());
            newChildren.addAll(d1.getChildren());
            for (DependencyNode d2Child : d2.getChildren()) {
                var existingChild = newChildren.stream()
                        .filter(
                            d1Child -> Objects.equals(
                                ArtifactIdUtils.toId(d1Child.getArtifact()),
                                ArtifactIdUtils.toId(d2Child.getArtifact())))
                        .findAny();
                if (existingChild.isEmpty()) {
                    newChildren.add(d2Child);
                } else {
                    var d1Child = existingChild.get();
                    newChildren.remove(d1Child);
                    newChildren.add(mergeNodesForIndex(d1Child, d2Child));
                }
            }

            newNode.setChildren(newChildren);
            return newNode;
        }

        private final DependencyResult resolveResult;
        private final List<Artifact> resolvedArtifacts;
        private final List<ArtifactResult> optionalSourceAndPomDownloadResults;
        private final Map<String, Artifact> indexOfAllDownloads;
        private final Map<String, DependencyNode> dependencyNodeByResolvedCoordinates;
        private final Supplier<Map<String, String>> indexOfRepositoryIdByArtifactKeySupplier;
        private final List<Dependency> managedDependencies;
        private final Supplier<Map<String, DependencyNode>> indexOfMergedDependencyNodesByArtifactIdSupplier;
        private final List<TransferEvent> transferFailures;

        public ResolveAndDownloadResult(DependencyResult resolveResult, List<Artifact> resolvedArtifacts,
                List<ArtifactResult> optionalSourceAndPomDownloadResults,
                Map<String, DependencyNode> dependencyNodeByResolvedCoordinates, List<Dependency> managedDependencies,
                List<TransferEvent> transferFailures) {
            this.resolveResult = resolveResult;
            this.resolvedArtifacts = resolvedArtifacts;
            this.optionalSourceAndPomDownloadResults = optionalSourceAndPomDownloadResults;
            this.dependencyNodeByResolvedCoordinates = dependencyNodeByResolvedCoordinates;
            this.managedDependencies = managedDependencies;
            this.transferFailures = transferFailures;

            // generate the index for querying for downloads latest
            indexOfAllDownloads =
                    Stream.concat(resolvedArtifacts.stream(), getSuccessfulOptionalSourceAndPomDownloads())
                            .collect(toMap(ArtifactIdUtils::toId, a -> a, (a1, a2) -> a1));

            // create an index of the repository id an artifact was resolved from
            indexOfRepositoryIdByArtifactKeySupplier = Suppliers.memoize(
                () -> resolveResult.getArtifactResults()
                        .parallelStream()
                        .collect(
                            toMap(
                                r -> ArtifactIdUtils.toId(r.getArtifact()),
                                r -> r.getRepository().getId(),
                                ResolveAndDownloadResult::ensureUniqueRepositoryIds)));

            // create a merged index of the resolved dependency nodes by their artifact id from the single as well as the individual resolution nodes
            // and also record their parents so we can actually identify possible paths
            indexOfMergedDependencyNodesByArtifactIdSupplier = Suppliers.memoize(() -> {
                // never consider unresolved nodes
                var skipReplacedNodes = MavenDepsNodeListGenerator.skipReplacedNodes();

                // 1st pass: process and collect nodes from the individual resolution result and the single result
                var resolveResultNodeListGenerator = new MavenDepsNodeListGenerator();
                for (DependencyNode node : dependencyNodeByResolvedCoordinates.values()) {
                    node.accept(resolveResultNodeListGenerator);
                }
                resolveResult.getRoot().accept(resolveResultNodeListGenerator);

                // 2nd pass: go over children and index them too
                // but make sure to prefer the child with the larger (more complete) tree
                return resolveResultNodeListGenerator.getNodes()
                        .parallelStream()
                        .filter(d -> skipReplacedNodes.accept(d, Collections.emptyList()))
                        .collect(toMap(d -> ArtifactIdUtils.toId(d.getArtifact()), d -> d, (d1, d2) -> {
                            if (Objects.equals(d1, d2)) {
                                return d1;
                            }

                            // at this point we can assume the dependencies are equal; just merge their nodes
                            return mergeNodesForIndex(d1, d2);
                        }));

                //return result;
            });
        }

        /**
         * Discovers and collects all possible parents
         *
         * @param search
         *            the node to find all parents
         * @return a set of all parents (direct and indirect)
         */
        public Set<String> findAllParentVersionlessIds(DependencyNode search) {
            var versionlessId = ArtifactIdUtils
                    .toVersionlessId(requireNonNull(search.getArtifact(), () -> "unresolved node: " + search));

            // find all possible paths pointing to the node
            var visitor = new PathRecordingDependencyVisitor(
                    (node, parents) -> (node.getArtifact() != null)
                            && ArtifactIdUtils.toVersionlessId(node.getArtifact()).equals(versionlessId));

            for (DependencyNode resolveRoot : dependencyNodeByResolvedCoordinates.values()) {
                resolveRoot.accept(visitor);
            }
            resolveResult.getRoot().accept(visitor);

            return visitor.getPaths()
                    .stream()
                    .flatMap(List::stream)
                    .map(d -> ArtifactIdUtils.toVersionlessId(d.getArtifact()))
                    .collect(toSet());
        }

        /**
         * @return the resolution result from resolving a dependency from the collection individually (in contrast to
         *         the single root)
         */
        public Map<String, DependencyNode> getDependencyNodeByIndividuallyResolvedCoordinates() {
            return dependencyNodeByResolvedCoordinates;
        }

        /**
         * @param artifactId
         *            the artifact id as spec'd by {@link ArtifactIdUtils#toId(Artifact)}
         * @return an individual {@link DependencyNode} from the merged index matching the specified
         *         {@link ArtifactIdUtils#toId(Artifact) artifact key} (maybe <code>null</code>)
         * @see ResolveAndDownloadResult
         */
        public DependencyNode getDependencyNodeFromMergedIndex(String artifactId) {
            return indexOfMergedDependencyNodesByArtifactIdSupplier.get().get(artifactId);
        }

        public Artifact getDownloadedArtifact(String coordinatesString) {
            return indexOfAllDownloads.get(coordinatesString);
        }

        /**
         * @return the list of dependencies managed by the underlying collection (either directly or via a BOM import)
         */
        public List<Dependency> getManagedDependencies() {
            return managedDependencies;
        }

        public List<ArtifactResult> getOptionalSourceAndPomDownloadResults() {
            return optionalSourceAndPomDownloadResults;
        }

        /**
         * @param artifactKey
         *            see {@link ArtifactIdUtils#toId(String, String, String, String, String)}
         * @return the repository id an artifact was resolved from
         */
        public String getRepositoryId(String artifactKey) {
            return indexOfRepositoryIdByArtifactKeySupplier.get().get(artifactKey);
        }

        public List<Artifact> getResolvedArtifacts() {
            return resolvedArtifacts;
        }

        public DependencyResult getResolveResult() {
            return resolveResult;
        }

        private Stream<Artifact> getSuccessfulOptionalSourceAndPomDownloads() {
            return optionalSourceAndPomDownloadResults.stream()
                    .filter(ArtifactResult::isResolved)
                    .map(ArtifactResult::getArtifact);
        }

        /**
         * @return the transferFailures
         */
        public List<TransferEvent> getTransferFailures() {
            return transferFailures;
        }

        /**
         * @param coordinatesString
         *            sources artifact coordinates
         * @return <code>true</code> if an optional sources artifact with the given coordinates was successful,
         *         <code>false</code> otherwise
         */
        public boolean hasSuccessfulSourcesDownload(String coordinatesString) {
            return indexOfAllDownloads.containsKey(coordinatesString);
        }
    }

    private static final Logger LOG = UnifiedLogger.getLogger();

    private static String toReadableString(Duration duration) {
        return duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();
    }

    private final MavenDepsRepoSys repoSys;

    private final MavenDependenciesCollection mavenDependenciesCollection;

    public StarlarkDependenciesResolver(MavenDependenciesCollection mavenDependenciesCollection,
            MavenDepsRepoSys repoSys) throws IOException {
        this.mavenDependenciesCollection = mavenDependenciesCollection;
        this.repoSys = repoSys;
    }

    private Artifact getPom(Artifact artifact) {
        // create pom artifact only for the standard jar
        if ("jar".equals(artifact.getExtension())) {
            final var classifier = artifact.getClassifier();
            if ((classifier == null) || classifier.isBlank()) {
                return new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        null,
                        "pom",
                        artifact.getVersion());
            }
        }
        return null;
    }

    private Artifact getSourcesJar(Artifact artifact) {
        final var sourcesClassifier =
                MavenConventions.getSourcesClassifier(artifact.getClassifier(), artifact.getExtension());
        if (sourcesClassifier != null) {
            return new DefaultArtifact(
                    artifact.getGroupId(),
                    artifact.getArtifactId(),
                    sourcesClassifier,
                    "jar",
                    artifact.getVersion());
        }
        return null;
    }

    /**
     * Prepares the given lists with dependencies and managed dependencies from the underlying Maven dependencies
     * collection.
     * <p>
     * This will resolve imported BOMs and add their content to the resolution scope.
     * </p>
     *
     * @param out
     *            progress reporting
     * @param dependencies
     *            dependencies allowed to resolve
     * @param managedDependencies
     *            manages versions of dependencies
     *
     * @throws Exception
     */
    private void prepareResolutionScope(
            MessagePrinter out,
            final List<Dependency> dependencies,
            final List<Dependency> managedDependencies) throws Exception {
        LOG.debug("Preparing resolution scope.");

        // collect the managed dependencies from all imported BOMs
        final var importedBomsDependencies = mavenDependenciesCollection.getImportedBoms();
        try (var monitor = out.progressMonitor("Analyzing imported BOMs")) {
            monitor.maxHint(importedBomsDependencies.size());
            for (final MavenArtifact a : importedBomsDependencies) {
                final var importBomDependency = toDependency(a, "import");

                // add the BOM to the list of managed dependencies
                managedDependencies.add(importBomDependency);

                // resolve the BOM
                final var importResult =
                        repoSys.resolveDependencies(List.of(importBomDependency), managedDependencies, monitor);
                if (!importResult.getTransferFailures().isEmpty()) {
                    throw new IllegalStateException(
                            format(
                                "The imported BOM '%s' could not be fully resolved. There were transfer failures:%n - %s",
                                a.toCoordinatesString(),
                                toSortedStreamOfMessages(importResult.getTransferFailures())
                                        .collect(joining("\n - "))));
                }

                // root's children will be the imported BOM
                for (final DependencyNode importedNode : importResult.getDependencyResult().getRoot().getChildren()) {
                    monitor.additionalMessage(
                        importedNode.getArtifact().getGroupId() + ":" + importedNode.getArtifact().getArtifactId());

                    // load the BOM model
                    final var dependencyManagement =
                            repoSys.loadModel(importedNode.getArtifact().getFile(), false, monitor)
                                    .getEffectiveModel()
                                    .getDependencyManagement();
                    if (dependencyManagement == null) {
                        throw new IllegalStateException(
                                format(
                                    "The imported BOM '%s' does not define any managed dependency. It's not a valid BOM for importing!",
                                    importedNode.getArtifact()));
                    }

                    // make every managed dependency from the BOM available and managed (unless it's
                    // excluded)
                    final var selector = new ExclusionDependencySelector(importBomDependency.getExclusions());
                    for (final org.apache.maven.model.Dependency d : dependencyManagement.getDependencies()) {
                        final var dependency = new Dependency(
                                new DefaultArtifact(
                                        d.getGroupId(),
                                        d.getArtifactId(),
                                        d.getClassifier(),
                                        d.getType(),
                                        d.getVersion()),
                                null,
                                null,
                                null);
                        if (selector.selectDependency(dependency)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug(
                                    "Adding dependency '{}:{}' version '{}' for imported BOM '{}'",
                                    d.getGroupId(),
                                    d.getArtifactId(),
                                    d.getVersion(),
                                    importedNode.getArtifact());
                            }
                            dependencies.add(dependency);
                            managedDependencies.add(dependency);
                        } else if (LOG.isDebugEnabled()) {
                            LOG.debug(
                                "Excluding dependency '{}:{}' version '{}' from imported BOM '{}'",
                                d.getGroupId(),
                                d.getArtifactId(),
                                d.getVersion(),
                                importedNode.getArtifact());
                        }
                    }
                }

            }
            monitor.progressBy(1);
        }

        // add all defined dependencies
        final var definedDependencies = mavenDependenciesCollection.getAllDependencies();
        for (final MavenArtifact a : definedDependencies) {
            final var dependency = toDependency(a, "compile");
            dependencies.add(dependency);
            managedDependencies.add(dependency);
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "Adding managed dependency '{}' version '{}'",
                    a.toCoordinatesStringWithoutVersion(),
                    a.getVersion());
            }
        }
    }

    public ResolveAndDownloadResult resolveAndDownload(MessagePrinter out) throws Exception {
        final List<Dependency> dependencies = new ArrayList<>();
        final List<Dependency> managedDependencies = new ArrayList<>();

        prepareResolutionScope(out, dependencies, managedDependencies);

        // resolve
        final var start = Instant.now();
        LOG.debug("Starting dependency resolution: {}", start);

        final ConcurrentMap<String, DependencyNode> resolveResultByCoordinates = new ConcurrentHashMap<>();
        final List<TransferEvent> transferFailures = new CopyOnWriteArrayList<>();
        DependencyResult resolveResult;
        try (var monitor = out.progressMonitor("Resolving dependencies")) {
            monitor.maxHint(-1);
            final List<Exception> collectedExceptions = new CopyOnWriteArrayList<>();

            // 1st pass is a bit involved because we want all the individual trees
            final var calculations =
                    Executors.newWorkStealingPool(Math.min(8, Runtime.getRuntime().availableProcessors()));
            for (final Dependency dependency : dependencies) {
                calculations.submit(() -> {
                    try {
                        final var result =
                                repoSys.resolveDependencies(List.of(dependency), managedDependencies, monitor);
                        final var children = result.getDependencyResult().getRoot().getChildren();
                        if (children.size() != 1) {
                            collectedExceptions.add(
                                new IllegalStateException(
                                        format(
                                            "Unexpected resolution result for '%s':%n%s",
                                            dependency,
                                            children.stream()
                                                    .map(DependencyNode::toString)
                                                    .collect(joining("\n - ", " - ", "\n")))));
                        }
                        final var node = children.get(0);
                        resolveResultByCoordinates.put(ArtifactIdUtils.toId(node.getArtifact()), node);
                        collectedExceptions.addAll(result.getDependencyResult().getCollectExceptions());
                        transferFailures.addAll(result.getTransferFailures());
                    } catch (final DependencyResolutionException e) {
                        LOG.error("Resolution error for '{}': {}", dependency, e.getMessage(), e);
                        collectedExceptions.add(e);
                    }
                });
            }

            // last is a full pass of the whole graph at once
            // (for resolving all version conflicts)
            var resolveResultWithFailures = repoSys.resolveDependencies(dependencies, managedDependencies, monitor);
            resolveResult = resolveResultWithFailures.getDependencyResult();
            transferFailures.addAll(resolveResultWithFailures.getTransferFailures());

            // wait for all resolutions to finish
            calculations.shutdown();
            calculations.awaitTermination(10, TimeUnit.MINUTES);

            // ensure there were no exceptions
            if (!collectedExceptions.isEmpty()) {
                var exception = new IllegalStateException(
                        format(
                            "Unable to resolved the dependency graph properly. The following problems occured:%n%s",
                            collectedExceptions.stream()
                                    .map(Exception::toString)
                                    .collect(joining("\n - ", " - ", "\n"))));
                collectedExceptions.forEach(exception::addSuppressed);
                throw exception;
            }
        }
        out.important(
            format(
                "Resolved compile dependencies for the CRM Core Build (%s)",
                toReadableString(Duration.between(start, Instant.now()))));

        // collect sources (and poms)
        final var nodeListGenerator = new MavenDepsNodeListGenerator();
        resolveResult.getRoot().accept(nodeListGenerator);
        final var resolvedArtifacts =
                nodeListGenerator.getArtifacts(false, MavenDepsNodeListGenerator.skipReplacedNodes());
        final Set<Artifact> optionalSourcesAndPomArtifacts = new HashSet<>(resolvedArtifacts.size());
        try (var monitor = out.progressMonitor("Finding sources and parent poms")) {
            for (final Artifact artifact : resolvedArtifacts) {
                final var sourcesJar = getSourcesJar(artifact);
                if (sourcesJar != null) {
                    optionalSourcesAndPomArtifacts.add(sourcesJar);
                }
                final var pom = getPom(artifact);
                if (pom != null) {
                    optionalSourcesAndPomArtifacts.add(pom);
                }
            }
        }
        List<ArtifactResult> optionalSourceAndPomDownloadResults;
        try (var monitor = out.progressMonitor("Downloading sources")) {
            monitor.maxHint(optionalSourcesAndPomArtifacts.size());
            try {
                optionalSourceAndPomDownloadResults =
                        repoSys.downloadArtifacts(optionalSourcesAndPomArtifacts, monitor);
            } catch (final ArtifactResolutionException e) {
                // all downloads are optional, just collect the results we have and
                optionalSourceAndPomDownloadResults = e.getResults();
            }
        }

        // now build the final list for the catalog
        return new ResolveAndDownloadResult(
                resolveResult,
                resolvedArtifacts,
                optionalSourceAndPomDownloadResults,
                resolveResultByCoordinates,
                managedDependencies,
                transferFailures);
    }

    /**
     * Resolves a single dependency based on the underlying collection
     *
     * @param a
     *            the {@link MavenArtifact} to resolve
     * @param out
     *            progress reporting
     * @return Maven's {@link DependencyResult}
     * @throws Exception
     */
    public DependencyResultWithTransferInfo resolveDependency(MavenArtifact a, MessagePrinter out) throws Exception {
        final List<Dependency> dependencies = new ArrayList<>();
        final List<Dependency> managedDependencies = new ArrayList<>();

        prepareResolutionScope(out, dependencies, managedDependencies);

        // resolve
        final var start = Instant.now();
        DependencyResultWithTransferInfo resolveResult;
        try (var monitor = out.progressMonitor("Resolving dependency")) {
            monitor.maxHint(-1);
            resolveResult = repoSys.resolveDependency(toDependency(a, null), managedDependencies, monitor);
        }
        out.important(
            format(
                "Resolved dependency %s (%s)",
                a.toCoordinatesString(),
                toReadableString(Duration.between(start, Instant.now()))));

        return resolveResult;
    }

    private Dependency toDependency(MavenArtifact a, String scope) {
        final var version = mavenDependenciesCollection.hasVersionVariable(a.getVersion())
                ? mavenDependenciesCollection.getVersionVariableValue(a.getVersion()) : a.getVersion();

        // combine artifact exclusions with banned dependencies
        var allExclusions = a.getExclusions() != null
                ? Stream.concat(a.getExclusions().stream(), mavenDependenciesCollection.getGlobalExclusions())
                        .distinct()
                : mavenDependenciesCollection.getGlobalExclusions();

        final var exclusions =
                allExclusions.map(e -> new Exclusion(e.getGroupId(), e.getArtifactId(), "*", "*")).collect(toSet());
        return new Dependency(
                new DefaultArtifact(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getPackaging(), version),
                scope,
                null,
                exclusions);
    }

}
