load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_hamcrest(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_hamcrest."""

    jvm_maven_import_external(
        name = "org_hamcrest_hamcrest",
        artifact = "org.hamcrest:hamcrest:jar:2.2",
        artifact_sha256 = "5e62846a89f05cd78cd9c1a553f340d002458380c320455dd1f8fc5497a8a1c1",
        artifact_sha1 = "1820c0968dba3a11a1b30669bb1f01978a91dedc",
        server_urls = maven_servers,
        testonly_ = True,
        srcjar_sha256 = "f49e697dbc70591f91a90dd7f741f5780f53f63f34a416d6a9879499d4d666af",
        srcjar_sha1 = "a0a13cfc629420efb587d954f982c4c6a100da25",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "org_hamcrest_hamcrest_core",
        artifact = "org.hamcrest:hamcrest-core:jar:1.3",
        artifact_sha256 = "66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9",
        artifact_sha1 = "42a25dc3219429f0e5d060061f71acb49bf010a0",
        server_urls = maven_servers,
        testonly_ = True,
        srcjar_sha256 = "e223d2d8fbafd66057a8848cc94222d63c3cedd652cc48eddc0ab5c39c0f84df",
        srcjar_sha1 = "1dc37250fbc78e23a65a67fbbaf71d2e9cbc3c0b",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_org_hamcrest():
    """Returns the list of repository names of all Maven dependencies in group org_hamcrest."""

    return [
        "org_hamcrest_hamcrest",
        "org_hamcrest_hamcrest_core",
    ]
