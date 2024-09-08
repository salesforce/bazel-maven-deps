load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_aopalliance(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group aopalliance."""

    jvm_maven_import_external(
        name = "aopalliance_aopalliance",
        artifact = "aopalliance:aopalliance:jar:1.0",
        artifact_sha256 = "0addec670fedcd3f113c5c8091d783280d23f75e3acb841b61a9cdb079376a08",
        artifact_sha1 = "0235ba8b489512805ac13a8f9ea77a1ca5ebe3e8",
        server_urls = maven_servers,
        srcjar_sha256 = "e6ef91d439ada9045f419c77543ebe0416c3cdfc5b063448343417a3e4a72123",
        srcjar_sha1 = "4a4b6d692e17846a9f3da036438a7ac491d3c814",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_aopalliance():
    """Returns the list of repository names of all Maven dependencies in group aopalliance."""

    return ["aopalliance_aopalliance"]
