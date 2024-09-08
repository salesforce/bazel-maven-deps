load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_javax_annotation(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group javax_annotation."""

    jvm_maven_import_external(
        name = "javax_annotation_javax_annotation_api",
        artifact = "javax.annotation:javax.annotation-api:jar:1.2",
        artifact_sha256 = "5909b396ca3a2be10d0eea32c74ef78d816e1b4ead21de1d78de1f890d033e04",
        artifact_sha1 = "479c1e06db31c432330183f5cae684163f186146",
        server_urls = maven_servers,
        srcjar_sha256 = "8bd08333ac2c195e224cc4063a72f4aab3c980cf5e9fb694130fad41689689d0",
        srcjar_sha1 = "ad18a02db08eaee697f812e333f692fc51129e4a",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_javax_annotation():
    """Returns the list of repository names of all Maven dependencies in group javax_annotation."""

    return ["javax_annotation_javax_annotation_api"]
