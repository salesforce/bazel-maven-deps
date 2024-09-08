load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_checkerframework(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_checkerframework."""

    jvm_maven_import_external(
        name = "org_checkerframework_checker_compat_qual",
        artifact = "org.checkerframework:checker-compat-qual:jar:2.5.3",
        artifact_sha256 = "d76b9afea61c7c082908023f0cbc1427fab9abd2df915c8b8a3e7a509bccbc6d",
        artifact_sha1 = "045f92d2e0676d05ae9297269b8268f93a875d4a",
        server_urls = maven_servers,
        srcjar_sha256 = "68011773fd60cfc7772508134086787210ba2a1443e3f9c3f5d4233a226c3346",
        srcjar_sha1 = "5853ff40085a6f44b51e6fe2d0ee95792190956d",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "org_checkerframework_checker_qual",
        artifact = "org.checkerframework:checker-qual:jar:3.37.0",
        artifact_sha256 = "e4ce1376cc2735e1dde220b62ad0913f51297704daad155a33f386bc5db0d9f7",
        artifact_sha1 = "ba74746d38026581c12166e164bb3c15e90cc4ea",
        server_urls = maven_servers,
        srcjar_sha256 = "2ca31c7e959ad82fe270b2baac11a59c570f8778191233c54927e94adab7b640",
        srcjar_sha1 = "9990309889a76e4c6556c18140ce8559b854bae3",
        fetch_sources = True,
    )

def maven_repo_names_org_checkerframework():
    """Returns the list of repository names of all Maven dependencies in group org_checkerframework."""

    return [
        "org_checkerframework_checker_compat_qual",
        "org_checkerframework_checker_qual",
    ]
