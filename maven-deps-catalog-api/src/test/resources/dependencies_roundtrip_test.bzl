load("@bazel_maven_deps//bazel:defs.bzl", "maven")

#
# Collection of Maven dependencies for this Bazel workspace
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Use a TODO file to capture additional notes/technical debt.
#

_COMMONS_LANG3_VERSION = "3.12.0"
_COMMONS_TEXT_VERSION = "1.9"
_HAMCREST_VERSION = "2.2"
_JUNIT_VERSION = "5.10.1"
_MAVEN_PLUGIN_ANNOTATIONS_VERSION = "3.10.2"
_MAVEN_RESOLVER_API_VERSION = "1.9.18"
_MAVEN_VERSION = "3.9.6"

MAVEN_BOM_IMPORTS = maven.imports([
    maven.bom(
        group = "com.fasterxml.jackson",
        artifact = "jackson-bom",
        version = "2.10.3",
        exclusions = ["junit:junit"],
    ),
    maven.bom(
        group = "org.junit",
        artifact = "junit-bom",
        version = _JUNIT_VERSION,
    ),
])

MAVEN_DEPENDENCIES = maven.dependencies([
    maven.artifact(
        group = "org.apache.commons",
        artifact = "commons-lang3",
        packaging = "jar",
        version = _COMMONS_LANG3_VERSION,
    ),
    maven.artifact(
        group = "org.apache.commons",
        artifact = "commons-text",
        packaging = "jar",
        version = _COMMONS_TEXT_VERSION,
    ),
    maven.artifact(
        group = "org.apache.maven",
        artifact = "maven-core",
        packaging = "jar",
        version = _MAVEN_VERSION,
    ),
    maven.artifact(
        group = "org.apache.maven.plugin-tools",
        artifact = "maven-plugin-annotations",
        packaging = "jar",
        version = _MAVEN_PLUGIN_ANNOTATIONS_VERSION,
    ),
    maven.artifact(
        group = "org.apache.maven.resolver",
        artifact = "maven-resolver-api",
        packaging = "jar",
        version = _MAVEN_RESOLVER_API_VERSION,
    ),
    maven.artifact(
        group = "org.apache.maven.resolver",
        artifact = "maven-resolver-connector-basic",
        packaging = "jar",
        version = _MAVEN_RESOLVER_API_VERSION,
    ),
    maven.artifact(
        group = "org.apache.maven.resolver",
        artifact = "maven-resolver-impl",
        packaging = "jar",
        version = _MAVEN_RESOLVER_API_VERSION,
    ),
    maven.artifact(
        group = "org.apache.maven.resolver",
        artifact = "maven-resolver-transport-classpath",
        packaging = "jar",
        version = _MAVEN_RESOLVER_API_VERSION,
    ),
    maven.artifact(
        group = "org.apache.maven.resolver",
        artifact = "maven-resolver-transport-file",
        packaging = "jar",
        version = _MAVEN_RESOLVER_API_VERSION,
    ),
    maven.artifact(
        group = "org.apache.maven.resolver",
        artifact = "maven-resolver-transport-http",
        packaging = "jar",
        version = _MAVEN_RESOLVER_API_VERSION,
    ),
    maven.artifact(
        group = "org.apache.maven.resolver",
        artifact = "maven-resolver-util",
        packaging = "jar",
        version = _MAVEN_RESOLVER_API_VERSION,
    ),
    maven.artifact(
        group = "org.hamcrest",
        artifact = "hamcrest",
        packaging = "jar",
        version = _HAMCREST_VERSION,
        exclusions = ["*:*"],
        neverlink = True,
        testonly = True,
    ),
])

MAVEN_EXCLUSIONS = [
]
