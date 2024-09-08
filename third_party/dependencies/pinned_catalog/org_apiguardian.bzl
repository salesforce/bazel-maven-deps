load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_apiguardian(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_apiguardian."""

    jvm_maven_import_external(
        name = "org_apiguardian_apiguardian_api",
        artifact = "org.apiguardian:apiguardian-api:jar:1.1.2",
        artifact_sha256 = "b509448ac506d607319f182537f0b35d71007582ec741832a1f111e5b5b70b38",
        artifact_sha1 = "a231e0d844d2721b0fa1b238006d15c6ded6842a",
        server_urls = maven_servers,
        testonly_ = True,
        srcjar_sha256 = "277a7a4315412817beb6655b324dc7276621e95ebff00b8bf65e17a27b685e2d",
        srcjar_sha1 = "e0787a997746ac32639e0bf3cb27af8dce8a3428",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_org_apiguardian():
    """Returns the list of repository names of all Maven dependencies in group org_apiguardian."""

    return ["org_apiguardian_apiguardian_api"]
