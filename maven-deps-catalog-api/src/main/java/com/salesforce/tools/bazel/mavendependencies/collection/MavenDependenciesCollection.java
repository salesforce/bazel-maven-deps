package com.salesforce.tools.bazel.mavendependencies.collection;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.salesforce.tools.bazel.cli.helper.ProgressMonitor;
import com.salesforce.tools.bazel.cli.helper.ScmSupport;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionDelta.Modification;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact.Exclusion;

/**
 * The Maven dependencies collection is a set of {@link MavenDependenciesFile *.bzl dependency files} within the
 * <code>//third_party/dependencies/</code> package.
 * <p>
 * The collection exists to fulfill the following requirements:
 * <ul>
 * <li>Allow developers to declare dependencies used in the build</li>
 * <li>Source of truth for Bazel and Maven</li>
 * <li>Store dependencies in multiple files to reduce merge conflicts</li>
 * <li>Provide common structure to the *.bzl files for easy consumption</li>
 * <li>Ensure there is at most one version of any dependency declared in the whole collection</li>
 * </ul>
 * </p>
 * <p>
 * Clients should use this collection for modifying any dependencies.
 * </p>
 */
public class MavenDependenciesCollection {

    public static final String INDEX_BZL = "index.bzl";
    private static final String BUILD_BAZEL = "BUILD.bazel";
    private static final Path STANDARDIZED_COLLECTION_DIRECTORY = Path.of("third_party", "dependencies");

    /**
     * @param additionalLines
     *            additional lines to append to the preamble
     * @return the recommended preamble (which is some comment to not edit the file and the additional lines combined)
     */
    public static String getRecommendedPreamble(String... additionalLines) {
        var preamble = new StringBuilder();
        preamble.append("#").append(System.lineSeparator());
        preamble.append("# Collection of Maven dependencies for this Bazel workspace").append(System.lineSeparator());
        preamble.append("#").append(System.lineSeparator());
        preamble.append(System.lineSeparator());
        preamble.append("#").append(System.lineSeparator());
        preamble.append("# This file is manipulated using tools.").append(System.lineSeparator());
        preamble.append("#   -> Formatting and comments will not be preserved.").append(System.lineSeparator());
        preamble.append("#   -> Use a TODO file to capture additional notes/technical debt.")
                .append(System.lineSeparator());
        preamble.append("#").append(System.lineSeparator());
        if (additionalLines != null) {
            for (String line : additionalLines) {
                if (line != null) {
                    preamble.append(line);
                    if (!line.endsWith(System.lineSeparator())) {
                        preamble.append(System.lineSeparator());
                    }
                }
            }
        }
        return preamble.toString();
    }

    public static String toGroupFileName(String group) {
        return group.concat(".bzl");
    }

    private final Path workspaceDirectory;

    private volatile ConcurrentMap<String, MavenDependenciesFile> dependenciesFileByGroup;
    private volatile ConcurrentMap<String, MavenArtifact> indexByCoordinatesWithoutVersion;
    private volatile SortedSet<String> obsoleteGroups;
    private volatile ConcurrentMap<String, String> versionVariableIndex;
    private volatile SortedSet<Exclusion> globalExclusions;
    private volatile MavenDependenciesCollectionIndexFile collectionIndexFile;

    public MavenDependenciesCollection(Path workspaceDirectory) {
        this.workspaceDirectory = workspaceDirectory;
    }

    /**
     * Creates a new dependency without version variables.
     *
     * @param artifact
     */
    public void addDependency(MavenArtifact artifact) {
        requireNonNull(artifact, "artifact must not be null");

        addDependency(artifact, null, null);
    }

    private void addDependency(MavenArtifact artifact, String versionName, String versionValue) {
        if (isExcludedByGlobalExclusions(artifact)) {
            throw new IllegalArgumentException(
                    format(
                        "Artifact '%s' cannot be added because it's already excluded via a global exclusion.",
                        artifact.toCoordinatesStringWithoutVersion()));
        }

        var group = getGroup(artifact);
        initializeFileGroup(group);

        if ((versionName != null) && (versionValue != null)) {
            if (!Objects.equals(versionName, artifact.getVersion())) {
                throw new IllegalArgumentException(
                        format(
                            "When adding an artifact with a version variable to the collection the artifact's version should be the version variable name. Artifact '%s' does not satisfy this criteria (expected '%s', found '%s')",
                            artifact.toCoordinatesStringWithoutVersion(),
                            versionName,
                            artifact.getVersion()));
            }
            dependenciesFileByGroup.get(group).setVersion(versionName, versionValue);
        }
        dependenciesFileByGroup.get(group).getDependencies().add(artifact);

        rebuildIndices();
    }

    /**
     * Creates a new dependency and adds (or updates) the given version variable to the specified value.
     *
     * @param artifact
     * @param versionName
     * @param versionValue
     */
    public void addDependencyWithManagedVersion(MavenArtifact artifact, String versionName, String versionValue) {
        requireNonNull(artifact, "artifact must not be null");
        requireNonNull(versionName, "versionName must not be null");
        requireNonNull(versionValue, "versionValue must not be null");

        addDependency(artifact, versionName, versionValue);
    }

    /**
     * Creates a new global exclusions.
     *
     * @param exclusion
     */
    public void addExclusion(Exclusion exclusion) {
        requireNonNull(exclusion, "exclusion must not be null");

        // check that this does not affect the collection
        List<MavenArtifact> excludedArtifacts = indexByCoordinatesWithoutVersion.values()
                .parallelStream()
                .filter(a -> exclusion.matches(a.getGroupId(), a.getArtifactId()))
                .sorted()
                .collect(toList());
        if (!excludedArtifacts.isEmpty()) {
            throw new IllegalStateException(
                    format(
                        "Cannot add exclusion '%s' because it matches existing artifacts  in the dependencies collection.%nThe following artifacts are impacted. Please remove them first!%n%n%s",
                        exclusion,
                        excludedArtifacts.stream()
                                .map(MavenArtifact::toCoordinatesStringWithoutVersion)
                                .collect(joining(System.lineSeparator() + " - ", " - ", ""))));
        }

        var group = getGroup(exclusion);
        initializeFileGroup(group);

        dependenciesFileByGroup.get(group).getExclusions().add(exclusion);

        rebuildIndices();
    }

    public void addImportedBom(MavenArtifact artifact) {
        var group = getGroup(artifact);
        initializeFileGroup(group);

        dependenciesFileByGroup.get(group).getImportedBoms().add(artifact);

        rebuildIndices();
    }

    /**
     * @param coordinatesWithoutVersion
     *            the Maven coordinates (without version) as specified by
     *            {@link MavenArtifact#toCoordinatesStringWithoutVersion()},
     * @return A {@link BazelJavaDependencyImport} matching the coordinates
     */
    public MavenArtifact findArtifact(String coordinatesWithoutVersion) {
        return requireNonNull(indexByCoordinatesWithoutVersion, "not loaded").get(coordinatesWithoutVersion);
    }

    /**
     * @return stream of all {@link MavenArtifact artifacts} in this collection
     */
    public Stream<MavenArtifact> getAllArtifacts() {
        return indexByCoordinatesWithoutVersion.values().stream();
    }

    /**
     * @param versionVariableName
     *            the variable name to search for
     * @return sorted collection of all {@link MavenArtifact artifacts} using the specified version variable name of
     *         this collection
     */
    public SortedSet<MavenArtifact> getAllArtifactsUsingVersionVariable(String versionVariableName) {
        return indexByCoordinatesWithoutVersion.values()
                .stream()
                .filter(a -> a.getVersion().equals(versionVariableName))
                .collect(toCollection(TreeSet::new));
    }

    /**
     * @return sorted collection of all {@link MavenArtifact artifacts excluding poms} in this collection
     */
    public SortedSet<MavenArtifact> getAllDependencies() {
        return indexByCoordinatesWithoutVersion.values()
                .stream()
                .filter(Predicate.not(MavenArtifact::isPomPackaging))
                .collect(toCollection(TreeSet::new));
    }

    public Path getDirectory() {
        return workspaceDirectory.resolve(STANDARDIZED_COLLECTION_DIRECTORY);
    }

    /**
     * Returns a stream of global exclusions to be applied to all dependencies.
     *
     * @return stream of global exclusions
     */
    public Stream<Exclusion> getGlobalExclusions() {
        return globalExclusions.stream();
    }

    /**
     * Returns the file group for a given exclusions.
     * <p>
     * Please be aware that the result is not guaranteed to be stable across releases. As we progress with the grouped
     * set of files we anticipate further optimizations. This method is only exposed for low-level operations with the
     * collection (such as tests).
     * </p>
     *
     * @param exclusion
     *            the {@link Exclusion}
     * @return the file group for the specified exclusions
     * @throws IllegalArgumentException
     *             if the exclusion has a group id with a <code>*</code> wildcard
     */
    public String getGroup(Exclusion exclusion) {
        var groupId = requireNonNull(exclusion.getGroupId(), "missing groupID in exclusion");
        if (groupId.indexOf('*') != -1) {
            throw new IllegalArgumentException(
                    "Exclusion '%s' is not supported. A full Maven group id is required. Wildcards cannot be used at this point.");
        }
        return MavenConventions.getFileGroup(groupId);
    }

    /**
     * Returns the file group for a given artifact.
     * <p>
     * Please be aware that the result is not guaranteed to be stable across releases. As we progress with the grouped
     * set of files we anticipate further optimizations. This method is only exposed for low-level operations with the
     * collection (such as tests).
     * </p>
     *
     * @param artifact
     *            the {@link MavenArtifact}
     * @return the file group for the specified artifact
     */
    public String getGroup(MavenArtifact artifact) {
        var groupId = artifact.getGroupId();
        return MavenConventions.getFileGroup(groupId);
    }

    public Path getGroupFileLocation(String group) {
        return getDirectory().resolve(toGroupFileName(group));
    }

    public SortedSet<MavenArtifact> getImportedBoms() {
        return indexByCoordinatesWithoutVersion.values()
                .stream()
                .filter(MavenArtifact::isPomPackaging)
                .collect(toCollection(TreeSet::new));
    }

    public Stream<String> getVersionVariableNames() {
        return versionVariableIndex.keySet().stream();
    }

    public String getVersionVariableValue(String versionVariableName) {
        return versionVariableIndex.get(versionVariableName);
    }

    public Path getWorkspaceDirectory() {
        return workspaceDirectory;
    }

    public boolean hasVersionVariable(String versionVariableName) {
        return versionVariableIndex.containsKey(versionVariableName);
    }

    /**
     * Checks to see if a given fileGroup already exists, and if not, initializes a new one and adds to
     * dependenciesFileByGroup.
     *
     * @param fileGroup
     */
    private void initializeFileGroup(String fileGroup) {
        if (!dependenciesFileByGroup.containsKey(fileGroup)) {
            dependenciesFileByGroup.put(
                fileGroup,
                new MavenDependenciesFile(
                        new TreeMap<>(),
                        new TreeSet<>(),
                        new TreeSet<>(),
                        new TreeSet<>(),
                        getGroupFileLocation(fileGroup)));
        }
    }

    public boolean isEmpty() {
        return indexByCoordinatesWithoutVersion.isEmpty();
    }

    /**
     * Indicates if the artifact matches a defined global exclusion.
     *
     * @param artifact
     *            the artifact to check (must not be <code>null</code>)
     * @return <code>true</code> if a global exclusion exist matching the artifact, <code>false</code> otherwise
     */
    public boolean isExcludedByGlobalExclusions(MavenArtifact artifact) {
        return globalExclusions.parallelStream()
                .anyMatch(e -> e.matches(artifact.getGroupId(), artifact.getArtifactId()));
    }

    public void load() throws IOException {
        var collectionDirectory = getDirectory();
        if (isDirectory(collectionDirectory)) {
            // read all except index.bzl files
            try (var fileStream = Files.list(collectionDirectory)) {
                dependenciesFileByGroup = fileStream.parallel()
                        .filter(
                            p -> isRegularFile(p) && p.getFileName().toString().endsWith(".bzl")
                                    && !INDEX_BZL.equals(p.getFileName().toString()))
                        .map(p -> {
                            try {
                                return MavenDependenciesFile.read(p);
                            } catch (IOException e) {
                                throw new IllegalStateException(
                                        format("Error reading file '%s': %s", p.getFileName(), e.getMessage()),
                                        e);
                            }
                        })
                        .collect(toConcurrentMap(MavenDependenciesFile::getGroup, f -> f));
            }
            var indexFile = collectionDirectory.resolve(INDEX_BZL);
            if (isRegularFile(indexFile)) {
                collectionIndexFile = MavenDependenciesCollectionIndexFile.read(indexFile);
            } else {
                collectionIndexFile = new MavenDependenciesCollectionIndexFile();
            }
        } else {
            dependenciesFileByGroup = new ConcurrentHashMap<>();
            collectionIndexFile = new MavenDependenciesCollectionIndexFile();
        }

        rebuildIndices();
    }

    private void rebuildIndices() {
        indexByCoordinatesWithoutVersion = dependenciesFileByGroup.values()
                .parallelStream()
                .flatMap(f -> Stream.concat(f.getDependencies().stream(), f.getImportedBoms().stream()))
                .collect(
                    toConcurrentMap(MavenArtifact::toCoordinatesStringWithoutVersion, Function.identity(), (a1, a2) -> {
                        throw new IllegalStateException(
                                format(
                                    "Duplicate dependency entries in '%s': '%s'%n%nThis is not allowed! Please ensure there is only one version and no duplicate entries.",
                                    getDirectory(),
                                    a1.toCoordinatesStringWithoutVersion()));
                    }));

        versionVariableIndex = dependenciesFileByGroup.values()
                .parallelStream()
                .flatMap(f -> f.getVersionVariables().entrySet().stream())
                .collect(toConcurrentMap(Entry::getKey, Function.identity(), (e1, e2) -> {
                    if (!Objects.equals(e1.getValue(), e2.getValue())) {
                        throw new IllegalStateException(
                                format(
                                    "Duplicate variable in '%s': '%s' with '%s' <> '%s'%nThis is not allowed! Please ensure all version variables have the same value.%n%n",
                                    getDirectory(),
                                    e1.getKey(),
                                    e1.getValue(),
                                    e2.getValue()));
                    }
                    return e1;
                }))
                .entrySet()
                .parallelStream()
                .collect((toConcurrentMap(Entry::getKey, e -> e.getValue().getValue())));

        globalExclusions = dependenciesFileByGroup.values()
                .parallelStream()
                .flatMap(f -> f.getExclusions().stream())
                .collect(toCollection(TreeSet::new));

        // ensure no exclusion is also listed as a dependency
        List<MavenArtifact> excludedArtifacts = indexByCoordinatesWithoutVersion.values()
                .parallelStream()
                .filter(this::isExcludedByGlobalExclusions)
                .sorted()
                .collect(toList());
        if (!excludedArtifacts.isEmpty()) {
            throw new IllegalStateException(
                    format(
                        "Conflicting information found in the dependencies collection.%nThe following artifacts are both - excluded and listed. Only one is allowed!%n%n%s",
                        excludedArtifacts.stream()
                                .map(MavenArtifact::toCoordinatesStringWithoutVersion)
                                .collect(joining(System.lineSeparator() + " - ", " - ", ""))));
        }
    }

    public boolean removeDependency(MavenArtifact artifact) {
        var group = getGroup(artifact);

        if (!dependenciesFileByGroup.containsKey(group)) {
            return false;
        }

        var removed = dependenciesFileByGroup.get(group).getDependencies().remove(artifact);

        rebuildIndices();

        return removed;
    }

    public boolean removeExclusion(Exclusion exclusion) {
        requireNonNull(exclusion, "exclusion must not be null");

        var group = getGroup(exclusion);
        if (!dependenciesFileByGroup.containsKey(group)) {
            return false;
        }

        var removed = dependenciesFileByGroup.get(group).getExclusions().remove(exclusion);

        rebuildIndices();

        return removed;
    }

    public boolean removeImportedBom(MavenArtifact artifact) {
        var group = getGroup(artifact);

        if (!dependenciesFileByGroup.containsKey(group)) {
            return false;
        }

        var removed = dependenciesFileByGroup.get(group).getImportedBoms().remove(artifact);

        rebuildIndices();

        return removed;
    }

    public void removeVersionVariable(String versionVariableName) {
        // need to update all catalogs the version is present in
        for (MavenDependenciesFile dependenciesFile : dependenciesFileByGroup.values()) {
            if (dependenciesFile.getVersionVariables().containsKey(versionVariableName)) {
                dependenciesFile.getVersionVariables().remove(versionVariableName);
            }
        }

        rebuildIndices();
    }

    public CollectionDelta replaceContent(
            Map<String, String> newVersionVariables,
            SortedSet<MavenArtifact> newImportedBoms,
            SortedSet<MavenArtifact> newDependencies,
            SortedSet<Exclusion> newExclusions) {
        var oldCollection = dependenciesFileByGroup;
        Map<String, MavenArtifact> oldIndex = new HashMap<>(indexByCoordinatesWithoutVersion);
        Map<String, String> oldVersionVariables = new HashMap<>(versionVariableIndex);
        SortedSet<String> obsoleteGroups = new TreeSet<>(oldCollection.keySet());
        SortedSet<Exclusion> oldExclusions = new TreeSet<>(globalExclusions);

        SortedSet<Modification> modifications = new TreeSet<>();

        var dependenciesFileByGroup = new ConcurrentHashMap<String, MavenDependenciesFile>();

        Stream.concat(newImportedBoms.stream(), newDependencies.stream()).forEach(newArtifact -> {
            var group = getGroup(newArtifact);

            // ensure new collection is populated with an empty file
            if (!dependenciesFileByGroup.containsKey(group)) {
                dependenciesFileByGroup.put(
                    group,
                    new MavenDependenciesFile(
                            new TreeMap<>(),
                            new TreeSet<>(),
                            new TreeSet<>(),
                            new TreeSet<>(),
                            getGroupFileLocation(group)));
            }

            // store the new dependency
            if (newArtifact.isPomPackaging()) {
                dependenciesFileByGroup.get(group).getImportedBoms().add(newArtifact);
            } else {
                dependenciesFileByGroup.get(group).getDependencies().add(newArtifact);
            }

            // create the managed version
            if (newVersionVariables.containsKey(newArtifact.getVersion())) {
                dependenciesFileByGroup.get(group)
                        .setVersion(newArtifact.getVersion(), newVersionVariables.get(newArtifact.getVersion()));
            }

            // detect delta
            var mavenCoordinatesWithoutVersion = newArtifact.toCoordinatesStringWithoutVersion();
            var newVersion = newVersionVariables.containsKey(newArtifact.getVersion())
                    ? newVersionVariables.get(newArtifact.getVersion()) : newArtifact.getVersion();
            var oldValue = oldIndex.remove(mavenCoordinatesWithoutVersion);
            if (oldValue == null) {
                modifications.add(Modification.added(mavenCoordinatesWithoutVersion, newVersion));
            } else {
                var oldVersion = oldVersionVariables.containsKey(oldValue.getVersion())
                        ? oldVersionVariables.get(oldValue.getVersion()) : oldValue.getVersion();
                if (!Objects.equals(oldVersion, newVersion)) {
                    modifications
                            .add(Modification.versionUpdate(mavenCoordinatesWithoutVersion, newVersion, oldVersion));
                } else if (!newArtifact.equals(oldValue)) {
                    modifications.add(Modification.otherUpdate(mavenCoordinatesWithoutVersion, newVersion, null));
                }
            }

            // group is still present, drop it from obsolete list
            obsoleteGroups.remove(group);
        });

        // anything remaining in old index will be removed
        for (MavenArtifact removed : oldIndex.values()) {
            modifications.add(
                Modification.removed(
                    removed.toCoordinatesStringWithoutVersion(),
                    oldVersionVariables.containsKey(removed.getVersion())
                            ? oldVersionVariables.get(removed.getVersion()) : removed.getVersion()));
        }

        // process new global exclusions
        for (Exclusion newExclusion : newExclusions) {
            var group = getGroup(newExclusion);

            // ensure new collection is populated with an empty file
            if (!dependenciesFileByGroup.containsKey(group)) {
                dependenciesFileByGroup.put(
                    group,
                    new MavenDependenciesFile(
                            new TreeMap<>(),
                            new TreeSet<>(),
                            new TreeSet<>(),
                            new TreeSet<>(),
                            getGroupFileLocation(group)));
            }

            // store the new exclusion
            dependenciesFileByGroup.get(group).getExclusions().add(newExclusion);

            // detect delta
            if (!oldExclusions.remove(newExclusion)) {
                modifications.add(Modification.exclusionAdded(newExclusion.getGroupId(), newExclusion.getArtifactId()));
            }

            // group is still present, drop it from obsolete list
            obsoleteGroups.remove(group);
        }

        // any exclusions remaining in old index will be removed
        for (Exclusion removed : oldExclusions) {
            modifications.add(Modification.exclusionRemoved(removed.getGroupId(), removed.getArtifactId()));
        }

        this.dependenciesFileByGroup = dependenciesFileByGroup;
        this.obsoleteGroups = obsoleteGroups;
        rebuildIndices();

        return new CollectionDelta(modifications, obsoleteGroups);
    }

    /**
     * Saves the dependencies collection to disk.
     *
     * @param labelForLoadingMavenSymbol
     *            label to the <code>bzl</code> file for loading the <code>maven</code> symbol
     * @param preamble
     *            a preamble to inject after the load statement (use {@link #getRecommendedPreamble(String)} for a
     *            default)
     * @param conciseFormat
     *            set to <code>true</code> to enable concise format, i.e. Maven coordinate strings instead of
     *            <code>maven.artifact</code> macro calls
     * @param skipIndexFile
     *            skip writing of an index file
     * @param monitor
     *            monitor for reporting progress
     * @param scmSupport
     *            SCM tool used to create/write/delete files
     * @return the result of the save operation
     * @throws IOException
     */
    public CollectionSaveResult save(
            String labelForLoadingMavenSymbol,
            String preamble,
            boolean conciseFormat,
            boolean skipIndexFile,
            ProgressMonitor monitor,
            ScmSupport scmSupport) throws IOException {
        var collectionDirectory = getDirectory();
        createDirectories(collectionDirectory);

        monitor.maxHint(2 + dependenciesFileByGroup.size() + (obsoleteGroups != null ? obsoleteGroups.size() : 0));

        SortedSet<Path> modifiedFiles = new TreeSet<>();
        for (MavenDependenciesFile file : dependenciesFileByGroup.values()) {
            var catalogFile = collectionDirectory.resolve(toGroupFileName(file.getGroup()));
            monitor.additionalMessage(catalogFile.getFileName().toString());
            if (scmSupport.writeFile(
                catalogFile,
                file.prettyPrint(labelForLoadingMavenSymbol, preamble, conciseFormat),
                StandardCharsets.UTF_8)) {
                modifiedFiles.add(catalogFile);
            }
            monitor.progressBy(1);
        }

        if (!skipIndexFile) {
            var mainFile = collectionDirectory.resolve(INDEX_BZL);
            monitor.additionalMessage(INDEX_BZL);
            if (scmSupport.writeFile(
                mainFile,
                collectionIndexFile.prettyPrint(
                    new TreeSet<>(dependenciesFileByGroup.keySet()),
                    STANDARDIZED_COLLECTION_DIRECTORY.toString()),
                UTF_8)) {
                modifiedFiles.add(mainFile);
            }
        }
        monitor.progressBy(1);

        var buildFile = collectionDirectory.resolve(BUILD_BAZEL);
        if (!isRegularFile(buildFile)) {
            monitor.additionalMessage(BUILD_BAZEL);
            if (scmSupport.writeFile(buildFile, "", UTF_8)) {
                modifiedFiles.add(buildFile);
            }
        }
        monitor.progressBy(1);

        SortedSet<Path> deletedFiles = new TreeSet<>();
        SortedSet<Path> obsoleteFiles = new TreeSet<>();
        if (obsoleteGroups != null) {
            for (String obsoleteGroup : obsoleteGroups) {
                // delete or clear out any obsolete files
                var obsoleteCatalogFile = collectionDirectory.resolve(toGroupFileName(obsoleteGroup));
                monitor.additionalMessage(obsoleteCatalogFile.getFileName().toString());
                if (scmSupport.removeFile(obsoleteCatalogFile)) {
                    deletedFiles.add(obsoleteCatalogFile);
                } else {
                    obsoleteFiles.add(obsoleteCatalogFile);
                }
                monitor.progressBy(1);
            }

            // reset modifications
            obsoleteGroups = null;
        }

        return new CollectionSaveResult(modifiedFiles, deletedFiles, obsoleteFiles);
    }

    /**
     * Updates an existing version variable. Does nothing if the variable cannot be found.
     *
     * @param versionVariableName
     * @param value
     * @return the previous value of the version variable (or null)
     */
    public String updateVersionVariable(String versionVariableName, String value) {
        // need to update all catalogs the version is present in
        String oldVersion = null;
        for (MavenDependenciesFile dependenciesFile : dependenciesFileByGroup.values()) {
            if (dependenciesFile.getVersionVariables().containsKey(versionVariableName)) {
                oldVersion = dependenciesFile.getVersionVariables().put(versionVariableName, value);
            }
        }

        rebuildIndices();

        return oldVersion;
    }
}
