load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_jline(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_jline."""

    jvm_maven_import_external(
        name = "org_jline_jline",
        artifact = "org.jline:jline:jar:3.21.0",
        artifact_sha256 = "1e7d63a2bd1c26354ca1987e55469ea4327c4a3845c10d7a7790ca9729c49c02",
        artifact_sha1 = "2bf6f2311356f309fda0412e9389d2499346b5a1",
        server_urls = maven_servers,
        srcjar_sha256 = "9d1f5958a0cff8f0b1729cd10b3bbe71d2587a0ec9537ece87cca45d21ba3db3",
        srcjar_sha1 = "633b5478dfc8fed93eb223bcfb1c101bfbba4362",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_org_jline():
    """Returns the list of repository names of all Maven dependencies in group org_jline."""

    return ["org_jline_jline"]
