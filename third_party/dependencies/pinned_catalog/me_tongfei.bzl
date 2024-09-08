load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_me_tongfei(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group me_tongfei."""

    jvm_maven_import_external(
        name = "me_tongfei_progressbar",
        artifact = "me.tongfei:progressbar:jar:0.9.5",
        artifact_sha256 = "a1a086fa66f85c49bb3ca701a78cebb33647f367d4a5be8588c784d56272cc6e",
        artifact_sha1 = "aaf7a04a9e9e6bd0aa03268cfcd6168e903bcf2d",
        server_urls = maven_servers,
        deps = ["@org_jline_jline"],
        srcjar_sha256 = "25017bc8a27758fce2ba43c4e5b63e59de3377656ffefc3f3b4070f262747030",
        srcjar_sha1 = "e221741b99afcc4d4ef05ab7da2852eaaf334558",
        fetch_sources = True,
    )

def maven_repo_names_me_tongfei():
    """Returns the list of repository names of all Maven dependencies in group me_tongfei."""

    return ["me_tongfei_progressbar"]
