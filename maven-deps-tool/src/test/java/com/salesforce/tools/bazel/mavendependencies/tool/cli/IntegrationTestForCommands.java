package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import com.salesforce.tools.bazel.cli.helper.NullProgressMonitor;
import com.salesforce.tools.bazel.cli.scm.NaiveScmTool;
import com.salesforce.tools.bazel.cli.scm.NaiveScmToolConverter;
import com.salesforce.tools.bazel.mavendependencies.collection.CollectionSaveResult;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenDependenciesCollection;
import com.salesforce.tools.bazel.mavendependencies.vulnerabilities.NoOpVulnerabilityScanner;

import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;

@ExtendWith(MockitoExtension.class)
public abstract class IntegrationTestForCommands<T extends BaseCommandUsingDependencyCollection> {

    private final Class<T> commandClass;

    @TempDir
    protected Path tempDir;

    private MessageCollector out;

    private MavenDependenciesCollection collectionAfterExecution;

    private CollectionSaveResult collectionSaveResult;

    private T command;

    public IntegrationTestForCommands(Class<T> commandClass) {
        this.commandClass = commandClass;
    }

    /**
     * @return the collection loaded directly after executing a command with {@link #executeCommand(String...)}
     */
    protected MavenDependenciesCollection assertCollection() {
        var colletion = collectionAfterExecution;
        assertNotNull(colletion, "no collectionAfterExecution; did you call executeCommand?");
        return colletion;
    }

    protected void assertCollectionContainsArtifact(MavenArtifact expected) {
        var actual = assertCollection().findArtifact(expected.toCoordinatesStringWithoutVersion());
        assertNotNull(
            actual,
            () -> format("No artifact for coordinates '%s' found!", expected.toCoordinatesStringWithoutVersion()));
        assertEquals(
            expected.getVersion(),
            actual.getVersion(),
            () -> format(
                "Expected to find artifact '%s' with same version but: %s <> %s",
                expected.toCoordinatesStringWithoutVersion(),
                expected.getVersion(),
                actual.getVersion()));
        assertEquals(
            expected,
            actual,
            () -> format(
                "Expected to find matching artifact '%s' but they don't: %s <> %s",
                expected.toCoordinatesStringWithoutVersion(),
                expected,
                actual));
    }

    protected void assertCollectionContainsVariable(String name, String value) {
        var actual = assertCollection().getVersionVariableValue(name);
        assertEquals(
            value,
            actual,
            () -> format(
                "Expected to find variable '%s' in collection with value '%s' but found '%s'",
                name,
                value,
                actual));
    }

    /**
     * @return the intercepted {@link CollectionSaveResult} when the collection was saved
     * @throws Exception
     */
    protected CollectionSaveResult assertCollectionSaveResult() throws Exception {
        var saveResult = collectionSaveResult;
        assertNotNull(
            saveResult,
            "no collectionSaveResult; looks like the collection was not saved when executing the command");
        verify(command, times(1)).saveStarlarkDependenciesFile(any());
        return saveResult;
    }

    protected void assertInfoMessageContains(String text) {
        out.assertInfo(text);
    }

    protected MessageCollector assertMessageCollector() {
        assertNotNull(out);
        return out;
    }

    protected void assertNoCollectionSaveResult() throws Exception {
        assertNull(collectionSaveResult, "Unexpected save of the collection when executing the command!");
        verify(command, never()).getScmTool();
        verify(command, never()).saveStarlarkDependenciesFile(any());
    }

    protected void assertOnlyOneModifiedFileForArtifactWithGroupId(MavenArtifact artifact) throws Exception {
        var collection = assertCollection();
        var group = collection.getGroup(artifact);
        var fileLocation = collection.getGroupFileLocation(group);

        var saveResult = assertCollectionSaveResult();
        assertThat(saveResult.writtenFiles, hasSize(1));
        assertThat(saveResult.writtenFiles, hasItem(fileLocation));

        assertThat(saveResult.deletedFiles, hasSize(0));
    }

    protected void executeCommand(String... args) throws Exception {
        List<String> arguments = new ArrayList<>(args.length + 1);

        arguments.add("--workspace-root=" + tempDir.toString());
        arguments.addAll(Arrays.asList(args));

        // setup command line
        var commandLine =
                new CommandLine(requireNonNull(command, "setUp not called?")).setUnmatchedArgumentsAllowed(false);

        // parse & execute manually (instead of using #execute) because we don't want to swallow any exceptions!
        var parseResult = commandLine.parseArgs(arguments.toArray(new String[arguments.size()]));

        try {
            commandLine.getExecutionStrategy().execute(parseResult);
        } catch (ExecutionException e) {
            // re-throw cause if possible (it's easier for writing tests)
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }

        collectionAfterExecution = loadCollection();
    }

    private MavenDependenciesCollection loadCollection() throws IOException {
        var dependenciesCollection = new MavenDependenciesCollection(tempDir);
        dependenciesCollection.load();
        return dependenciesCollection;
    }

    protected NullProgressMonitor noopProgressMonitor() {
        return new NullProgressMonitor();
    }

    protected NaiveScmTool noopScmTool() throws Exception {
        return new NaiveScmToolConverter().convert("noop");
    }

    protected NoOpVulnerabilityScanner noopVulnerabilityScanner() throws Exception {
        return new NoOpVulnerabilityScanner(out);
    }

    @BeforeEach
    protected void setUp() throws Exception {
        // create initial collection
        var dependenciesCollection = loadCollection();
        setupNewCollection(dependenciesCollection);
        dependenciesCollection.save(
            "@bazel_maven_deps//bazel:defs.bzl",
            null /* no preamble */,
            false,
            false,
            noopProgressMonitor(),
            noopScmTool());

        // setup stubs for test
        out = new MessageCollector();
        command = setupCommandInstance();
    }

    private T setupCommandInstance() throws Exception {
        // FIXME: lenient because of https://github.com/mockito/mockito/issues/2649
        var command = mock(commandClass, withSettings().lenient().useConstructor().defaultAnswer(CALLS_REAL_METHODS));

        // use our custom message logger
        when(command.createMessagePrinter()).thenReturn(out);

        // use a no-op scm tool
        doReturn(noopScmTool()).when(command).getScmTool();

        // use no-op
        doReturn(noopVulnerabilityScanner()).when(command).getVulnerabilityScanner(any());

        // intercept save result
        doAnswer(invocation -> collectionSaveResult = (CollectionSaveResult) invocation.callRealMethod()).when(command)
                .saveStarlarkDependenciesFile(out);

        return command;
    }

    protected abstract void setupNewCollection(MavenDependenciesCollection dependenciesCollection);

}