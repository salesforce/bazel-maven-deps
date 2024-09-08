load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_objenesis(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_objenesis."""

    jvm_maven_import_external(
        name = "org_objenesis_objenesis",
        artifact = "org.objenesis:objenesis:jar:3.3",
        artifact_sha256 = "02dfd0b0439a5591e35b708ed2f5474eb0948f53abf74637e959b8e4ef69bfeb",
        artifact_sha1 = "1049c09f1de4331e8193e579448d0916d75b7631",
        server_urls = maven_servers,
        testonly_ = True,
        srcjar_sha256 = "d06164f8ca002c8ef193cef2d682822014dd330505616af93a3fb64226fc131d",
        srcjar_sha1 = "5fef34eeee6816b0ba2170755a8a9db7744990c3",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_org_objenesis():
    """Returns the list of repository names of all Maven dependencies in group org_objenesis."""

    return ["org_objenesis_objenesis"]
