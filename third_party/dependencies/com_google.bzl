load("@bazel_maven_deps//bazel:defs.bzl", "maven")

#
# Collection of Maven dependencies for this Bazel workspace
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Use a TODO file to capture additional notes/technical debt.
#

_AUTO_VALUE_VERSION = "1.10.2"
_ERROR_PRONE_ANNOTATIONS_VERSION = "2.21.1"
_FAILUREACCESS_VERSION = "1.0.2"
_FLOGGER_VERSION = "0.8"
_GOOGLE_AUTH_LIBRARY_CREDENTIALS_VERSION = "1.19.0"
_GSON_VERSION = "2.11.0"
_GUAVA_VERSION = "33.0.0-jre"

MAVEN_BOM_IMPORTS = maven.imports([
])

MAVEN_DEPENDENCIES = maven.dependencies([
    "com.google.auth:google-auth-library-credentials:" + _GOOGLE_AUTH_LIBRARY_CREDENTIALS_VERSION,
    "com.google.auto.value:auto-value:" + _AUTO_VALUE_VERSION,
    "com.google.auto.value:auto-value-annotations:" + _AUTO_VALUE_VERSION,
    "com.google.code.gson:gson:" + _GSON_VERSION,
    "com.google.errorprone:error_prone_annotations:" + _ERROR_PRONE_ANNOTATIONS_VERSION,
    "com.google.flogger:flogger:" + _FLOGGER_VERSION,
    "com.google.flogger:flogger-slf4j-backend:" + _FLOGGER_VERSION,
    "com.google.flogger:google-extensions:" + _FLOGGER_VERSION,
    "com.google.guava:failureaccess:" + _FAILUREACCESS_VERSION,
    "com.google.guava:guava:" + _GUAVA_VERSION,
])

MAVEN_EXCLUSIONS = [
]
