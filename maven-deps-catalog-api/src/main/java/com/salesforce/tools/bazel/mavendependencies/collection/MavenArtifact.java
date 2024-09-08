package com.salesforce.tools.bazel.mavendependencies.collection;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * An artifact addressable using Maven coordinates.
 * <p>
 * This artifact implements common Maven semantics for persisting and creating from a coordinates string. Additionally,
 * exclusions and other attributes are allowed which allow to tweak behavior of the Maven artifact when used in a
 * dependencies system.
 * </p>
 * <p>
 * A {@link MavenArtifact} is intentionally immutable.
 * </p>
 */
public final class MavenArtifact implements Comparable<MavenArtifact> {

    public static class Exclusion implements Comparable<Exclusion> {

        private static Comparator<Exclusion> exclusionCoordinatesComparator = Comparator //
                .comparing(Exclusion::getGroupId, nullSafeStringComparator) //
                .thenComparing(Exclusion::getArtifactId, nullSafeStringComparator);

        public static Exclusion fromCoordinatesString(String coordinates) throws IllegalArgumentException {
            var tokens = coordinates.split(":");
            if (tokens.length != 2) {
                throw new IllegalArgumentException(
                        format(
                            "Error parsing exclusion coordinates '%s'. Syntax is <groupId>:<artifactId>",
                            coordinates));
            }

            return new Exclusion(tokens[0], tokens[1]);
        }

        static boolean isWildcardOrEqualsValue(String pattern, String value) {
            return "*".equals(pattern) || pattern.equals(value);
        }

        private final String groupId;
        private final String artifactId;

        public Exclusion(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        @Override
        public int compareTo(Exclusion o) {
            return Objects.compare(this, o, exclusionCoordinatesComparator);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }
            var other = (Exclusion) obj;
            return Objects.equals(artifactId, other.artifactId) && Objects.equals(groupId, other.groupId);
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getGroupId() {
            return groupId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(artifactId, groupId);
        }

        boolean matches(String groupId, String artifactId) {
            return isWildcardOrEqualsValue(getGroupId(), groupId)
                    && isWildcardOrEqualsValue(getArtifactId(), artifactId);
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId;
        }
    }

    /**
     * The default packaging type as per Maven conventions
     */
    public static final String DEFAULT_PACKAGING = "jar";

    static Comparator<String> nullSafeStringComparator = Comparator.nullsFirst(String::compareTo);

    private static Comparator<MavenArtifact> artifactCoordinatesComparator = Comparator //
            .comparing(MavenArtifact::getGroupId, nullSafeStringComparator) //
            .thenComparing(MavenArtifact::getArtifactId, nullSafeStringComparator) //
            .thenComparing(MavenArtifact::getPackaging, nullSafeStringComparator) //
            .thenComparing(MavenArtifact::getClassifier, nullSafeStringComparator) //
            .thenComparing(MavenArtifact::getVersion, nullSafeStringComparator);

    private static final Pattern COORDINATE_PATTERN =
            Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");

    /**
     * Parses coordinates into a {@link MavenArtifact}.
     * <p>
     * The artifact will not have any exclusions or any other details set.
     * </p>
     *
     * @param coordinates
     *            coordinates as
     *            <code>&lt;groupId&gt;:&lt;artifactId&gt;[:&lt;packaging&gt;[:&lt;classifier&gt;]]:&lt;version&gt;</code>
     * @return
     * @throws IllegalArgumentException
     */
    public static MavenArtifact fromCoordinatesString(String coordinates) throws IllegalArgumentException {
        var m = COORDINATE_PATTERN.matcher(coordinates);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    format(
                        "Error parsing Maven coordinates '%s'. Syntax is <groupId>:<artifactId>[:<packaging>[:<classifier>]]:<version>",
                        coordinates));
        }

        // ensure default "jar" packaging is used when not specified (this is important for comparisons and maps)
        var packaging = m.group(4);
        if ((packaging == null) || packaging.isEmpty()) {
            packaging = DEFAULT_PACKAGING;
        }

        return new MavenArtifact(m.group(1), m.group(2), m.group(7), packaging, m.group(6), null, false, false);
    }

    private final String groupId;

    private final String artifactId;

    private final String version;
    private final String packaging;
    private final String classifier;
    private final SortedSet<Exclusion> exclusions;

    private final boolean neverlink;
    private final boolean testonly;

    public MavenArtifact(String groupId, String artifactId, String version, String packaging, String classifier) {
        this(groupId, artifactId, version, packaging, classifier, null, false, false);
    }

    public MavenArtifact(String groupId, String artifactId, String version, String packaging, String classifier,
            Collection<Exclusion> exclusions, boolean neverlink, boolean testonly) {
        this.groupId = requireNonNull(groupId);
        this.artifactId = requireNonNull(artifactId);
        this.version = requireNonNull(version);
        this.packaging = requireNonNull(packaging);
        this.classifier = classifier;
        this.exclusions = ((exclusions != null) && (exclusions.size() > 0))
                ? Collections.unmodifiableSortedSet(new TreeSet<>(exclusions)) : null;
        this.neverlink = neverlink;
        this.testonly = testonly;
    }

    private void appendCoordinatesWithoutVersion(StringBuilder coordinates) {
        // see #fromCoordinatesString for syntax
        // except: we always output packaging (as Maven does)
        coordinates.append(groupId).append(':').append(artifactId).append(':').append(packaging);
        if ((classifier != null) && !classifier.isBlank()) {
            coordinates.append(':').append(classifier);
        }
    }

    @Override
    public int compareTo(MavenArtifact o) {
        return Objects.compare(this, o, artifactCoordinatesComparator);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        var other = (MavenArtifact) obj;
        return Objects.equals(artifactId, other.artifactId) && Objects.equals(classifier, other.classifier)
                && Objects.equals(exclusions, other.exclusions) && Objects.equals(groupId, other.groupId)
                && (neverlink == other.neverlink) && Objects.equals(packaging, other.packaging)
                && (testonly == other.testonly) && Objects.equals(version, other.version);
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getClassifier() {
        return classifier;
    }

    public SortedSet<Exclusion> getExclusions() {
        return exclusions;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactId, classifier, exclusions, groupId, neverlink, packaging, testonly, version);
    }

    public boolean isJarPackaging() {
        return DEFAULT_PACKAGING.equals(packaging);
    }

    public boolean isNeverlink() {
        return neverlink;
    }

    public boolean isPomPackaging() {
        return "pom".equals(packaging);
    }

    public boolean isTestonly() {
        return testonly;
    }

    /**
     * @return the Maven coordinates of this {@link MavenArtifact} as specified by
     *         {@link #fromCoordinatesString(String)}
     */
    public String toCoordinatesString() {
        // see #fromCoordinatesString for syntax
        var coordinates = new StringBuilder();
        appendCoordinatesWithoutVersion(coordinates);
        coordinates.append(':').append(version);
        return coordinates.toString();
    }

    /**
     * Returns a Maven coordinates string without version info.
     *
     * @return the Maven coordinates of this {@link MavenArtifact} as
     *         <code>&lt;groupId&gt;:&lt;artifactId&gt;:&lt;packaging&gt;[:&lt;classifier&gt;]</code>
     */
    public String toCoordinatesStringWithoutVersion() {
        var coordinates = new StringBuilder();
        appendCoordinatesWithoutVersion(coordinates);
        return coordinates.toString();
    }

    public String toRelativePath() {
        // see
        // https://github.com/bazelbuild/bazel/blob/3fd0ffa93e479486a8b8fa68485b59d14b98836b/tools/build_defs/repo/jvm.bzl#L174
        var path = new StringBuilder();

        // path
        path.append(groupId.replace(".", "/")).append('/').append(artifactId).append('/').append(version).append('/');

        // file name
        path.append(artifactId).append('-').append(version);
        if (classifier != null) {
            path.append('-').append(classifier);
        }
        path.append('.').append(packaging);

        return path.toString();
    }

    /**
     * @return a corresponding sources artifact (based on {@link MavenConventions#getSourcesClassifier(String, String)},
     *         may be <code>null</code> if none can be calculated)
     */
    public MavenArtifact toSourcesArtifact() {
        var sourcesClassifier = MavenConventions.getSourcesClassifier(classifier, packaging);
        if (sourcesClassifier == null) {
            // no sources
            return null;
        }

        return new MavenArtifact(groupId, artifactId, version, DEFAULT_PACKAGING, sourcesClassifier);
    }

    @Override
    public String toString() {
        var details = new StringBuilder();
        if ((exclusions != null) && !exclusions.isEmpty()) {
            details.append("exclusions = [")
                    .append(exclusions.stream().map(Exclusion::toString).collect(joining(", ")))
                    .append("]");
        }

        if (neverlink) {
            if (details.length() > 0) {
                details.append(", ");
            }
            details.append("neverlink = true");
        }

        if (testonly) {
            if (details.length() > 0) {
                details.append(", ");
            }
            details.append("testonly = true");
        }

        var result = new StringBuilder();
        appendCoordinatesWithoutVersion(result);
        result.append(':').append(version);

        if (details.length() > 0) {
            result.append(" (").append(details).append(")");
        }

        return result.toString();
    }

}
