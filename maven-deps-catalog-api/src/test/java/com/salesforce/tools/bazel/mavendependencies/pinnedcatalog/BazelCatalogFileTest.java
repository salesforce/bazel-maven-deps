package com.salesforce.tools.bazel.mavendependencies.pinnedcatalog;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.readString;
import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BazelCatalogFileTest {

    private static final String LOAD_SYMBOL = "@bazel_maven_deps//bazel:jvm.bzl";

    private static final String CATALOG_ROUNDTRIP_TEST_BZL = "catalog_roundtrip_test.bzl";

    @TempDir
    static Path sharedTempDir;

    @BeforeAll
    static void extractTestResources() throws IOException {
        for (String filename : List.of(CATALOG_ROUNDTRIP_TEST_BZL)) {
            try (var in = BazelCatalogFileTest.class.getResourceAsStream("/" + filename)) {
                copy(in, sharedTempDir.resolve(filename));
            }
        }
    }

    SortedSet<String> mavenServers = Collections.emptySortedSet();

    @Test
    public void testRoundtrip() throws IOException {
        // read source
        var sourceFile = sharedTempDir.resolve(CATALOG_ROUNDTRIP_TEST_BZL);
        var catalogFile = BazelCatalogFile.read(sourceFile);

        assertEquals(readString(sourceFile), catalogFile.prettyPrint(mavenServers, LOAD_SYMBOL, null));

        // write target
        var targetDir = sharedTempDir.resolve("dir" + System.nanoTime());
        createDirectories(targetDir);
        var targetFile = targetDir.resolve(CATALOG_ROUNDTRIP_TEST_BZL); // must use same name (because of GROUP)
        writeString(
            targetFile,
            catalogFile.prettyPrint(mavenServers, LOAD_SYMBOL, null),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);

        var catalogFile2 = BazelCatalogFile.read(targetFile);

        // compare content
        assertEquals(catalogFile.getJavaImports(), catalogFile2.getJavaImports());
        assertEquals(catalogFile.getGroup(), catalogFile2.getGroup());

        // compare files
        assertEquals(readString(sourceFile), readString(targetFile));
    }
}
