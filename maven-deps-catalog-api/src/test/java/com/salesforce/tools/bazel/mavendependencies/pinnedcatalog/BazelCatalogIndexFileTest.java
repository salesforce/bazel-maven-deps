package com.salesforce.tools.bazel.mavendependencies.pinnedcatalog;

import static java.nio.file.Files.readString;
import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.SortedSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BazelCatalogIndexFileTest {

    @TempDir
    static Path sharedTempDir;

    SortedSet<String> mavenServers = Collections.emptySortedSet();
    SortedSet<String> fileGroups = Collections.emptySortedSet();
    String catalogDirectory = "catalog";

    @Test
    public void testRoundtrip() throws IOException {
        // create initial file
        var sourceFile = sharedTempDir.resolve("index.bzl");
        writeString(
            sourceFile,
            new BazelCatalogIndexFile().prettyPrint(fileGroups, catalogDirectory, mavenServers, null),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);

        // read file again
        var indexFile = BazelCatalogIndexFile.read(sourceFile);
        assertEquals(readString(sourceFile), indexFile.prettyPrint(fileGroups, catalogDirectory, mavenServers, null));
    }
}
