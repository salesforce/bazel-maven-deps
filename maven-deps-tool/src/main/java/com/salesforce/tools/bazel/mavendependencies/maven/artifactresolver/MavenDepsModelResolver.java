package com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver;

import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

/**
 * A model resolver to assist building of dependency POMs. This resolver gives priority to those repositories that have
 * been initially specified and repositories discovered in dependency POMs are recessively merged into the search chain.
 *
 */
@SuppressWarnings("deprecation")
class MavenDepsModelResolver implements ModelResolver {

    private static RepositoryPolicy convert(final org.apache.maven.model.RepositoryPolicy policy) {
        var enabled = true;
        var checksums = RepositoryPolicy.CHECKSUM_POLICY_WARN;
        var updates = RepositoryPolicy.UPDATE_POLICY_DAILY;

        if (policy != null) {
            enabled = policy.isEnabled();
            if (policy.getUpdatePolicy() != null) {
                updates = policy.getUpdatePolicy();
            }
            if (policy.getChecksumPolicy() != null) {
                checksums = policy.getChecksumPolicy();
            }
        }

        return new RepositoryPolicy(enabled, updates, checksums);
    }

    static RemoteRepository convert(final Repository repository) {
        final var builder =
                new RemoteRepository.Builder(repository.getId(), repository.getLayout(), repository.getUrl());
        builder.setSnapshotPolicy(convert(repository.getSnapshots()));
        builder.setReleasePolicy(convert(repository.getReleases()));
        return builder.build();
    }

    private final RepositorySystemSession session;

    private final String context;

    private List<org.eclipse.aether.repository.RemoteRepository> repositories;

    private final RepositorySystem repoSys;

    private final Set<String> repositoryIds;

    private MavenDepsModelResolver(final MavenDepsModelResolver original) {
        session = original.session;
        context = original.context;
        repoSys = original.repoSys;
        repositories = original.repositories;
        repositoryIds = new HashSet<>(original.repositoryIds);
    }

    MavenDepsModelResolver(final RepositorySystemSession session, final String context, final RepositorySystem repoSys,
            final List<RemoteRepository> repositories) {
        this.session = session;
        this.context = context;
        this.repoSys = repoSys;
        this.repositories = repositories;
        repositoryIds = repositories.stream().map(RemoteRepository::getId).collect(toCollection(HashSet::new));
    }

    @Override
    public void addRepository(final Repository repository) throws InvalidRepositoryException {
        if (session.isIgnoreArtifactDescriptorRepositories() || !repositoryIds.add(repository.getId())) {
            return;
        }

        final List<RemoteRepository> newRepositories = new ArrayList<>(repositories);
        newRepositories.add(convert(repository));

        repositories = repoSys.newResolutionRepositories(session, newRepositories);
    }

    @Override
    public void addRepository(final Repository repository, final boolean replace) throws InvalidRepositoryException {
        addRepository(repository);
    }

    // @Override
    // public Object getCacheKey() {
    // return ImmutableList.copyOf(repositories);
    // }

    @Override
    public ModelResolver newCopy() {
        return new MavenDepsModelResolver(this);
    }

    @Override
    public ModelSource resolveModel(final Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    @Override
    public ModelSource resolveModel(final Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public FileModelSource resolveModel(
            final String groupId,
            final String artifactId,
            final String version) throws UnresolvableModelException {
        Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);

        try {
            final var request = new ArtifactRequest(pomArtifact, repositories, context);
            pomArtifact = repoSys.resolveArtifact(session, request).getArtifact();
        } catch (final ArtifactResolutionException e) {
            throw new UnresolvableModelException(
                    "Failed to resolve POM for " + groupId + ":" + artifactId + ":" + version + " due to "
                            + e.getMessage(),
                    groupId,
                    artifactId,
                    version,
                    e);
        }

        final var pomFile = pomArtifact.getFile();

        return new FileModelSource(pomFile);
    }

}