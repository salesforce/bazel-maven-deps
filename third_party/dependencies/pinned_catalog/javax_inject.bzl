load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_javax_inject(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group javax_inject."""

    jvm_maven_import_external(
        name = "javax_inject_javax_inject",
        artifact = "javax.inject:javax.inject:jar:1",
        artifact_sha256 = "91c77044a50c481636c32d916fd89c9118a72195390452c81065080f957de7ff",
        artifact_sha1 = "6975da39a7040257bd51d21a231b76c915872d38",
        server_urls = maven_servers,
        srcjar_sha256 = "c4b87ee2911c139c3daf498a781967f1eb2e75bc1a8529a2e7b328a15d0e433e",
        srcjar_sha1 = "a00123f261762a7c5e0ec916a2c7c8298d29c400",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_javax_inject():
    """Returns the list of repository names of all Maven dependencies in group javax_inject."""

    return ["javax_inject_javax_inject"]
