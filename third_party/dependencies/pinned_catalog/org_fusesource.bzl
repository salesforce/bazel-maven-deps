load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_fusesource(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_fusesource."""

    jvm_maven_import_external(
        name = "org_fusesource_jansi_jansi",
        artifact = "org.fusesource.jansi:jansi:jar:2.4.1",
        artifact_sha256 = "2e5e775a9dc58ffa6bbd6aa6f099d62f8b62dcdeb4c3c3bbbe5cf2301bc2dcc1",
        artifact_sha1 = "d5774f204d990c9f5da2809b88f928515577beb4",
        server_urls = maven_servers,
        srcjar_sha256 = "f707511567a13ebf8c51164133770eb5a8e023e1d391bfbc6e7a0591c71729b8",
        srcjar_sha1 = "201dbdbf78b09144d5ea566a104a37dac8c3d437",
        fetch_sources = True,
    )

def maven_repo_names_org_fusesource():
    """Returns the list of repository names of all Maven dependencies in group org_fusesource."""

    return ["org_fusesource_jansi_jansi"]
