package com.salesforce.tools.bazel.mavendependencies.tool.cli.it;

import static java.lang.String.format;
import static java.nio.file.Files.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.salesforce.tools.bazel.mavendependencies.starlark.BazelConventions;

/**
 * A set of integration tests for the Maven produced jar-with-dependencies version.
 * <p>
 * These test launch a different JVM in order to test the fat jar. They ensure that the fat is operational, i.e.
 * complete with all its dependencies for execution of the commands. This covers corecli as well as pre-checkin usage.
 * </p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class JarWithDependenciesIT {

    private static Path workingDirectory;
    private static Path jarWithDependenciesFile;
    private static Path javaExecutable;
    private static Path targetDirectory;

    @BeforeAll
    static void initialize() throws IOException {
        workingDirectory = Paths.get(System.getProperty("user.dir"));
        assertTrue(
            workingDirectory.getFileName().toString().equals("tool"),
            "This test must be run using the module 'tool' as working directory!");

        javaExecutable = Paths.get(System.getProperty("java.home"), "bin/java");
        assertTrue(
            isExecutable(javaExecutable),
            format(
                "No executable java binary found at '%s'. System property 'java.home' not set. The test does not support this environment!",
                javaExecutable));

        targetDirectory = workingDirectory.resolve("target");
        try (var jars = find(targetDirectory, 1, JarWithDependenciesIT::isJarWithDependenciesOrDirectory)) {
            List<Path> result = jars.filter(Files::isRegularFile).collect(toList());
            assertFalse(
                result.isEmpty(),
                () -> format(
                    "No jar-with-dependencies jar file found in '%s'. Did you run a Maven build?",
                    targetDirectory));
            assertThat(
                "Multiple jar-with-dependencies found in target directory. Please run 'clean' after updating the version!: "
                        + result.stream().map(Path::toString).collect(joining(", ")),
                result,
                hasSize(1));
            jarWithDependenciesFile = result.get(0);
            assertNotNull(jarWithDependenciesFile, "Bug in Java? null entry in Files.find result!");
        }
    }

    private static boolean isJarWithDependenciesOrDirectory(Path p, BasicFileAttributes a) {
        if (a.isDirectory()) {
            return true;
        }

        var fileName = p.getFileName().toString();
        return fileName.startsWith("module-graph-tool-") && fileName.endsWith("-jar-with-dependencies.jar");
    }

    private static String tryRead(Path file) {
        try {
            return readString(file);
        } catch (IOException e) {
            return e.toString();
        }
    }

    private Path testDirectory;
    private Path testWorkingDirectory;
    private Path logFile;

    private List<String> logLines;

    @Test
    public final void _001_add_dependency() throws Exception {
        prepareTestEnvironment("001");

        runGraphTool("add-dependency", List.of("--dependency", "commons-collections:commons-collections:3.2.2"), false);
        assertOutputLogged(
            "Added dependency 'commons-collections:commons-collections:jar:_COMMONS_COLLECTIONS_VERSION' to dependencies collection");

        // this should be the last check (so we can re-use the generated output if needed)
        assertTestOutputContainsExpectedContent();
    }

    @Test
    public final void _002_add_dependency_and_pin() throws Exception {
        prepareTestEnvironment("002");

        runGraphTool("add-dependency", List.of("--dependency", "commons-collections:commons-collections:3.2.2"), false);
        assertOutputLogged(
            "Added dependency 'commons-collections:commons-collections:jar:_COMMONS_COLLECTIONS_VERSION' to dependencies collection");

        runGraphTool("pin-dependencies", List.of(), false);
        assertOutputLogged(" + commons-collections:commons-collections:jar:3.2.2");

        // this should be the last check (so we can re-use the generated output if needed)
        assertTestOutputContainsExpectedContent();
    }

    @Test
    public final void _003_update_version_variable_and_pin() throws Exception {
        prepareTestEnvironment("003");

        runGraphTool(
            "update-version-variable",
            List.of("--variable-name", "commons.collections.version", "--new-version", "3.2.2"),
            false);
        assertOutputLogged("Updated version variable '_COMMONS_COLLECTIONS_VERSION': '3.2.1' -> '3.2.2'");
        assertOutputLogged(" - commons-collections:commons-collections:jar");
        assertOutputLogged("third_party/dependencies/commons_collections.bzl");

        runGraphTool("pin-dependencies", List.of(), false);
        assertOutputLogged(
            " * commons-collections:commons-collections:jar (3.2.1 -> 3.2.2, artifact: commons-collections:commons-collections:jar:3.2.1 <> commons-collections:commons-collections:jar:3.2.2, artifact_sha1: fixme <> 8ad72fe39fa8c91eaaf12aadb21e0c3661fe26d5, artifact_sha256: fixme <> eeeae917917144a68a741d4c0dff66aa5c5c5fd85593ff217bced3fc8ca783b8, sources_artifact: commons-collections:commons-collections:jar:sources:3.2.1 <> commons-collections:commons-collections:jar:sources:3.2.2, sources_sha1: fixme <> 78c50ebda5784937ca1615fc0e1d0cb35857d572, sources_sha256: fixme <> a5b5ee16a02edadf7fe637f250217c19878bc6134f15eb55635c48996f6fed1d)");

        // this should be the last check (so we can re-use the generated output if needed)
        assertTestOutputContainsExpectedContent();
    }

    @Test
    public final void _004_add_dependency_and_pin_with_empty_core_packages_json() throws Exception {
        prepareTestEnvironment("004");

        runGraphTool("add-dependency", List.of("--dependency", "commons-collections:commons-collections:3.2.2"), false);
        assertOutputLogged(
            "Added dependency 'commons-collections:commons-collections:jar:_COMMONS_COLLECTIONS_VERSION' to dependencies collection");

        runGraphTool("pin-dependencies", List.of(), false);
        assertOutputLogged(" + commons-collections:commons-collections:jar:3.2.2");

        // this should be the last check (so we can re-use the generated output if needed)
        assertTestOutputContainsExpectedContent();
    }

    @Test
    public final void _005_add_dependency_and_pin__and_show_with_empty_core_packages_json() throws Exception {
        prepareTestEnvironment("005");

        runGraphTool("add-dependency", List.of("--dependency", "commons-collections:commons-collections:3.2.2"), false);
        assertOutputLogged(
            "Added dependency 'commons-collections:commons-collections:jar:_COMMONS_COLLECTIONS_VERSION' to dependencies collection");

        runGraphTool("pin-dependencies", List.of(), false);
        assertOutputLogged(" + commons-collections:commons-collections:jar:3.2.2");

        runGraphTool("dependency-info", List.of("collections"), false);
        assertOutputLogged("Found: commons-collections:commons-collections:jar (best match)");

        // this should be the last check (so we can re-use the generated output if needed)
        assertTestOutputContainsExpectedContent();
    }

    private void assertOutputLogged(String text) {
        assertTrue(
            logLines.parallelStream().anyMatch(l -> l.contains(text)),
            () -> format("Expected log output not found in command output: %s", text));
    }

    /**
     * Checks if the produced test output contains the expected output.
     * <p>
     * Note, the test output is allowed to contain additional files (eg., debug output).
     * </p>
     *
     * @throws IOException
     */
    private void assertTestOutputContainsExpectedContent() throws IOException {
        var expected = testDirectory.resolve("expected");
        walkFileTree(expected, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                var expectedDirectory = expected.relativize(dir);
                assertTrue(
                    isDirectory(testWorkingDirectory.resolve(expectedDirectory)),
                    format(
                        "Expected directory '%s' not found in test result '%s'!",
                        expectedDirectory,
                        testWorkingDirectory));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                var expectedFile = expected.relativize(file);
                assertTrue(
                    isRegularFile(testWorkingDirectory.resolve(expectedFile)),
                    format("Expected file '%s' not found in test result '%s'!", expectedFile, testWorkingDirectory));

                assertEquals(
                    readString(file),
                    readString(testWorkingDirectory.resolve(expectedFile)),
                    format("File content mismatch for '%s'", expectedFile));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    void prepareTestEnvironment(String itTestName) throws InterruptedException, IOException {
        testDirectory = workingDirectory.resolve("src/test/it").resolve(itTestName);
        assertTrue(isDirectory(testDirectory), format("Test directory '%s' not found!", testDirectory));

        // create a working dir for the test
        testWorkingDirectory = targetDirectory.resolve("it").resolve(itTestName);
        if (isDirectory(testWorkingDirectory)) {
            assertTrue(
                new ProcessBuilder("rm", "-rf", testWorkingDirectory.toString()).inheritIO().start().waitFor() == 0,
                "Deleting test target folder failed!");
        }
        createDirectories(testWorkingDirectory);

        // clone the source
        var source = testDirectory.resolve("source");
        walkFileTree(source, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(testWorkingDirectory.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, testWorkingDirectory.resolve(source.relativize(file)));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void runGraphTool(String subCommand, List<String> args, boolean allowFail) throws Exception {
        List<String> command = new ArrayList<>();

        command.add(javaExecutable.toString());
        command.add("-jar");
        command.add(jarWithDependenciesFile.toString());
        command.add(subCommand);
        command.add("--batch-mode");
        command.add("--verbose");
        command.add("--no-vuln-check");
        command.add(testWorkingDirectory.toString());
        command.addAll(args);

        logFile = testWorkingDirectory.resolve(
            format(
                "out__%s__%s.log",
                subCommand,
                args.stream().map(BazelConventions::toTargetName).collect(joining("--"))));

        var builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.redirectOutput(logFile.toFile());
        builder.directory(workingDirectory.toFile());

        var process = builder.start();
        var exitCode = process.waitFor();

        if (!allowFail) {
            assertEquals(
                0,
                exitCode,
                () -> format("Graph tool execution failed! Check log at: %s%n%n%s", logFile, tryRead(logFile)));
        }

        logLines = readAllLines(logFile);
    }
}
