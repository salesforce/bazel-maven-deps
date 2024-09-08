package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import org.junit.jupiter.api.Test;

import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenDependenciesCollection;

public class SetDependencyVersionCommandTest extends IntegrationTestForCommands<SetDependencyVersionCommand> {

    private static final String ARTIFACT = "artifact";
    private static final String ARTIFACT_WITH_VARIABLE = "artifact-with-variable";

    public SetDependencyVersionCommandTest() {
        super(SetDependencyVersionCommand.class);
    }

    @Test
    public final void noop_update_of_version_variable_does_not_change_catalog() throws Exception {
        executeCommand("--dependency=test:" + ARTIFACT_WITH_VARIABLE, "--new-version=1.0.0");

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
        dependenciesCollection.addDependency(new MavenArtifact("test", ARTIFACT, "1.0.2", "jar", null));
    }

    @Test
    public final void update_of_artifact_version() throws Exception {
        executeCommand("--dependency=test:" + ARTIFACT, "--new-version=0.0.1");

        var expectedArtifact = new MavenArtifact("test", ARTIFACT, "0.0.1", "jar", null);
        assertCollectionContainsArtifact(expectedArtifact);
        assertOnlyOneModifiedFileForArtifactWithGroupId(expectedArtifact);
    }

    @Test
    public final void update_of_version_variable() throws Exception {
        executeCommand("--dependency=test:" + ARTIFACT_WITH_VARIABLE, "--new-version=1.0.1");

        assertCollectionContainsVariable("_TEST_VERSION", "1.0.1");
        assertOnlyOneModifiedFileForArtifactWithGroupId(
            new MavenArtifact("test", ARTIFACT_WITH_VARIABLE, "_TEST_VERSION", "jar", null));
    }
}
