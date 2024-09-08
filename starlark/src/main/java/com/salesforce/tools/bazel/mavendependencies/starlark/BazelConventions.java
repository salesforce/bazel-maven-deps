package com.salesforce.tools.bazel.mavendependencies.starlark;

import java.util.regex.Pattern;

/**
 * A set of conventions for translating Module descriptor information into Bazel compatible names.
 */
public class BazelConventions {

    private static final Pattern NOT_ALLOWED_NAME_CHARS = Pattern.compile("[^_0-9A-Za-z]");

    /**
     * Simplifies a label by removing its target name if it matched the last segment of the package path.
     *
     * @param maybeLabel
     *            label to simplify (may be <code>null</code>)
     * @return simplified label or <code>null</code>
     */
    public static String simplifyLabel(String maybeLabel) {
        if (maybeLabel == null) {
            return null;
        }

        var collonPos = maybeLabel.indexOf(':');
        if (collonPos == -1) {
            return maybeLabel;
        }

        var packagePathAndRepo = maybeLabel.substring(0, collonPos);
        var target = maybeLabel.substring(collonPos + 1);

        if (packagePathAndRepo.endsWith("/" + target)) {
            return packagePathAndRepo; // simplify label
        }

        return maybeLabel; // cannot simplify
    }

    /**
     * Converts any given identifier to a Starlark compatible target name.
     *
     * @param identifier
     *            identifier to convert (replacing all its not allowed characters with underscore)
     * @return a Starlark conforming identifier containing only <code>A-Z</code>, <code>a–z</code>, <code>0–9</code>,
     *         and '<code>_</code>'
     */
    public static String toStarlarkIdentifier(String identifier) {
        return NOT_ALLOWED_NAME_CHARS.matcher(identifier).replaceAll("_");
    }

    /**
     * Converts any given identifier to a Bazel compatible target name.
     * <p>
     * See <a href="https://bazel.build/concepts/labels#labels-lexical-specification">the Bazel documentation</a> for
     * the full specification. We intentionally allow only a subset of characters.
     * </p>
     *
     * @param identifier
     *            identifier to convert (replacing all its not allowed characters with underscore)
     * @return a Bazel rule name conforming identifier containing only <code>A-Z</code>, <code>a–z</code>,
     *         <code>0–9</code>, and '<code>_</code>'
     */
    public static String toTargetName(String identifier) {
        return NOT_ALLOWED_NAME_CHARS.matcher(identifier).replaceAll("_");
    }

    /**
     * Converts the specified artifact id into a Bazel conforming target name.
     * <p>
     * Per our conventions, this method <b>must</b> be used when translating from third party Maven modules artifact ids
     * to Bazel dependencies.
     * </p>
     * <p>
     * Note, this method is not 100% conflict free. There is a chance for collisions for the same group id + artifact id
     * when one has an extension of type 'foo' but no classifier and the other has no extension but a classifier of type
     * 'foo'. Since both are sharing the same artifact and group id, the confusion should be addressed with the origin.
     * We do not want our conventions to become bloated.
     * </p>
     *
     * @param groupId
     *            Maven group id
     * @param artifactId
     *            Maven artifact id
     * @param extensions
     *            Maven extension (aka. type, may be <code>null</code>)
     * @param classifier
     *            Maven classifier (may be <code>null</code>)
     * @return a Bazel conforming target name
     */
    public static String toTargetName(String groupId, String artifactId, String extensions, String classifier) {
        // group and artifact are always expected (no shortening to avoid conflicts)
        var targetName = toTargetName(groupId) + "_" + toTargetName(artifactId);

        // extension is only relevant if it's set and not 'jar'
        if ((extensions != null) && !extensions.isBlank() && !"jar".equals(extensions)) {
            targetName += '_' + toTargetName(extensions);
        }

        // any classifier that is null, blank or explicitly 'jar' is considered a jar target.
        if ((classifier == null) || classifier.isBlank() || "jar".equals(classifier)) {
            return targetName;
        }
        return targetName + '_' + toTargetName(classifier);
    }

    private BazelConventions() {

    }

}
