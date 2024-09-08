package com.salesforce.tools.bazel.mavendependencies.collection;

import java.nio.file.Path;
import java.util.SortedSet;

/**
 * A simple structure to communicate list of modified and deleted files after saving a collection
 */
public class CollectionSaveResult {
    public final SortedSet<Path> writtenFiles;
    public final SortedSet<Path> deletedFiles;
    public final SortedSet<Path> obsoleteFiles;

    public CollectionSaveResult(SortedSet<Path> writtenFiles, SortedSet<Path> deletedFiles,
            SortedSet<Path> obsoleteFiles) {
        this.writtenFiles = writtenFiles;
        this.deletedFiles = deletedFiles;
        this.obsoleteFiles = obsoleteFiles;
    }
}