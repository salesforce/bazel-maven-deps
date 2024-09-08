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
_COMMONS_TEXT_VERSION = "1.11.0"
_MAVEN_PLUGIN_ANNOTATIONS_VERSION = "3.10.2"
_MAVEN_RESOLVER_API_VERSION = "1.9.18"
_MAVEN_VERSION = "3.9.6"

MAVEN_BOM_IMPORTS = maven.imports([
])

MAVEN_DEPENDENCIES = maven.dependencies([
    "org.apache.commons:commons-lang3:" + _COMMONS_LANG3_VERSION,
    "org.apache.commons:commons-text:" + _COMMONS_TEXT_VERSION,
    "org.apache.maven:maven-core:" + _MAVEN_VERSION,
    "org.apache.maven:maven-model-builder:" + _MAVEN_VERSION,
    "org.apache.maven:maven-resolver-provider:" + _MAVEN_VERSION,
    "org.apache.maven.plugin-tools:maven-plugin-annotations:" + _MAVEN_PLUGIN_ANNOTATIONS_VERSION,
    "org.apache.maven.resolver:maven-resolver-api:" + _MAVEN_RESOLVER_API_VERSION,
    "org.apache.maven.resolver:maven-resolver-connector-basic:" + _MAVEN_RESOLVER_API_VERSION,
    "org.apache.maven.resolver:maven-resolver-impl:" + _MAVEN_RESOLVER_API_VERSION,
    "org.apache.maven.resolver:maven-resolver-supplier:" + _MAVEN_RESOLVER_API_VERSION,
    "org.apache.maven.resolver:maven-resolver-transport-classpath:" + _MAVEN_RESOLVER_API_VERSION,
    "org.apache.maven.resolver:maven-resolver-transport-file:" + _MAVEN_RESOLVER_API_VERSION,
    "org.apache.maven.resolver:maven-resolver-transport-http:" + _MAVEN_RESOLVER_API_VERSION,
    "org.apache.maven.resolver:maven-resolver-util:" + _MAVEN_RESOLVER_API_VERSION,
])

MAVEN_EXCLUSIONS = [
]
