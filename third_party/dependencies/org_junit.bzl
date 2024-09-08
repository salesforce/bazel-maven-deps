load("@bazel_maven_deps//bazel:defs.bzl", "maven")

#
# Collection of Maven dependencies for this Bazel workspace
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Use a TODO file to capture additional notes/technical debt.
#

_JUNIT_PLATFORM_SUITE_VERSION = "1.10.2"
_JUNIT_VERSION = "5.10.2"

MAVEN_BOM_IMPORTS = maven.imports([
])

MAVEN_DEPENDENCIES = maven.dependencies([
    maven.artifact(
        group = "org.junit.jupiter",
        artifact = "junit-jupiter",
        version = _JUNIT_VERSION,
        testonly = True,
    ),
    maven.artifact(
        group = "org.junit.platform",
        artifact = "junit-platform-reporting",
        version = _JUNIT_PLATFORM_SUITE_VERSION,
        testonly = True,
    ),
    maven.artifact(
        group = "org.junit.platform",
        artifact = "junit-platform-runner",
        version = _JUNIT_PLATFORM_SUITE_VERSION,
        testonly = True,
    ),
    maven.artifact(
        group = "org.junit.platform",
        artifact = "junit-platform-suite",
        version = _JUNIT_PLATFORM_SUITE_VERSION,
        testonly = True,
    ),
])

MAVEN_EXCLUSIONS = [
]
