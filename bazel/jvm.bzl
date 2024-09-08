"""
Thin layer over Bazel's jvm_maven_import_external to filter unnecessary attributes and wrap into maybe.
"""

load("@bazel_tools//tools/build_defs/repo:jvm.bzl", _jvm_maven_import_external = "jvm_maven_import_external")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

_FILTER_ATTRIBUTES = (
    "srcjar_sha1",
    "artifact_sha1",
)

def jvm_maven_import_external(**kwargs):
    """A wrapper around `jvm_maven_import_external` to filter unsupported attributes and use maybe."""

    for attr in _FILTER_ATTRIBUTES:
        kwargs.pop(attr, None)

    maybe(_jvm_maven_import_external, **kwargs)
