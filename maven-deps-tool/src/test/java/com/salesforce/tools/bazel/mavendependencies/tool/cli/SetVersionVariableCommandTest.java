package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenDependenciesCollection;

public class SetVersionVariableCommandTest extends IntegrationTestForCommands<SetVersionVariableCommand> {

    private static final String ARTIFACT_WITH_VARIABLE = "artifact-with-variable";

    public SetVersionVariableCommandTest() {
        super(SetVersionVariableCommand.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "testv" })
    public final void no_update_of_version_variable_invalid_spellings(String variableName) throws Exception {

        var e = assertThrows(
            IllegalStateException.class,
            () -> executeCommand("--variable-name=" + variableName, "--new-version=1.0.1"));

        assertThat(e.getMessage(), startsWith("Version variable '"));
        assertThat(e.getMessage(), endsWith("' not found in dependencies collection!"));
        assertNoCollectionSaveResult();
    }

    @Test
    public final void noop_update_of_version_variable_does_not_change_collection() throws Exception {
        executeCommand("--variable-name=test", "--new-version=1.0.0");

        assertInfoMessageContains(
            "No update of version variable '_TEST_VERSION' necessary. Existing value already matches new value: '1.0.0' == '1.0.0'");
        assertNoCollectionSaveResult();
    }

    @Override
    protected void setupNewCollection(MavenDependenciesCollection dependenciesCollection) {
        dependenciesCollection.addDependencyWithManagedVersion(
            new MavenArtifact("test", ARTIFACT_WITH_VARIABLE, "_TEST_VERSION", "jar", null),
            "_TEST_VERSION",
            "1.0.0");
    }

    @ParameterizedTest
    @ValueSource(strings = { "test", "test.version", "test-version", "TEST", "TEST-VERSION", "TEST.VERSION", "_TEST",
            "_TEST_VERSION" })
    public final void update_of_version_variable_multiple_spellings(String variableName) throws Exception {
        executeCommand("--variable-name=" + variableName, "--new-version=1.0.1");

        assertCollectionContainsVariable("_TEST_VERSION", "1.0.1");
        assertOnlyOneModifiedFileForArtifactWithGroupId(
            new MavenArtifact("test", ARTIFACT_WITH_VARIABLE, "_TEST_VERSION", "jar", null));
    }
}
