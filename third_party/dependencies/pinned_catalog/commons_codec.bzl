load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_commons_codec(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group commons_codec."""

    jvm_maven_import_external(
        name = "commons_codec_commons_codec",
        artifact = "commons-codec:commons-codec:jar:1.16.0",
        artifact_sha256 = "56595fb20b0b85bc91d0d503dad50bb7f1b9afc0eed5dffa6cbb25929000484d",
        artifact_sha1 = "4e3eb3d79888d76b54e28b350915b5dc3919c9de",
        server_urls = maven_servers,
        srcjar_sha256 = "b983ff8bf2e730d3d51eb198e6d928b9c6b7843266d2106f457c5c83634c3c48",
        srcjar_sha1 = "57c67a979689051a455570196d0f48134db9909e",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_commons_codec():
    """Returns the list of repository names of all Maven dependencies in group commons_codec."""

    return ["commons_codec_commons_codec"]
