"""Macros for dealing with the dependency collection & catalog"""

def _maven_artifact(group, artifact, version, classifier = None, packaging = "jar", exclusions = None, neverlink = False, testonly = False):
    """Generates the data map for a Maven artifact given the available information about its coordinates.

    Args:
      group: `string`
        The Maven artifact coordinate group name (ex: "com.google.guava").
      artifact: `string`
        The Maven artifact coordinate artifact name (ex: "guava").
      version: `string`
        The Maven artifact version (ex: "1.0.0").
      classifier: `string`
        The Maven artifact classifier (ex: "javadoc").
      packaging: `string`
        The Maven packaging specifier (ex: "jar").
        Defaults to `jar`.
      exclusions: `list of string; optional`
        List of `<groupId>:<artifactId>` to exclude from the transitive dependencies of this artifact.

        Exclusions provide a way to exclude certain artifacts from being made available in the pinned catalog.
        It's possible to use `*` (star) wildcard for matching any group and/or any artifact id.
      neverlink: `bool; optional; default is False`
        Whether this artifact should only be used for compilation and not at runtime.
        See Bazel's `java_library` documentation for details.
      testonly: `bool; optional; default is False`
        If True, only testonly targets (such as tests) can depend on this artifact.
        See Bazel's `java_library` documentation for details.
    """

    # Output Schema:
    #     {
    #         "group": String
    #         "artifact": String
    #         "version": String
    #         "classifier": Optional String
    #         "packaging": String
    #         "exclusions": List of String
    #         "neverlink": Boolean
    #         "testonly": Boolean
    #     }
    maven_artifact = {}
    maven_artifact["group"] = group
    maven_artifact["artifact"] = artifact
    maven_artifact["version"] = version
    if classifier != None:
        maven_artifact["classifier"] = classifier
    maven_artifact["packaging"] = packaging
    if exclusions != None:
        maven_artifact["exclusions"] = exclusions
    maven_artifact["neverlink"] = neverlink
    maven_artifact["testonly"] = testonly

    return maven_artifact

def _parse_maven_coordinates_string(maven_coordinates_string):
    """Parses a Maven coordinates string into a maven artifact map (see above).

    Args:
      maven_coordinates_string: `string`

      group : artifact [ : packaging [ : classifier] ] : version
    """

    # must match
    # com.salesforce.tools.corebuild.graph.dependencies.MavenArtifact.fromCoordinatesString(String)
    # (https://git.soma.salesforce.com/modularization-team/module-graph/blob/006fae950a7d0a182f4a59d61dcee9d0fcb7d6a0/module-descriptor/src/main/java/com/salesforce/tools/corebuild/graph/dependencies/MavenArtifact.java#L116)

    tokens = maven_coordinates_string.split(":")
    if len(tokens) < 3:
        fail("Could not parse maven coordinate", attr = maven_coordinates_string)

    group = tokens[0]
    if len(group) < 1 or group.isspace():
        fail("Maven group id must not be empty", attr = maven_coordinates_string)

    artifact = tokens[1]
    if len(artifact) < 1 or artifact.isspace():
        fail("Maven artifact id must not be empty", attr = maven_coordinates_string)

    version = tokens[-1]
    if len(version) < 1 or version.isspace():
        fail("Maven version must not be empty", attr = maven_coordinates_string)

    if len(tokens) == 3:
        return _maven_artifact(group = group, artifact = artifact, version = version)
    elif len(tokens) == 4:
        return _maven_artifact(group = group, artifact = artifact, packaging = tokens[2], version = version)
    elif len(tokens) == 5:
        return _maven_artifact(group = group, artifact = artifact, packaging = tokens[2], classifier = tokens[3], version = version)
    else:
        fail("Could not parse maven coordinate", attr = maven_coordinates_string)

def _maven_artifact_from_string(artifact):
    """Computes a Maven artifact data map (parses string or returns as if is type is not a string)"""
    return _parse_maven_coordinates_string(artifact) if type(artifact) == "string" else artifact

def _maven_dependencies(artifacts):
    """Generates the list of artifact data maps.

    Args:
      artifacts: `list of string or dict`
        List of Maven artifacts.

        Supported values are either Maven coordinate strings (including a version) or Maven
        artifact data maps as created by `maven.artifact`.
    """

    # Output Schema:
    #     [
    #         {
    #           see maven.artifact(...)
    #         },
    #         ...
    #     ]
    return [_maven_artifact_from_string(artifact) for artifact in artifacts]

def _maven_bom(group, artifact, version, classifier = None, exclusions = None, neverlink = False, testonly = False):
    """Generates the data map for a Maven BOM artifact given the available information about its coordinates.

    This is a convience method. The result is equal to calling `maven.artifact` with packaging set to `pom`.

    Args:
      group: `string`
        The Maven artifact coordinate group name (ex: "com.google.guava").
      artifact: `string`
        The Maven artifact coordinate artifact name (ex: "guava").
      version: `string`
        The Maven artifact version (ex: "1.0.0").
      classifier: `string`
        The Maven artifact classifier (ex: "javadoc").
      exclusions: `list of string; optional`
        List of `<groupId>:<artifactId>` to exclude from the transitive dependencies of this artifact.

        Exclusions provide a way to exclude certain artifacts from being made available in the pinned catalog.
        It's possible to use `*` (star) wildcard for matching any group and/or any artifact id.
      neverlink: `bool; optional; default is False`
        Whether this artifact should only be used for compilation and not at runtime.
        See Bazel's `java_library` documentation for details.
      testonly: `bool; optional; default is False`
        If True, only testonly targets (such as tests) can depend on this artifact.
        See Bazel's `java_library` documentation for details.
    """

    # Output Schema:
    #     {
    #         "group": String
    #         "artifact": String
    #         "version": String
    #         "classifier": Optional String
    #         "packaging": String
    #         "exclusions": List of String
    #         "neverlink": Boolean
    #         "testonly": Boolean
    #     }
    maven_artifact = {}
    maven_artifact["group"] = group
    maven_artifact["artifact"] = artifact
    maven_artifact["version"] = version
    if classifier != None:
        maven_artifact["classifier"] = classifier
    maven_artifact["packaging"] = "pom"
    if exclusions != None:
        maven_artifact["exclusions"] = exclusions
    maven_artifact["neverlink"] = neverlink
    maven_artifact["testonly"] = testonly

    return maven_artifact

def _maven_bom_from_string(artifact):
    """Computes a Maven artifact data map"""
    artifact_obj = _parse_maven_coordinates_string(artifact) if type(artifact) == "string" else artifact

    # override packaging type
    artifact_obj["packaging"] = "pom"

    return artifact_obj

def _maven_imports(boms):
    """Generates the list of BOM artifact data maps.

    Args:
      boms: `list of string or dict`
        List of Maven BOM artifacts.

        Supported values are either Maven coordinate strings (including a version) or Maven
        artifact data maps as created by `maven.bom`.
    """

    # Output Schema:
    #     [
    #         {
    #           see maven.bom(...)
    #         },
    #         ...
    #     ]
    return [_maven_bom_from_string(bom) for bom in boms]

maven = struct(
    artifact = _maven_artifact,
    dependencies = _maven_dependencies,
    bom = _maven_bom,
    imports = _maven_imports,
    parse_maven_coordinates = _parse_maven_coordinates_string,
)
