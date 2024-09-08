package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

import java.util.SortedSet;
import java.util.TreeSet;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact;
import com.salesforce.tools.bazel.mavendependencies.collection.MavenArtifact.Exclusion;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;

/**
 * Base class for a command working with dependencies file and requiring single dependency coordinates as input
 */
public abstract class BaseDependencyCommand extends BaseCommandUsingDependencyCollection {

    /**
     * Maven artifact coordinates as command line options.
     */
    public static class ArtifactCoordinates {
        @Option(names = { "--group-id" }, description = "Maven group id of the dependency")
        public String groupId;

        @Option(names = { "--artifact-id" }, description = "Maven artifact id of the dependency")
        public String artifactId;

        @Option(names = { "--version" }, description = "optional version of the dependency")
        public String version;

        @Option(names = {
                "--packaging" }, description = "optional packaging of the dependency to add", defaultValue = "jar")
        public String packaging;

        @Option(names = { "--classifier" }, description = "optional classifier of the dependency")
        public String classifier;
    }

    protected static class ExclusionConverter implements ITypeConverter<Exclusion> {
        @Override
        public Exclusion convert(String value) throws Exception {
            return Exclusion.fromCoordinatesString(value);
        }
    }

    @Parameters(index = "0", description = "Dependency coordinates as parameter (or artifact id search string if it doesn't contain a collon). When combining Maven coordinates as parameter with additional options, the coordinates must contain only <groupId:artifactId> or <groupId:artifactId:version>!", paramLabel = "[<groupId>:]<artifactId>[:<packaging>[:<classifier>]][:<version>]", arity = "0..1", scope = ScopeType.INHERIT)
    private String dependencyCoordinatesAsParameter;

    @Option(names = {
            "--dependency" }, description = "Dependency coordinates as option (see parameter documentation for details)", paramLabel = "[<groupId>:]<artifactId>[:<packaging>[:<classifier>]][:<version>]", scope = ScopeType.INHERIT)
    private String dependencyCoordinatesAsOption;

    @ArgGroup(exclusive = false, multiplicity = "0..1", heading = "  Dependency coordinates using options%n")
    protected ArtifactCoordinates optionalDependencyDeclaredUsingOptions;

    /**
     * Returns a {@link ArtifactCoordinates} from the command arguments.
     *
     * @param out
     * @return {@link ArtifactCoordinates} (never <code>null</code>)
     * @throws IllegalArgumentException
     *             in case of unparsable or incomplete options
     */
    protected ArtifactCoordinates getArtifactCoordinatesFromArguments(
            MessagePrinter out,
            boolean requireVersion) throws IllegalArgumentException {
        var result = new ArtifactCoordinates();

        // prefer parsing from coordinates
        final var coordinates = getCoordinatesFromParameterOrOption();
        if (coordinates != null) {
            // [<groupId>:]<artifactId>[:<packaging>[:<classifier>]][:<version>]
            if (coordinates.indexOf(':') == -1) {
                result.artifactId = coordinates;
            } else {
                final var tokens = coordinates.split(":");
                if (tokens.length < 2) {
                    throw new IllegalArgumentException(
                            "When combining Maven coordinates as parameter with additional options, the coordinates must contain only <groupId:artifactId> or <groupId:artifactId:version>!");
                }
                result.groupId = tokens[0];
                result.artifactId = tokens[1];

                switch (tokens.length) {
                    case 3:
                        result.version = tokens[2];
                        break;
                    case 4:
                        result.packaging = tokens[2];
                        result.version = tokens[3];
                        break;
                    case 5:
                        result.packaging = tokens[2];
                        result.classifier = tokens[3];
                        result.version = tokens[4];
                        break;
                    default:
                        break;
                }
            }
        } else if (optionalDependencyDeclaredUsingOptions == null) {
            throw new IllegalArgumentException(
                    "Either Maven coordinates or --group-id and --artifact-id options must be specified");
        }

        // now check we have everything
        if ((result.groupId == null) && (optionalDependencyDeclaredUsingOptions.groupId == null)) {
            throw new IllegalArgumentException(
                    "Missing group id. Please specify --group-id or provide it as part of coordinates");
        }

        if ((result.artifactId == null) && (optionalDependencyDeclaredUsingOptions.artifactId == null)) {
            throw new IllegalArgumentException(
                    "Missing artifact id. Please specify --artifact-id or provide it as part of coordinates");
        }

        if (requireVersion && (result.version == null) && (optionalDependencyDeclaredUsingOptions.version == null)) {
            throw new IllegalArgumentException(
                    "Missing version. Please specify --version or provide it as part of coordinates");
        }

        if (result.groupId == null) {
            result.groupId = optionalDependencyDeclaredUsingOptions.groupId;
        }
        if (result.artifactId == null) {
            result.groupId = optionalDependencyDeclaredUsingOptions.artifactId;
        }
        if ((result.packaging == null) && (optionalDependencyDeclaredUsingOptions != null)) {
            result.packaging = optionalDependencyDeclaredUsingOptions.packaging;
        }
        if ((result.classifier == null) && (optionalDependencyDeclaredUsingOptions != null)) {
            result.classifier = optionalDependencyDeclaredUsingOptions.classifier;
        }
        if ((result.version == null) && (optionalDependencyDeclaredUsingOptions != null)) {
            result.version = optionalDependencyDeclaredUsingOptions.version;
        }

        return result;
    }

    /**
     * Returns a {@link MavenArtifact} from the provided arguments.
     * <p>
     * Note, the returned artifact might or might not exists in the dependencies collection. There is no guarantee it
     * does exist.
     * </p>
     *
     * @param out
     * @return
     */
    protected MavenArtifact getArtifactFromArguments(MessagePrinter out) {
        final var coordinates = getCoordinatesFromParameterOrOption();
        if (coordinates != null) {
            if (coordinates.indexOf(':') == -1) {
                out.info(format("Searching collection for dependency '%s'", coordinates));
                final SortedSet<MavenArtifact> result = mavenDependenciesCollection.getAllArtifacts()
                        .filter(a -> a.getArtifactId().contains(coordinates))
                        .collect(toCollection(TreeSet::new));

                if (result.isEmpty()) {
                    throw new IllegalArgumentException(
                            format(
                                "No dependency found with a matching artifact id for '%s' in catalogs",
                                coordinates));
                }

                if (verbose) {
                    out.notice(format("Found %d candidates for '%s':", result.size(), coordinates));
                    result.stream().map(a -> " - " + a.toCoordinatesStringWithoutVersion()).forEach(out::notice);
                }

                var mavenArtifact = result.first();

                if (result.size() > 1) {
                    final TreeSet<MavenArtifact> preferred = result.stream()
                            .filter(a -> a.getArtifactId().equals(coordinates))
                            .collect(toCollection(TreeSet::new));
                    if (preferred.size() == 1) {
                        mavenArtifact = preferred.first();
                        out.info(
                            format(
                                "Found: %s (with matching artifact id '%s', out of %d candidates)",
                                mavenArtifact.toCoordinatesStringWithoutVersion(),
                                coordinates,
                                result.size()));
                    } else if (preferred.size() > 1) {
                        mavenArtifact = preferred.first();
                        out.info(
                            format(
                                "Picked: %s (out of %d candidates, multiple sharing same artifact id '%s')",
                                mavenArtifact.toCoordinatesStringWithoutVersion(),
                                result.size(),
                                coordinates));
                    } else {
                        out.info(
                            format(
                                "Found: %s (out of %d candidates)",
                                mavenArtifact.toCoordinatesStringWithoutVersion(),
                                result.size()));
                    }
                } else {
                    out.info(format("Found: %s (best match)", mavenArtifact.toCoordinatesStringWithoutVersion()));
                }

                return mavenArtifact;
            }
            if (optionalDependencyDeclaredUsingOptions != null) {
                final var tokens = coordinates.split(":");
                if (tokens.length == 2) {
                    if (optionalDependencyDeclaredUsingOptions.version == null) {
                        throw new IllegalArgumentException(
                                "Missing version. Please specify --version or provide it as part of coordinates");
                    }

                    return new MavenArtifact(
                            tokens[0],
                            tokens[1],
                            optionalDependencyDeclaredUsingOptions.version,
                            optionalDependencyDeclaredUsingOptions.packaging,
                            optionalDependencyDeclaredUsingOptions.classifier);
                }

                if (tokens.length == 3) {
                    return new MavenArtifact(
                            tokens[0],
                            tokens[1],
                            tokens[2],
                            optionalDependencyDeclaredUsingOptions.packaging,
                            optionalDependencyDeclaredUsingOptions.classifier);
                }

                throw new IllegalArgumentException(
                        "When combining Maven coordinates as parameter with additional options, the coordinates must contain only <groupId:artifactId> or <groupId:artifactId:version>!");
            }

            final var tokens = coordinates.split(":");
            if (tokens.length == 2) {
                // <groupId:artifactId>
                return getSingleArtifactInCollection(tokens[0], tokens[1]);
            }

            return MavenArtifact.fromCoordinatesString(coordinates);

        }

        if (optionalDependencyDeclaredUsingOptions == null) {
            throw new IllegalArgumentException(
                    "Either Maven coordinates or --group-id and --artifact-id options must be specified");
        }

        if (optionalDependencyDeclaredUsingOptions.groupId == null) {
            throw new IllegalArgumentException(
                    "Missing group id. Please specify --group-id or provide it as part of coordinates");
        }

        if (optionalDependencyDeclaredUsingOptions.artifactId == null) {
            throw new IllegalArgumentException(
                    "Missing artifact id. Please specify --artifact-id or provide it as part of coordinates");
        }

        if (optionalDependencyDeclaredUsingOptions.version == null) {
            throw new IllegalArgumentException(
                    "Missing version. Please specify --version or provide it as part of coordinates");
        }

        return new MavenArtifact(
                optionalDependencyDeclaredUsingOptions.groupId,
                optionalDependencyDeclaredUsingOptions.artifactId,
                optionalDependencyDeclaredUsingOptions.version,
                optionalDependencyDeclaredUsingOptions.packaging,
                optionalDependencyDeclaredUsingOptions.classifier);
    }

    protected String getCoordinatesFromParameterOrOption() {
        if ((dependencyCoordinatesAsParameter != null) && (dependencyCoordinatesAsOption != null)) {
            throw new IllegalArgumentException(
                    "Maven coordinates can only be supplied as either parameter or using the --dependency option but never both!");
        }

        return dependencyCoordinatesAsParameter != null ? dependencyCoordinatesAsParameter
                : dependencyCoordinatesAsOption;
    }

    private MavenArtifact getSingleArtifactInCollection(
            String groupId,
            String artifactId) throws IllegalArgumentException {
        final SortedSet<MavenArtifact> result = mavenDependenciesCollection.getAllArtifacts()
                .filter(a -> (a.getArtifactId().equals(artifactId) && a.getGroupId().equals(groupId)))
                .collect(toCollection(TreeSet::new));

        if (result.size() == 1) {
            return result.first();
        }
        if (result.size() > 1) {
            throw new IllegalArgumentException(
                    format(
                        "Maven coordinates '%s:%s' are ambiguous. More than one artifact found: %s",
                        groupId,
                        artifactId,
                        result.stream().map(MavenArtifact::toCoordinatesString).collect(joining(", "))));
        }
        throw new IllegalArgumentException(
                format("No artifact found in the dependency collection for coordinates '%s:%s'.", groupId, artifactId));
    }
}
