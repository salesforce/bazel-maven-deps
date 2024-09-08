package com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

/**
 * Customized version of {@link org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator}, which skips conflicts
 */
public class MavenDepsNodeListGenerator implements DependencyVisitor {

    /**
     * @return a {@link DependencyFilter} which skips nodes in the graph that has been replaced due to a conflict
     */
    public static DependencyFilter skipReplacedNodes() {
        return (node, parents) -> !node.getData().containsKey(ConflictResolver.NODE_DATA_WINNER);
    }

    private final Map<DependencyNode, Object> visitedNodes;
    private final List<DependencyNode> nodes;

    public MavenDepsNodeListGenerator() {
        nodes = new ArrayList<>(128);
        visitedNodes = new IdentityHashMap<>(512);
    }

    /**
     * Gets the artifacts associated with the list of dependency nodes generated during the graph traversal.
     *
     * @param includeUnresolved
     *            Whether unresolved artifacts shall be included in the result or not.
     * @param filter
     *            a mandatory filter for further customization of the result
     * @return The list of artifacts, never {@code null}.
     */
    public List<Artifact> getArtifacts(boolean includeUnresolved, DependencyFilter filter) {
        final List<Artifact> artifacts = new ArrayList<>(getNodes().size());

        for (final DependencyNode node : getNodes()) {
            if ((node.getDependency() != null) && filter.accept(node, Collections.emptyList())) {
                final var artifact = node.getDependency().getArtifact();
                if (includeUnresolved || (artifact.getFile() != null)) {
                    artifacts.add(artifact);
                }
            }
        }

        return artifacts;
    }

    /**
     * Gets a class path by concatenating the artifact files of the visited dependency nodes. Nodes with unresolved
     * artifacts are automatically skipped.
     *
     * @return The class path, using the platform-specific path separator, never {@code null}.
     */
    public String getClassPath() {
        final var buffer = new StringBuilder(1024);

        for (final var it = getNodes().iterator(); it.hasNext();) {
            final var node = it.next();
            if (node.getDependency() != null) {
                final var artifact = node.getDependency().getArtifact();
                if (artifact.getFile() != null) {
                    buffer.append(artifact.getFile().getAbsolutePath());
                    if (it.hasNext()) {
                        buffer.append(File.pathSeparatorChar);
                    }
                }
            }
        }

        return buffer.toString();
    }

    /**
     * Gets the dependencies seen during the graph traversal.
     *
     * @param includeUnresolved
     *            Whether unresolved dependencies shall be included in the result or not.
     * @return The list of dependencies, never {@code null}.
     */
    public List<Dependency> getDependencies(boolean includeUnresolved) {
        final List<Dependency> dependencies = new ArrayList<>(getNodes().size());

        for (final DependencyNode node : getNodes()) {
            final var dependency = node.getDependency();
            if ((dependency != null) && (includeUnresolved || (dependency.getArtifact().getFile() != null))) {
                dependencies.add(dependency);
            }
        }

        return dependencies;
    }

    /**
     * Gets the files of resolved artifacts seen during the graph traversal.
     *
     * @return The list of artifact files, never {@code null}.
     */
    public List<File> getFiles() {
        final List<File> files = new ArrayList<>(getNodes().size());

        for (final DependencyNode node : getNodes()) {
            if (node.getDependency() != null) {
                final var file = node.getDependency().getArtifact().getFile();
                if (file != null) {
                    files.add(file);
                }
            }
        }

        return files;
    }

    /**
     * Gets the list of dependency nodes that was generated during the graph traversal.
     *
     * @return The list of dependency nodes, never {@code null}.
     */
    public List<DependencyNode> getNodes() {
        return nodes;
    }

    /**
     * Marks the specified node as being visited and determines whether the node has been visited before.
     *
     * @param node
     *            The node being visited, must not be {@code null}.
     * @return {@code true} if the node has not been visited before, {@code false} if the node was already visited.
     */
    protected boolean setVisited(DependencyNode node) {
        return visitedNodes.put(node, Boolean.TRUE) == null;
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        if (!setVisited(node)) {
            return false;
        }

        if (node.getDependency() != null) {
            nodes.add(node);
        }

        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        return true;
    }

}
