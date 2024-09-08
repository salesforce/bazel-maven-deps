package com.salesforce.tools.bazel.mavendependencies.pinnedcatalog;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.salesforce.tools.bazel.cli.helper.ProgressMonitor;
import com.salesforce.tools.bazel.cli.helper.ScmSupport;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionDelta;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionDelta.Modification;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionSaveResult;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenConventions;

/**
 * The Bazel dependency catalog is a set of *.bzl files within the
 * <code>//third_party/dependencies/pinned_catalog</code> package of a Bazel workspace.
 * <p>
 * The catalog exists to fulfill the following requirements:
 * <ul>
 * <li>Pin a full set of dependencies for Bazel</li>
 * <li>Make Maven dependencies available to the Bazel build using <code>jvm_maven_import_external</code></li>
 * <li>Maintain a catalog of all available dependencies including their sha256 and sha1 checksums</li>
 * <li>Support incremental modifications to the catalog</li>
 * <li>Provide common structure to the *.bzl files for easy consumption</li>
 * </ul>
 * </p>
 */
public class BazelDependenciesCatalog {

    private static final String INDEX_BZL = "index.bzl";
    private static final String EXTENSION_BZL = "extension.bzl";
    private static final String BUILD_BAZEL = "BUILD.bazel";
    private static final Path STANDARDIZED_CATALOG_DIRECTORY = Path.of("third_party", "dependencies", "pinned_catalog");

    /**
     * @param additionalLines
     *            additional lines to append to the preamble
     * @return the recommended preamble (which is some comment to not edit the file and the additional lines combined)
     */
    public static String getRecommendedPreamble(String... additionalLines) {
        var preamble = new StringBuilder();
        preamble.append("#").append(System.lineSeparator());
        preamble.append("# Member of the Bazel dependency catalog. DO NOT EDIT.").append(System.lineSeparator());
        preamble.append("#").append(System.lineSeparator());
        preamble.append(System.lineSeparator());
        preamble.append("#").append(System.lineSeparator());
        preamble.append("# This file is generated using tools.").append(System.lineSeparator());
        preamble.append("#   -> Edits will be overridden at any time.").append(System.lineSeparator());
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

    /**
     * Returns a readily loaded catalog for the specified workspace
     *
     * @param workspaceDirectory
     *            the workspace directory (eg., core home)
     * @return the catalog
     * @throws IOException
     *             in case of problems loading the catalog
     */
    public static BazelDependenciesCatalog load(Path workspaceDirectory) throws IOException {
        var catalog = new BazelDependenciesCatalog(workspaceDirectory);
        catalog.load();
        return catalog;
    }

    public static String toGroupFileName(String group) {
        return group.concat(".bzl");
    }

    private final Path workspaceDirectory;

    private volatile ConcurrentMap<String, BazelCatalogFile> catalogFileByGroup;
    private volatile ConcurrentMap<String, BazelJavaDependencyImport> indexByCoordinatesWithoutVersions;

    private volatile ConcurrentMap<String, BazelJavaDependencyImport> indexByTargetName;
    private volatile SortedSet<String> obsoleteGroups;

    private volatile BazelCatalogIndexFile catalogIndexFile;
    private volatile BazelCatalogModuleExtensionFile catalogModuleExtensionFile;

    public BazelDependenciesCatalog(Path workspaceDirectory) {
        this.workspaceDirectory = workspaceDirectory;
    }

    /**
     * @param coordinatesWithoutVersion
     *            the Maven coordinates as specified by {@link MavenArtifact#toCoordinatesStringWithoutVersion()}
     * @return A {@link BazelJavaDependencyImport} matching the coordinates (maybe <code>null</code>)
     */
    public BazelJavaDependencyImport findImportByCoordinatesWithoutVersion(String coordinatesWithoutVersion) {
        return requireNonNull(indexByCoordinatesWithoutVersions, "not loaded").get(coordinatesWithoutVersion);
    }

    /**
     * @param targetName
     *            the target name as specified by {@link BazelJavaDependencyImport#getName()}
     * @return A {@link BazelJavaDependencyImport} matching the coordinates (maybe <code>null</code>)
     */
    public BazelJavaDependencyImport findImportByTargetName(String targetName) {
        return requireNonNull(indexByTargetName, "not loaded").get(targetName);
    }

    /**
     * @param coordinatesWithoutVersionFilter
     *            a filter for Maven coordinates as specified by
     *            {@link MavenArtifact#toCoordinatesStringWithoutVersion()}
     * @return a collection of {@link BazelJavaDependencyImport} matching the filter
     */
    public List<BazelJavaDependencyImport> findImportsByCoordinatesWithoutVersion(
            Predicate<String> coordinatesWithoutVersionFilter) {
        return requireNonNull(indexByCoordinatesWithoutVersions, "not loaded").entrySet()
                .stream()
                .filter(e -> coordinatesWithoutVersionFilter.test(e.getKey()))
                .map(Entry::getValue)
                .map(BazelJavaDependencyImport.class::cast)
                .collect(toList());
    }

    public Stream<BazelJavaDependencyImport> getAllImports() {
        return indexByCoordinatesWithoutVersions.values().stream();
    }

    /**
     * Returns the file group for a given import.
     * <p>
     * Please be aware that the result is not guaranteed to be stable across releases. As we progress with the grouped
     * set of files we anticipate further optimizations. This method is only exposed for low-level operations with the
     * catalog.
     * </p>
     *
     * @param javaDependencyImport
     *            the {@link BazelJavaDependencyImport}
     * @return the file group for the specified import
     */
    public String getGroup(BazelJavaDependencyImport javaDependencyImport) {
        var groupId = javaDependencyImport.getArtifact().getGroupId();
        return MavenConventions.getFileGroup(groupId);
    }

    public Path getGroupFileLocation(String group) {
        return workspaceDirectory.resolve(STANDARDIZED_CATALOG_DIRECTORY).resolve(toGroupFileName(group));
    }

    public Path getWorkspaceDirectory() {
        return workspaceDirectory;
    }

    public void load() throws IOException {
        var catalogDirectory = workspaceDirectory.resolve(STANDARDIZED_CATALOG_DIRECTORY);
        if (isDirectory(catalogDirectory)) {
            // read all except index.bzl files
            try (var fileStream = Files.list(catalogDirectory)) {
                catalogFileByGroup = fileStream.parallel()
                        .filter(
                            p -> isRegularFile(p) && p.getFileName().toString().endsWith(".bzl")
                                    && !INDEX_BZL.equals(p.getFileName().toString())
                                    && !EXTENSION_BZL.equals(p.getFileName().toString()))
                        .map(p -> {
                            try {
                                return BazelCatalogFile.read(p);
                            } catch (IOException e) {
                                throw new IllegalStateException(format("Error reading file '%s'", p.getFileName()), e);
                            }
                        })
                        .collect(toConcurrentMap(BazelCatalogFile::getGroup, f -> f));
            }

            var indexFile = catalogDirectory.resolve(INDEX_BZL);
            if (isRegularFile(indexFile)) {
                catalogIndexFile = BazelCatalogIndexFile.read(indexFile);
            } else {
                catalogIndexFile = new BazelCatalogIndexFile();
            }

            var bzlmodFile = catalogDirectory.resolve(EXTENSION_BZL);
            if (isRegularFile(bzlmodFile)) {
                catalogModuleExtensionFile = BazelCatalogModuleExtensionFile.read(bzlmodFile);
            } else {
                catalogModuleExtensionFile = new BazelCatalogModuleExtensionFile();
            }
        } else {
            catalogFileByGroup = new ConcurrentHashMap<>();
            catalogIndexFile = new BazelCatalogIndexFile();
            catalogModuleExtensionFile = new BazelCatalogModuleExtensionFile();
        }

        rebuildIndexes();
    }

    private void rebuildIndexes() {
        try {
            indexByTargetName = catalogFileByGroup.values()
                    .parallelStream()
                    .flatMap(f -> f.getJavaImports().stream())
                    .collect(toConcurrentMap(BazelJavaDependencyImport::getName, javaImport -> javaImport));
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    format(
                        "Catalog '%s' is corrupt. It contains entries with duplicate names. Please remove the duplicate entries. %s",
                        getWorkspaceDirectory(),
                        e.getMessage()),
                    e);
        }

        try {
            indexByCoordinatesWithoutVersions = catalogFileByGroup.values()
                    .parallelStream()
                    .flatMap(f -> f.getJavaImports().stream())
                    .collect(
                        toConcurrentMap(
                            javaImport -> javaImport.getArtifact().toCoordinatesStringWithoutVersion(),
                            javaImport -> javaImport));
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    format(
                        "Catalog '%s' is corrupt. It contains entries with same Maven coordinates but different versions. This is not allowed. Please remove the excessive entries. %s",
                        getWorkspaceDirectory(),
                        e.getMessage()),
                    e);
        }
    }

    /**
     * Replaces content in the catalog.
     *
     * @param newContent
     *            the new content
     * @param diffsWithFieldContent
     *            set to <code>true</code> if detailed diffs are wanted
     * @return
     */
    public CollectionDelta replaceContent(Stream<BazelJavaDependencyImport> newContent, boolean diffsWithFieldContent) {
        var oldCatalog = catalogFileByGroup;
        Map<String, BazelJavaDependencyImport> oldIndex = new HashMap<>(indexByCoordinatesWithoutVersions);
        SortedSet<String> obsoleteGroups = new TreeSet<>(oldCatalog.keySet());

        SortedSet<Modification> modifications = new TreeSet<>();
        var catalogFileByGroup = new ConcurrentHashMap<String, BazelCatalogFile>();

        Map<String, List<BazelJavaDependencyImport>> newContentByGroup = newContent.collect(groupingBy(this::getGroup));
        for (Entry<String, List<BazelJavaDependencyImport>> newGroupEntry : newContentByGroup.entrySet()) {
            // wrap into new file
            catalogFileByGroup.put(
                newGroupEntry.getKey(),
                new BazelCatalogFile(newGroupEntry.getKey(), new TreeSet<>(newGroupEntry.getValue())));

            // detect delta
            for (BazelJavaDependencyImport newValue : newGroupEntry.getValue()) {
                var mavenCoordinatesWithoutVersion = newValue.getArtifact().toCoordinatesStringWithoutVersion();
                var oldValue = oldIndex.remove(mavenCoordinatesWithoutVersion);
                if (oldValue == null) {
                    modifications.add(
                        Modification.added(
                            newValue.getArtifact().toCoordinatesStringWithoutVersion(),
                            newValue.getArtifact().getVersion()));
                } else {
                    var oldVersion = oldValue.getArtifact().getVersion();
                    var newVersion = newValue.getArtifact().getVersion();
                    var diff = oldValue.diff(newValue, diffsWithFieldContent);
                    if (!Objects.equals(oldVersion, newVersion)) {
                        modifications.add(
                            Modification.versionUpdateWithDiff(
                                newValue.getArtifact().toCoordinatesStringWithoutVersion(),
                                newVersion,
                                oldVersion,
                                diff));
                    } else if (!newValue.equals(oldValue)) {
                        modifications.add(
                            Modification.otherUpdate(
                                newValue.getArtifact().toCoordinatesStringWithoutVersion(),
                                newValue.getArtifact().getVersion(),
                                oldValue.diff(newValue, diffsWithFieldContent)));
                    }

                }
            }

            // group is still present, drop it from obsolete list
            obsoleteGroups.remove(newGroupEntry.getKey());
        }

        // anything remaining in old index will be removed
        for (BazelJavaDependencyImport removed : oldIndex.values()) {
            modifications.add(
                Modification.removed(
                    removed.getArtifact().toCoordinatesStringWithoutVersion(),
                    removed.getArtifact().getVersion()));
        }

        this.catalogFileByGroup = catalogFileByGroup;
        this.obsoleteGroups = obsoleteGroups;
        rebuildIndexes();

        return new CollectionDelta(modifications, obsoleteGroups);
    }

    /**
     * Saves the pinned catalog to disk.
     *
     * @param defaultMavenServers
     *            the list of Maven repositories URLs to inject as default value
     * @param labelForLoadingJvmMavenImportExternalSymbol
     *            label to the <code>bzl</code> file for loading the <code>jvm_maven_import_external</code> symbol
     * @param preamble
     *            a preamble to inject after the load statement (use {@link #getRecommendedPreamble(String)} for a
     *            default)
     * @param monitor
     *            monitor for reporting progress
     * @param scmSupport
     *            SCM tool used to create/write/delete files
     * @return the result of the save operation
     * @throws IOException
     */
    public CollectionSaveResult save(
            SortedSet<String> defaultMavenServers,
            String labelForLoadingJvmMavenImportExternalSymbol,
            String preamble,
            ProgressMonitor monitor,
            ScmSupport scmSupport) throws IOException {
        var catalogDirectory = workspaceDirectory.resolve(STANDARDIZED_CATALOG_DIRECTORY);
        createDirectories(catalogDirectory);

        monitor.maxHint(3 + catalogFileByGroup.size() + (obsoleteGroups != null ? obsoleteGroups.size() : 0));

        SortedSet<Path> modifiedFiles = new TreeSet<>();
        for (BazelCatalogFile file : catalogFileByGroup.values()) {
            var catalogFile = catalogDirectory.resolve(toGroupFileName(file.getGroup()));
            monitor.additionalMessage(catalogFile.getFileName().toString());
            if (scmSupport.writeFile(
                catalogFile,
                file.prettyPrint(defaultMavenServers, labelForLoadingJvmMavenImportExternalSymbol, preamble),
                UTF_8)) {
                modifiedFiles.add(catalogFile);
            }
            monitor.progressBy(1);
        }

        var mainFile = catalogDirectory.resolve(INDEX_BZL);
        monitor.additionalMessage(INDEX_BZL);
        if (scmSupport.writeFile(
            mainFile,
            catalogIndexFile.prettyPrint(
                new TreeSet<>(catalogFileByGroup.keySet()),
                STANDARDIZED_CATALOG_DIRECTORY.toString(),
                defaultMavenServers,
                preamble),
            UTF_8)) {
            modifiedFiles.add(mainFile);
        }
        monitor.progressBy(1);

        var bzlmodFile = catalogDirectory.resolve(EXTENSION_BZL);
        monitor.additionalMessage(EXTENSION_BZL);
        if (scmSupport.writeFile(
            bzlmodFile,
            catalogModuleExtensionFile.prettyPrint(STANDARDIZED_CATALOG_DIRECTORY.toString(), preamble),
            UTF_8)) {
            modifiedFiles.add(bzlmodFile);
        }
        monitor.progressBy(1);

        var buildFile = catalogDirectory.resolve(BUILD_BAZEL);
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
                var obsoleteCatalogFile = catalogDirectory.resolve(toGroupFileName(obsoleteGroup));
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
}
