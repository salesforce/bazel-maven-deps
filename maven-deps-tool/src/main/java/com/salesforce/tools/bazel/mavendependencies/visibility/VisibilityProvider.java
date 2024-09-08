package com.salesforce.tools.bazel.mavendependencies.visibility;

import java.util.Collection;
import java.util.SortedSet;

import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;

/**
 * A visibility provider is responsible for providing visibility for a third party dependency import.
 * <p>
 * This interface may be implemented by extensions to provide customized logic for determine the visibility of
 * individual imports.
 * </p>
 */
public interface VisibilityProvider {

    /**
     * Returns the visibility for the specified
     * {@link com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport dependency import}.
     * <p>
     * This method is invoked during dependency pinning to compute the visibility information. At that time the
     * {@link com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport} is not fully
     * assembled yet. Therefore the relevant information is passed in as parameters.
     * </p>
     *
     * @param name
     *            the
     *            {@link com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport#getName()
     *            dependency import name}
     * @param mavenArtifact
     *            the
     *            {@link com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport#getArtifact()
     *            Maven artifact}
     * @param tags
     *            the
     *            {@link com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport#getTags()
     *            tags}
     * @param rdepsProvider
     *            the {@link ReverseDependenciesProvider} for collecting direct reverse dependencies in case visibility
     *            should be limited to those
     *
     * @return the computed visibility (never <code>null</code>, use empty for "default", does not need to be sorted)
     */
    Collection<String> getVisibility(
            String name,
            MavenArtifact mavenArtifact,
            SortedSet<String> tags,
            ReverseDependenciesProvider rdepsProvider);

}
