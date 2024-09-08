package com.salesforce.tools.bazel.mavendependencies.collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact.Exclusion;

public class MavenArtifactTest {

    @Test
    public void exclusions_not_equals() {
        assertNotEquals(
            new MavenArtifact("g1", "a1", "v1", "jar", null, List.of(new Exclusion("*", "*")), false, false).hashCode(),
            new MavenArtifact("g1", "a1", "v1", "jar", null, null, false, false).hashCode());
    }

    @Test
    public void testCompareTo() {
        var g1a1v1 = new MavenArtifact("g1", "a1", "v1", "jar", null, null, false, false);
        var g1a2v1 = new MavenArtifact("g1", "a2", "v1", "jar", null, null, false, false);
        var g1a2v2 = new MavenArtifact("g1", "a2", "v2", "jar", null, null, false, false);
        var g2a1v1 = new MavenArtifact("g2", "a1", "v1", "jar", null, null, false, false);

        SortedSet<MavenArtifact> sorted = new TreeSet<>(List.of(g1a2v1, g2a1v1, g1a2v2, g1a1v1));

        assertThat(sorted, contains(g1a1v1, g1a2v1, g1a2v2, g2a1v1));
    }

    @Test
    public void testEquals() {
        assertTrue(
            Objects.equals(
                new MavenArtifact("g1", "a1", "v1", "jar", null, null, false, false),
                new MavenArtifact("g1", "a1", "v1", "jar", null, null, false, false)));
        assertEquals(
            new MavenArtifact("g1", "a1", "v1", "jar", null, null, false, false).hashCode(),
            new MavenArtifact("g1", "a1", "v1", "jar", null, null, false, false).hashCode());
    }

    @Test
    public void testFromCoordinatesString() {
        assertEquals(
            new MavenArtifact("org.hsqldb", "hsqldb", "_HSQLDB_VERSION", "jar", "debug", null, false, false),
            MavenArtifact.fromCoordinatesString("org.hsqldb:hsqldb:jar:debug:_HSQLDB_VERSION"));
    }
}
