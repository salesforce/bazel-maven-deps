package com.salesforce.tools.bazel.mavendependencies.collection;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.readString;
import static java.nio.file.Files.writeString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.salesforce.tools.bazel.mavendependencies.starlark.ParseException;

import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.StarlarkFile;

public class MavenDependenciesFileTest {

    private static final String LOAD = "@bazel_maven_deps//bazel:defs.bzl";
    private static final String DEPENDENCIES_ROUNDTRIP_TEST_BZL = "dependencies_roundtrip_test.bzl";
    private static final String DEPENDENCIES_ROUNDTRIP_TEST_CONCISE_BZL = "dependencies_roundtrip_test_concise.bzl";
    private static final String DEPENDENCIES_TEST_BZL = "dependencies_test.bzl";

    @TempDir
    static Path sharedTempDir;

    @BeforeAll
    static void extractTestResources() throws IOException {
        for (String filename : List
                .of(DEPENDENCIES_TEST_BZL, DEPENDENCIES_ROUNDTRIP_TEST_BZL, DEPENDENCIES_ROUNDTRIP_TEST_CONCISE_BZL)) {
            try (var in = MavenDependenciesFileTest.class.getResourceAsStream("/" + filename)) {
                copy(in, sharedTempDir.resolve(filename));
            }
        }
    }

    private void assertRoundtripProducesExpectedOutput(String source, boolean conciseFormat) throws IOException {
        // read source
        var sourceFile = sharedTempDir.resolve(source);
        var dependenciesFile = MavenDependenciesFile.read(sourceFile);

        // write target
        var targetFile = sharedTempDir.resolve("dependencies_roundtrip_test_" + System.nanoTime() + ".bzl");
        writeString(
            targetFile,
            dependenciesFile.prettyPrint(LOAD, MavenDependenciesCollection.getRecommendedPreamble(), conciseFormat),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);

        var dependenciesFile2 = MavenDependenciesFile.read(targetFile);

        // compare content
        assertEquals(dependenciesFile.getVersionVariables(), dependenciesFile2.getVersionVariables());
        assertEquals(dependenciesFile.getImportedBoms(), dependenciesFile2.getImportedBoms());
        assertEquals(dependenciesFile.getDependencies(), dependenciesFile2.getDependencies());

        // compare files
        assertEquals(readString(sourceFile), readString(targetFile));
    }

    @Test
    public void testParseException() {
        var file = StarlarkFile.parse(ParserInput.fromLines("ASSIGNMENT = \"hello\""));
        var exception = new ParseException("test", file.getStatements().get(0));

        assertEquals("Invalid file: test (:1:1)\n ASSIGNMENT = \"hello\"\n", exception.getMessage());
    }

    @Test
    public void testRead() throws IOException {
        var dependenciesFile = MavenDependenciesFile.read(sharedTempDir.resolve(DEPENDENCIES_TEST_BZL));

        assertEquals("3.12.0", dependenciesFile.getVersion("_COMMONS_LANG3_VERSION"));
        assertEquals("1.9", dependenciesFile.getVersion("_COMMONS_TEXT_VERSION"));
        assertEquals("2.2", dependenciesFile.getVersion("_HAMCREST_VERSION"));
        assertEquals(6, dependenciesFile.getVersionVariables().size());

        var importedBoms = dependenciesFile.getImportedBoms();
        assertEquals(2, importedBoms.size());

        var jacksonBom = importedBoms.stream().filter(a -> "jackson-bom".equals(a.getArtifactId())).findFirst();
        assertTrue(jacksonBom.isPresent());
        assertNotNull(jacksonBom.get().getExclusions());
        assertEquals(1, jacksonBom.get().getExclusions().size());

        var dependencies = dependenciesFile.getDependencies();
        assertEquals(12, dependencies.size());

        assertThat(
            dependencies,
            hasItem(new MavenArtifact("org.apache.maven", "maven-core", "3.9.6", "jar", null, null, false, false)));
        assertThat(
            dependencies,
            not(
                hasItem(
                    new MavenArtifact(
                            "org.hamcrest",
                            "hamcrest",
                            "_HAMCREST_VERSION",
                            "jar",
                            null,
                            null,
                            true,
                            true))));
    }

    @Test
    public void testRoundtripConcise() throws IOException {
        assertRoundtripProducesExpectedOutput(DEPENDENCIES_ROUNDTRIP_TEST_CONCISE_BZL, true);
    }

    @Test
    public void testRoundtripNonConcise() throws IOException {
        assertRoundtripProducesExpectedOutput(DEPENDENCIES_ROUNDTRIP_TEST_BZL, false);
    }
}
