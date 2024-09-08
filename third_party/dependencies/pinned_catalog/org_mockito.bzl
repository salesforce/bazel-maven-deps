load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_mockito(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_mockito."""

    jvm_maven_import_external(
        name = "org_mockito_mockito_core",
        artifact = "org.mockito:mockito-core:jar:5.11.0",
        artifact_sha256 = "f076c96b1f49b8d9bc42e46b0969aaf5684c40c8b5b679d400e5d880073a0e00",
        artifact_sha1 = "e4069fa4f4ff2c94322cfec5f2e45341c6c70aff",
        server_urls = maven_servers,
        testonly_ = True,
        deps = [
            "@net_bytebuddy_byte_buddy",
            "@net_bytebuddy_byte_buddy_agent",
        ],
        runtime_deps = ["@org_objenesis_objenesis"],
        srcjar_sha256 = "83df46b0b44f232d76b40e46583334eaf2dfbe1699174bb8e76376b96cbac709",
        srcjar_sha1 = "e8cd357b9da47e69c96aadd398ff44dc1b44bb2e",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "org_mockito_mockito_junit_jupiter",
        artifact = "org.mockito:mockito-junit-jupiter:jar:5.11.0",
        artifact_sha256 = "a30ea4fe0484e54f64cdc15269a6c6ff1bd89bc26a0d41e4c27cb91cb78dc548",
        artifact_sha1 = "8e658dd339f40305ed4293db25545b5df98b171b",
        server_urls = maven_servers,
        testonly_ = True,
        deps = ["@org_mockito_mockito_core"],
        runtime_deps = ["@org_junit_jupiter_junit_jupiter_api"],
        srcjar_sha256 = "fd610a7f100a13480133db6b52c5d24eb5d34a2b895f3fbb4c4051d669f10e28",
        srcjar_sha1 = "f2541ac9fa097ce660fff3634263fb15fb4134f3",
        fetch_sources = True,
    )

def maven_repo_names_org_mockito():
    """Returns the list of repository names of all Maven dependencies in group org_mockito."""

    return [
        "org_mockito_mockito_core",
        "org_mockito_mockito_junit_jupiter",
    ]
