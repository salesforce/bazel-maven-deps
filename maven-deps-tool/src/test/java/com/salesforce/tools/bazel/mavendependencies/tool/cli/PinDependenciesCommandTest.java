package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport;

public class PinDependenciesCommandTest {

    static void assertValuesCopied(BazelJavaDependencyImport existing, BazelJavaDependencyImport result) {
        assertEquals(existing.getArtifact(), result.getArtifact());
        assertEquals(existing.hasSourcesArtifact(), result.hasSourcesArtifact());
        assertEquals(existing.getDeps(), result.getDeps());
        assertEquals(existing.getRuntimeDeps(), result.getRuntimeDeps());
        assertEquals(existing.getDefaultVisibility(), result.getDefaultVisibility());
        assertEquals(existing.getLicenses(), result.getLicenses());
        assertEquals(existing.isNeverlink(), result.isNeverlink());
        assertEquals(existing.isTestonly(), result.isTestonly());
        assertEquals(existing.getExtraBuildFileContent(), result.getExtraBuildFileContent());
    }

    @Test
    public void createNewFromExistingAndReplacingArtifact_resets_hashes() {
        var artifact = new MavenArtifact("test", "test", "1.0.0", "jar", null);
        var existing = BazelJavaDependencyImport.createWithNameAndArtifact("test", artifact)
                .setArtifactSha1("sha1")
                .setArtifactSha256("sha256")
                .setSourcesArtifact(true)
                .setSourcesArtifactSha1("sources-sha1")
                .setSourcesArtifactSha256("sources-sha256")
                .setDeps(List.of("dep1", "dep2"))
                .setRuntimeDeps(List.of("rdep1", "rdep2"))
                .setExports(List.of("foo"))
                .setLicenses(List.of("lic"))
                .setNeverlink(true)
                .setTestonly(false)
                .setExtraBuildFileContent(
                    format("# %s%ngenrule(name=\"test\") # test%n%n", "Comment " + System.nanoTime()))
                .build();

        var result =
                PinDependenciesCommand.createNewFromExistingAndReplacingArtifact(existing, artifact, false).build();

        assertValuesCopied(existing, result);
        assertNull(result.getArtifactSha1());
        assertNull(result.getArtifactSha256());
        assertNull(result.getSourcesArtifactSha1());
        assertNull(result.getSourcesArtifactSha256());

        var resultWithKeep =
                PinDependenciesCommand.createNewFromExistingAndReplacingArtifact(existing, artifact, true).build();

        assertValuesCopied(existing, resultWithKeep);
        assertEquals(existing.getArtifactSha1(), resultWithKeep.getArtifactSha1());
        assertEquals(existing.getArtifactSha256(), resultWithKeep.getArtifactSha256());
        assertEquals(existing.getSourcesArtifactSha1(), resultWithKeep.getSourcesArtifactSha1());
        assertEquals(existing.getSourcesArtifactSha256(), resultWithKeep.getSourcesArtifactSha256());
    }

}
