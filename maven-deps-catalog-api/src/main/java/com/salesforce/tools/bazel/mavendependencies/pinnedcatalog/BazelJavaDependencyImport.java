package com.salesforce.tools.bazel.mavendependencies.pinnedcatalog;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.starlark.BazelConventions;

/**
 * An entry of a {@link BazelCatalogFile} for importing a Java dependency from Maven coordinates.
 * <p>
 * Note, this implementation enforces some conventions (eg., sorting of field) to ensure better behavior when the output
 * is put in SCM.
 * </p>
 * <p>
 * A {@link BazelJavaDependencyImport} is immutable. The {@link #createForArtifact(MavenArtifact) builder} should be
 * used to create new objects.
 * </p>
 */
public final class BazelJavaDependencyImport implements Comparable<BazelJavaDependencyImport> {

    public static final class BazelJavaDependencyImportBuilder {

        private static SortedSet<String> emptyToNull(SortedSet<String> set) {
            if ((set == null) || set.isEmpty()) {
                return null;
            }
            return set;
        }

        private static SortedSet<String> toSortedModifiableCopyOrNull(Collection<String> input) {
            return input != null ? new TreeSet<>(input) : null;
        }

        private final String name;
        private final MavenArtifact artifact;

        private String artifactSha256, artifactSha1;
        private MavenArtifact sourcesArtifact;

        private String sourcesArtifactSha256, sourcesArtifactSha1;
        private SortedSet<String> licenses;

        private SortedSet<String> deps, runtimeDeps, exports;
        private boolean testonly, neverlink;
        private SortedSet<String> defaultVisibility;

        private SortedSet<String> tags;

        private String extraBuildFileContent;

        private BazelJavaDependencyImportBuilder(String name, MavenArtifact artifact) {
            this.name = name;
            this.artifact = artifact;
        }

        public BazelJavaDependencyImportBuilder addTag(String tag) {
            if (tags == null) {
                tags = new TreeSet<>();
            }
            this.tags.add(tag);
            return this;
        }

        public BazelJavaDependencyImport build() {
            return new BazelJavaDependencyImport(
                    name,
                    artifact,
                    artifactSha256,
                    artifactSha1,
                    sourcesArtifact,
                    sourcesArtifactSha256,
                    sourcesArtifactSha1,
                    emptyToNull(licenses),
                    emptyToNull(deps),
                    emptyToNull(runtimeDeps),
                    emptyToNull(exports),
                    testonly,
                    neverlink,
                    emptyToNull(defaultVisibility),
                    emptyToNull(tags),
                    extraBuildFileContent);
        }

        public MavenArtifact getArtifact() {
            return artifact;
        }

        public String getArtifactSha1() {
            return artifactSha1;
        }

        public String getArtifactSha256() {
            return artifactSha256;
        }

        public SortedSet<String> getDeps() {
            if (deps == null) {
                deps = new TreeSet<>();
            }
            return deps;
        }

        public String getName() {
            return name;
        }

        public SortedSet<String> getRuntimeDeps() {
            if (runtimeDeps == null) {
                runtimeDeps = new TreeSet<>();
            }
            return runtimeDeps;
        }

        public MavenArtifact getSourcesArtifact() {
            return sourcesArtifact;
        }

        public String getSourcesArtifactSha1() {
            return sourcesArtifactSha1;
        }

        public String getSourcesArtifactSha256() {
            return sourcesArtifactSha256;
        }

        public SortedSet<String> getTags() {
            if (this.tags == null) {
                this.tags = new TreeSet<>();
            }
            return this.tags;
        }

        public boolean hasSourcesArtifact() {
            return sourcesArtifact != null;
        }

        public BazelJavaDependencyImportBuilder removeTag(String tag) {
            if (tags != null) {
                this.tags.remove(tag);
            }
            return this;
        }

        public BazelJavaDependencyImportBuilder setArtifactSha1(String artifactSha1) {
            this.artifactSha1 = artifactSha1;
            return this;
        }

        public BazelJavaDependencyImportBuilder setArtifactSha256(String artifactSha256) {
            this.artifactSha256 = artifactSha256;
            return this;
        }

        public BazelJavaDependencyImportBuilder setDefaultVisibility(Collection<String> defaultVisibility) {
            this.defaultVisibility = toSortedModifiableCopyOrNull(defaultVisibility);
            return this;
        }

        public BazelJavaDependencyImportBuilder setDeps(Collection<String> deps) {
            this.deps = toSortedModifiableCopyOrNull(deps);
            return this;
        }

        public BazelJavaDependencyImportBuilder setExports(Collection<String> exports) {
            this.exports = toSortedModifiableCopyOrNull(exports);
            return this;
        }

        public BazelJavaDependencyImportBuilder setExtraBuildFileContent(String extraBuildFileContent) {
            this.extraBuildFileContent = extraBuildFileContent;
            return this;
        }

        public BazelJavaDependencyImportBuilder setLicenses(Collection<String> licenses) {
            this.licenses = toSortedModifiableCopyOrNull(licenses);
            return this;
        }

        public BazelJavaDependencyImportBuilder setNeverlink(boolean neverlink) {
            this.neverlink = neverlink;
            return this;
        }

        public BazelJavaDependencyImportBuilder setRuntimeDeps(Collection<String> runtimeDeps) {
            this.runtimeDeps = toSortedModifiableCopyOrNull(runtimeDeps);
            return this;
        }

        public BazelJavaDependencyImportBuilder setSourcesArtifact(boolean generateSourcesArtifact) {
            if (generateSourcesArtifact) {
                sourcesArtifact = requireNonNull(
                    artifact.toSourcesArtifact(),
                    () -> format(
                        "Artifact '%s' does not have a corresponding sources artifact!",
                        artifact.toCoordinatesString()));
            } else {
                sourcesArtifact = null;
            }
            return this;
        }

        public BazelJavaDependencyImportBuilder setSourcesArtifactSha1(String sourcesArtifactSha1) {
            this.sourcesArtifactSha1 = sourcesArtifactSha1;
            return this;
        }

        public BazelJavaDependencyImportBuilder setSourcesArtifactSha256(String sourcesArtifactSha256) {
            this.sourcesArtifactSha256 = sourcesArtifactSha256;
            return this;
        }

        public BazelJavaDependencyImportBuilder setTags(Collection<String> tags) {
            this.tags = toSortedModifiableCopyOrNull(tags);
            return this;
        }

        public BazelJavaDependencyImportBuilder setTestonly(boolean testonly) {
            this.testonly = testonly;
            return this;
        }
    }

    /**
     * A tag to be used to indicate that the import is transient, i.e. not directly declared in the dependency
     * collection.
     */
    public static final String TAG_NOT_IN_COLLECTION = "not_in_collection";

    /**
     * Creates a new builder for the specified artifact.
     * <p>
     * The name will be computed using {@link BazelConventions#toTargetName(String, String, String, String)}.
     * </p>
     *
     * @param artifact
     *            the {@link MavenArtifact}
     * @return a new builder
     */
    public static BazelJavaDependencyImportBuilder createForArtifact(MavenArtifact artifact) {
        return new BazelJavaDependencyImportBuilder(
                BazelConventions.toTargetName(
                    artifact.getGroupId(),
                    artifact.getArtifactId(),
                    artifact.getPackaging(),
                    artifact.getClassifier()),
                artifact);
    }

    /**
     * Creates a new builder for the specified artifact.
     * <p>
     * The name will be computed using {@link BazelConventions#toTargetName(String, String, String, String)}.
     * </p>
     *
     * @param name
     *            the name to use
     * @param artifact
     *            the {@link MavenArtifact}
     * @return a new builder
     */
    public static BazelJavaDependencyImportBuilder createWithNameAndArtifact(String name, MavenArtifact artifact) {
        return new BazelJavaDependencyImportBuilder(name, artifact);
    }

    private final String name;
    private final MavenArtifact artifact;
    private final String artifactSha256, artifactSha1;

    private final MavenArtifact sourcesArtifact;
    private final String sourcesArtifactSha256, sourcesArtifactSha1;

    private final SortedSet<String> licenses;
    private final SortedSet<String> deps, runtimeDeps, exports;

    private final boolean testonly, neverlink;
    private final SortedSet<String> defaultVisibility;
    private final SortedSet<String> tags;

    private final String extraBuildFileContent;

    BazelJavaDependencyImport(String name, MavenArtifact artifact, String artifactSha256, String artifactSha1,
            MavenArtifact sourcesArtifact, String sourcesArtifactSha256, String sourcesArtifactSha1,
            SortedSet<String> licenses, SortedSet<String> deps, SortedSet<String> runtimeDeps,
            SortedSet<String> exports, boolean testonly, boolean neverlink, SortedSet<String> defaultVisibility,
            SortedSet<String> tags, String extraBuildFileContent) {
        this.name = name;
        this.artifact = artifact;
        this.artifactSha256 = artifactSha256;
        this.artifactSha1 = artifactSha1;
        this.sourcesArtifact = sourcesArtifact;
        this.sourcesArtifactSha256 = sourcesArtifactSha256;
        this.sourcesArtifactSha1 = sourcesArtifactSha1;
        this.licenses = licenses;
        this.deps = deps;
        this.runtimeDeps = runtimeDeps;
        this.exports = exports;
        this.testonly = testonly;
        this.neverlink = neverlink;
        this.defaultVisibility = defaultVisibility;
        this.tags = tags;
        this.extraBuildFileContent = extraBuildFileContent;
    }

    @Override
    public int compareTo(BazelJavaDependencyImport o) {
        // name is the natural order
        return name.compareTo(o.name);
    }

    public SortedSet<String> diff(BazelJavaDependencyImport other, boolean includeContent) {
        SortedSet<String> result = new TreeSet<>();
        if (!Objects.equals(artifact, other.artifact)) {
            if (includeContent) {
                result.add(format("artifact: %s <> %s", artifact, other.artifact));
            } else {
                result.add("artifact");
            }

        }
        if (!Objects.equals(artifactSha1, other.artifactSha1)) {
            if (includeContent) {
                result.add(format("artifact_sha1: %s <> %s", artifactSha1, other.artifactSha1));
            } else {
                result.add("artifact_sha1");
            }
        }
        if (!Objects.equals(artifactSha256, other.artifactSha256)) {
            if (includeContent) {
                result.add(format("artifact_sha256: %s <> %s", artifactSha256, other.artifactSha256));
            } else {
                result.add("artifact_sha256");
            }
        }
        if (!Objects.equals(defaultVisibility, other.defaultVisibility)) {
            if (includeContent) {
                result.add(format("default_visibility: %s <> %s", defaultVisibility, other.defaultVisibility));
            } else {
                result.add("default_visibility");
            }
        }
        if (!Objects.equals(deps, other.deps)) {
            if (includeContent) {
                result.add(format("deps: %s <> %s", deps, other.deps));
            } else {
                result.add("deps");
            }
        }
        if (!Objects.equals(exports, other.exports)) {
            if (includeContent) {
                result.add(format("exports: %s <> %s", exports, other.exports));
            } else {
                result.add("exports");
            }

        }
        if (!Objects.equals(extraBuildFileContent, other.extraBuildFileContent)) {
            if (includeContent) {
                result.add(
                    format("extra_buildfile_content: %s <> %s", extraBuildFileContent, other.extraBuildFileContent));
            } else {
                result.add("extra_buildfile_content");
            }

        }
        if (!Objects.equals(licenses, other.licenses)) {
            if (includeContent) {
                result.add(format("licenses: %s <> %s", licenses, other.licenses));
            } else {
                result.add("licenses");
            }

        }
        if (!Objects.equals(name, other.name)) {
            if (includeContent) {
                result.add(format("name: %s <> %s", name, other.name));
            } else {
                result.add("name");
            }

        }
        if (!Objects.equals(neverlink, other.neverlink)) {
            if (includeContent) {
                result.add(format("neverlink: %s <> %s", neverlink, other.neverlink));
            } else {
                result.add("neverlink");
            }

        }
        if (!Objects.equals(runtimeDeps, other.runtimeDeps)) {
            if (includeContent) {
                result.add(format("runtime_deps: %s <> %s", runtimeDeps, other.runtimeDeps));
            } else {
                result.add("runtime_deps");
            }
        }
        if (!Objects.equals(sourcesArtifact, other.sourcesArtifact)) {
            if (includeContent) {
                result.add(
                    format(
                        "sources_artifact: %s <> %s",
                        sourcesArtifact != null ? sourcesArtifact.toCoordinatesString() : null,
                        other.sourcesArtifact != null ? other.sourcesArtifact.toCoordinatesString() : null));
            } else {
                result.add("sources_artifact");
            }
        }
        if (!Objects.equals(sourcesArtifactSha1, other.sourcesArtifactSha1)) {
            if (includeContent) {
                result.add(format("sources_sha1: %s <> %s", sourcesArtifactSha1, other.sourcesArtifactSha1));
            } else {
                result.add("sources_sha1");
            }
        }
        if (!Objects.equals(sourcesArtifactSha256, other.sourcesArtifactSha256)) {
            if (includeContent) {
                result.add(format("sources_sha256: %s <> %s", sourcesArtifactSha256, other.sourcesArtifactSha256));
            } else {
                result.add("sources_sha256");
            }
        }
        if (!Objects.equals(tags, other.tags)) {
            if (includeContent) {
                result.add(format("tags: %s <> %s", tags, other.tags));
            } else {
                result.add("tags");
            }
        }
        if (!Objects.equals(testonly, other.testonly)) {
            if (includeContent) {
                result.add(format("testonly: %s <> %s", testonly, other.testonly));
            } else {
                result.add("testonly");
            }

        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        var other = (BazelJavaDependencyImport) obj;
        return Objects.equals(artifact, other.artifact) && Objects.equals(artifactSha1, other.artifactSha1)
                && Objects.equals(artifactSha256, other.artifactSha256)
                && Objects.equals(defaultVisibility, other.defaultVisibility) && Objects.equals(deps, other.deps)
                && Objects.equals(exports, other.exports)
                && Objects.equals(extraBuildFileContent, other.extraBuildFileContent)
                && Objects.equals(licenses, other.licenses) && Objects.equals(name, other.name)
                && (neverlink == other.neverlink) && Objects.equals(runtimeDeps, other.runtimeDeps)
                && Objects.equals(sourcesArtifact, other.sourcesArtifact)
                && Objects.equals(sourcesArtifactSha1, other.sourcesArtifactSha1)
                && Objects.equals(sourcesArtifactSha256, other.sourcesArtifactSha256)
                && Objects.equals(tags, other.tags) && (testonly == other.testonly);
    }

    public MavenArtifact getArtifact() {
        return artifact;
    }

    public String getArtifactSha1() {
        return artifactSha1;
    }

    public String getArtifactSha256() {
        return artifactSha256;
    }

    public SortedSet<String> getDefaultVisibility() {
        return defaultVisibility;
    }

    public SortedSet<String> getDeps() {
        return deps;
    }

    public SortedSet<String> getExports() {
        return exports;
    }

    public String getExtraBuildFileContent() {
        return extraBuildFileContent;
    }

    public SortedSet<String> getLicenses() {
        return licenses;
    }

    public String getName() {
        return name;
    }

    public SortedSet<String> getRuntimeDeps() {
        return runtimeDeps;
    }

    public MavenArtifact getSourcesArtifact() {
        return sourcesArtifact;
    }

    public String getSourcesArtifactSha1() {
        return sourcesArtifactSha1;
    }

    public String getSourcesArtifactSha256() {
        return sourcesArtifactSha256;
    }

    public SortedSet<String> getTags() {
        return tags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            artifact,
            artifactSha1,
            artifactSha256,
            defaultVisibility,
            deps,
            exports,
            extraBuildFileContent,
            licenses,
            name,
            neverlink,
            runtimeDeps,
            sourcesArtifact,
            sourcesArtifactSha1,
            sourcesArtifactSha256,
            tags,
            testonly);
    }

    public boolean hasSourcesArtifact() {
        return sourcesArtifact != null;
    }

    public boolean isNeverlink() {
        return neverlink;
    }

    public boolean isTestonly() {
        return testonly;
    }

    @Override
    public String toString() {
        return "BazelJavaDependencyImport [" + name + "]";
    }
}
