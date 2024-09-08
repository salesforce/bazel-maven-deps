package com.salesforce.tools.bazel.mavendependencies.collection;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import java.util.Comparator;
import java.util.SortedSet;

/**
 * A very simple structure to communicated content delta.
 */
public class CollectionDelta {

    public static class Modification implements Comparable<Modification> {

        public enum Type {
            ADDED, REMOVED, VERSION_UPDATE, OTHER_UPDATE, EXCLUSION_ADDED, EXCLUSION_REMOVED
        }

        private static final Comparator<Modification> cmp =
                Comparator.comparing(Modification::getArtifactCoordinatesWithoutVersion)
                        .thenComparing(Comparator.comparing(Modification::getTypeOfModification));

        public static Modification added(String artifactCoordinatesWithoutVersion, String artifactVersion) {
            return new Modification(Type.ADDED, artifactCoordinatesWithoutVersion, artifactVersion, null, null);
        }

        public static Modification exclusionAdded(String groupId, String artifactId) {
            return new Modification(Type.EXCLUSION_ADDED, groupId + ":" + artifactId, null, null, null);
        }

        public static Modification exclusionRemoved(String groupId, String artifactId) {
            return new Modification(Type.EXCLUSION_REMOVED, groupId + ":" + artifactId, null, null, null);
        }

        public static Modification otherUpdate(
                String artifactCoordinatesWithoutVersion,
                String artifactVersion,
                SortedSet<String> detailedDiff) {
            return new Modification(
                    Type.OTHER_UPDATE,
                    artifactCoordinatesWithoutVersion,
                    artifactVersion,
                    null,
                    detailedDiff);
        }

        public static Modification removed(String coordinatesStringWithoutVersion, String version) {
            return new Modification(Type.REMOVED, coordinatesStringWithoutVersion, version, null, null);
        }

        public static Modification versionUpdate(
                String artifactCoordinatesWithoutVersion,
                String newVersion,
                String oldVersion) {
            return new Modification(
                    Type.VERSION_UPDATE,
                    artifactCoordinatesWithoutVersion,
                    newVersion,
                    oldVersion,
                    null);
        }

        public static Modification versionUpdateWithDiff(
                String artifactCoordinatesWithoutVersion,
                String newVersion,
                String oldVersion,
                SortedSet<String> diff) {
            return new Modification(
                    Type.VERSION_UPDATE,
                    artifactCoordinatesWithoutVersion,
                    newVersion,
                    oldVersion,
                    diff);
        }

        private final Type typeOfModification;
        private final String artifactCoordinatesWithoutVersion;
        private final String artifactVersion;
        private final String oldVersion;
        private final SortedSet<String> detailedDiff;

        private Modification(Type typeOfModification, String artifactCoordinatesWithoutVersion, String artifactVersion,
                String oldVersion, SortedSet<String> detailedDiff) {
            this.typeOfModification = requireNonNull(typeOfModification);
            this.artifactCoordinatesWithoutVersion = requireNonNull(artifactCoordinatesWithoutVersion);
            this.artifactVersion = requireNonNull(artifactVersion);
            this.oldVersion = oldVersion;
            this.detailedDiff = detailedDiff;
        }

        @Override
        public int compareTo(Modification o) {
            return cmp.compare(this, o);
        }

        /**
         * @return artifact coordinates (typically Maven groupId:artifactId; never <code>null</code>)
         */
        public String getArtifactCoordinatesWithoutVersion() {
            return artifactCoordinatesWithoutVersion;
        }

        /**
         * @return artifact version (never <code>null</code>)
         */
        public String getArtifactVersion() {
            return artifactVersion;
        }

        /**
         * @return detailed diff (may be set of OTHER_UPDATE and VERSION_UPDATE; but can be <code>null</code>)
         */
        public SortedSet<String> getDetailedDiff() {
            return detailedDiff;
        }

        /**
         * @return old version (only set when type is VERSION_UPDATE; otherwise <code>null</code>)
         */
        public String getOldVersion() {
            return oldVersion;
        }

        /**
         * @return type of modification (never <code>null</code>)
         */
        public Type getTypeOfModification() {
            return typeOfModification;
        }

        @Override
        public String toString() {
            switch (typeOfModification) {
                case ADDED:
                    return String.format("+ %s:%s", artifactCoordinatesWithoutVersion, artifactVersion);

                case REMOVED:
                    return String.format("- %s:%s", artifactCoordinatesWithoutVersion, artifactVersion);

                case EXCLUSION_ADDED:
                    return String.format("+ exclusion: %s", artifactCoordinatesWithoutVersion);

                case EXCLUSION_REMOVED:
                    return String.format("- exclusion: %s", artifactCoordinatesWithoutVersion);

                case VERSION_UPDATE:
                    if (detailedDiff != null) {
                        return String.format(
                            "* %s (%s -> %s, %s)",
                            artifactCoordinatesWithoutVersion,
                            oldVersion,
                            artifactVersion,
                            detailedDiff.stream().collect(joining(", ")));
                    }

                    return String
                            .format("* %s (%s -> %s)", artifactCoordinatesWithoutVersion, oldVersion, artifactVersion);

                case OTHER_UPDATE:
                default:
                    if (detailedDiff != null) {
                        return String.format(
                            "* %s:%s (%s)",
                            artifactCoordinatesWithoutVersion,
                            artifactVersion,
                            detailedDiff.stream().collect(joining(", ")));
                    }

                    return String.format("* %s:%s", artifactCoordinatesWithoutVersion, artifactVersion);
            }
        }
    }

    /**
     * A set of modifications
     */
    public final SortedSet<Modification> modifications;

    /**
     * A list of obsolete groups, i.e. files no longer needed in a collection
     */
    public final SortedSet<String> obsoleteGroups;

    public CollectionDelta(SortedSet<Modification> modifications, SortedSet<String> obsoleteGroups) {
        this.modifications = modifications;
        this.obsoleteGroups = obsoleteGroups;
    }
}