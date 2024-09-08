load("@bazel_maven_deps//bazel:defs.bzl", "maven")

#
# Collection of Maven dependencies for this Bazel workspace
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Use a TODO file to capture additional notes/technical debt.
#

_MOCKITO_VERSION = "5.11.0"

MAVEN_BOM_IMPORTS = maven.imports([
])

MAVEN_DEPENDENCIES = maven.dependencies([
    maven.artifact(
        group = "org.mockito",
        artifact = "mockito-core",
        version = _MOCKITO_VERSION,
        testonly = True,
    ),
    maven.artifact(
        group = "org.mockito",
        artifact = "mockito-junit-jupiter",
        version = _MOCKITO_VERSION,
        testonly = True,
    ),
])

MAVEN_EXCLUSIONS = [
]
