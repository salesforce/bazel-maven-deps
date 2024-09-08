load("@bazel_maven_deps//bazel:defs.bzl", "maven")

#
# Collection of Maven dependencies for this Bazel workspace
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Use a TODO file to capture additional notes/technical debt.
#

_CAFFEINE_VERSION = "3.1.8"

MAVEN_BOM_IMPORTS = maven.imports([
])

MAVEN_DEPENDENCIES = maven.dependencies([
    "com.github.ben-manes.caffeine:caffeine:" + _CAFFEINE_VERSION,
    "com.github.ben-manes.caffeine:guava:" + _CAFFEINE_VERSION,
])

MAVEN_EXCLUSIONS = [
]
