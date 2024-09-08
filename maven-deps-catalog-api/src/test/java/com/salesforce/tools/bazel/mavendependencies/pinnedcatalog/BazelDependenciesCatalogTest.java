package com.salesforce.tools.bazel.mavendependencies.pinnedcatalog;

import static com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelDependenciesCatalog.getRecommendedPreamble;
import static com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport.createForArtifact;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.salesforce.tools.bazel.mavendependencies.collection.CollectionDelta.Modification.Type;
import com.salesforce.tools.bazel.cli.helper.NullProgressMonitor;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.helper.NoScmWritableFilesystem;

public class BazelDependenciesCatalogTest {

    private static final String LOAD_SYMBOL = "@bazel_maven_deps//bazel:jvm.bzl";

    @TempDir
    Path tempDir;

    private void createCatalog(Stream<BazelJavaDependencyImport> of) throws IOException {
        var catalog = new BazelDependenciesCatalog(tempDir);
        catalog.load();
        catalog.replaceContent(of, false);
        catalog.save(
            Collections.emptySortedSet(),
            LOAD_SYMBOL,
            getRecommendedPreamble(),
            new NullProgressMonitor(),
            new NoScmWritableFilesystem());
    }

    private BazelDependenciesCatalog loadCatalog() throws IOException {
        var catalog = new BazelDependenciesCatalog(tempDir);
        catalog.load();
        return catalog;
    }

    @Test
    public void same_artifact_shows_as_no_change_in_delta() throws IOException {
        // create initial catalog
        createCatalog(Stream.of(createForArtifact(new MavenArtifact("test", "test", "1.0.0", "jar", null)).build()));

        var catalog = loadCatalog();
        var delta = catalog.replaceContent(
            Stream.of(createForArtifact(new MavenArtifact("test", "test", "1.0.0", "jar", null)).build()),
            false);

        assertThat(delta.modifications, hasSize(0));
    }

    @Test
    public void version_update_shows_as_change_in_delta() throws IOException {
        // create initial catalog
        createCatalog(Stream.of(createForArtifact(new MavenArtifact("test", "test", "1.0.0", "jar", null)).build()));

        var catalog = loadCatalog();
        var delta = catalog.replaceContent(
            Stream.of(createForArtifact(new MavenArtifact("test", "test", "1.0.1", "jar", null)).build()),
            false);

        assertThat(delta.modifications, hasSize(1));

        var modification = delta.modifications.first();
        assertEquals(Type.VERSION_UPDATE, modification.getTypeOfModification());
        assertThat(modification.getDetailedDiff(), hasSize(1));
        assertThat(modification.getDetailedDiff(), contains("artifact"));
    }
}
