load("@bazel_maven_deps//bazel:defs.bzl", "maven")

#
# Collection of Maven dependencies for this Bazel workspace
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Use a TODO file to capture additional notes/technical debt.
#

_SLF4J_VERSION = "2.0.7"

MAVEN_BOM_IMPORTS = maven.imports([
])

MAVEN_DEPENDENCIES = maven.dependencies([
    "org.slf4j:jcl-over-slf4j:" + _SLF4J_VERSION,
    "org.slf4j:slf4j-api:" + _SLF4J_VERSION,
    "org.slf4j:slf4j-simple:" + _SLF4J_VERSION,
])

MAVEN_EXCLUSIONS = [
]
