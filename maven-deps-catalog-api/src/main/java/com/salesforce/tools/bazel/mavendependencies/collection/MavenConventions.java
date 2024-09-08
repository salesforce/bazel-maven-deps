package com.salesforce.tools.bazel.mavendependencies.collection;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.Set;

import com.salesforce.tools.bazel.mavendependencies.starlark.BazelConventions;

/**
 * Some typical Maven things
 */
public class MavenConventions {

    private static final Set<String> groupIdPrefixesWithLevelThreeGrouping = Set.of("com.salesforce.");

    /**
     * Returns the file grouping for sorting artifacts into different files based on group id.
     * <p>
     * The algorithm splits the group id into multiple tokens and uses the first two tokens basically.
     * </p>
     *
     * @param groupId
     *            the group id
     * @return the file group
     */
    public static String getFileGroup(String groupId) {
        var tokens = requireNonNull(groupId, "group id cannot be null").split("\\.");
        if (tokens.length <= 2) {
            return BazelConventions.toTargetName(groupId);
        }

        if ((tokens.length >= 3) && groupIdPrefixesWithLevelThreeGrouping.stream().anyMatch(groupId::startsWith)) {
            return BazelConventions.toTargetName(tokens[0] + "_" + tokens[1] + "_" + tokens[2]);
        }

        return BazelConventions.toTargetName(tokens[0] + "_" + tokens[1]);
    }

    /**
     * Returns the classifier for obtaining sources jar.
     * <p>
     * By convention sources are retrieved as jar files in Maven. They use a special classifier.
     * </p>
     *
     * @param classifier
     *            original classifier
     * @param extension
     *            original extension
     * @return a classifier (maybe <code>null</code> if the given classifier/artifact combination does not typically
     *         have sources)
     */
    public static String getSourcesClassifier(String classifier, String extension) {
        if ((extension == null) || extension.isBlank() || "jar".equals(extension)) {
            if ((classifier == null) || classifier.isBlank()) {
                return "sources"; // regular sources jar
            }

            if ("tests".equals(classifier)) {
                return "test-sources"; // test sources
            }
        }

        return null; // no sources
    }

    /**
     * Converts the specified Maven property key to a Bazel version variable name as used in the dependencies file.
     *
     * @param versionProperty
     * @return
     */
    public static String toBazelVersionVariableName(String versionProperty) {
        var bazelVersionName = BazelConventions.toTargetName(versionProperty).toUpperCase(Locale.US);
        if (!bazelVersionName.startsWith("_")) {
            bazelVersionName = "_" + bazelVersionName;
        }
        if (!bazelVersionName.endsWith("_VERSION")) {
            bazelVersionName += "_VERSION";
        }
        return bazelVersionName;
    }

    private MavenConventions() {
        // empty
    }

}
