package com.salesforce.tools.bazel.mavendependencies.visibility;

import static com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport.TAG_NOT_IN_COLLECTION;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;

/**
 * A default implementation using <code>//visibility:private</code> for transient imports
 */
public class StrictDepsVisibilityProvider implements VisibilityProvider {

    private final boolean strictDeps;

    public StrictDepsVisibilityProvider(MessagePrinter out, boolean strictDeps) {
        this.strictDeps = strictDeps;
    }

    @Override
    public Collection<String> getVisibility(
            String name,
            MavenArtifact mavenArtifact,
            SortedSet<String> tags,
            ReverseDependenciesProvider rdepsProvider) {
        if (strictDeps && tags.contains(TAG_NOT_IN_COLLECTION)) {
            var directReverseDependencies = rdepsProvider.getDirectReverseDependencies(name);
            if (!directReverseDependencies.isEmpty()) {
                var visibility = new TreeSet<String>();
                for (String rdep : directReverseDependencies) {
                    visibility.add("@" + rdep + "//:__subpackages__"); // visible to all sub packages
                }
                return visibility;
            }

            return Set.of("//visibility:private");
        }

        return Collections.emptySet();
    }
}
