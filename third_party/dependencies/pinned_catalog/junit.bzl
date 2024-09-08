load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_junit(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group junit."""

    jvm_maven_import_external(
        name = "junit_junit",
        artifact = "junit:junit:jar:4.13.2",
        artifact_sha256 = "8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3",
        artifact_sha1 = "8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12",
        server_urls = maven_servers,
        testonly_ = True,
        deps = ["@org_hamcrest_hamcrest_core"],
        srcjar_sha256 = "34181df6482d40ea4c046b063cb53c7ffae94bdf1b1d62695bdf3adf9dea7e3a",
        srcjar_sha1 = "33987872a811fe4d4001ed494b07854822257f42",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_junit():
    """Returns the list of repository names of all Maven dependencies in group junit."""

    return ["junit_junit"]
