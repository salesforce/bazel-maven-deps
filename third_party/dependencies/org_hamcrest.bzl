load("@bazel_maven_deps//bazel:defs.bzl", "maven")

#
# Collection of Maven dependencies for this Bazel workspace
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Use a TODO file to capture additional notes/technical debt.
#

_HAMCREST_VERSION = "2.2"

MAVEN_BOM_IMPORTS = maven.imports([
])

MAVEN_DEPENDENCIES = maven.dependencies([
    maven.artifact(
        group = "org.hamcrest",
        artifact = "hamcrest",
        version = _HAMCREST_VERSION,
        testonly = True,
    ),
])

MAVEN_EXCLUSIONS = [
]
