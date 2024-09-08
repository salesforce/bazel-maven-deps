package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenDependenciesCollection;

public class GetVersionVariableValueCommandTest extends IntegrationTestForCommands<GetVersionVariableValueCommand> {

    private static final String ARTIFACT_WITH_VARIABLE = "artifact-with-variable";

    public GetVersionVariableValueCommandTest() {
        super(GetVersionVariableValueCommand.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "test", "test.version", "test-version", "TEST", "TEST-VERSION", "TEST.VERSION", "_TEST",
            "_TEST_VERSION" })
    public final void get_version_variable_multiple_spellings(String variableName) throws Exception {
        executeCommand("--variable-name=" + variableName);

        assertInfoMessageContains("1.0.0");
        assertNoCollectionSaveResult();
    }

    @Override
    protected void setupNewCollection(MavenDependenciesCollection dependenciesCollection) {
        dependenciesCollection.addDependencyWithManagedVersion(
            new MavenArtifact("test", ARTIFACT_WITH_VARIABLE, "_TEST_VERSION", "jar", null),
            "_TEST_VERSION",
            "1.0.0");
    }
}
