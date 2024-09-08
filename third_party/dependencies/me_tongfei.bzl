load("@bazel_maven_deps//bazel:defs.bzl", "maven")

#
# Collection of Maven dependencies for this Bazel workspace
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Use a TODO file to capture additional notes/technical debt.
#

_PROGRESSBAR_VERSION = "0.9.5"

MAVEN_BOM_IMPORTS = maven.imports([
])

MAVEN_DEPENDENCIES = maven.dependencies([
    "me.tongfei:progressbar:" + _PROGRESSBAR_VERSION,
])

MAVEN_EXCLUSIONS = [
]
