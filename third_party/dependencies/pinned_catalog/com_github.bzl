load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_com_github(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group com_github."""

    jvm_maven_import_external(
        name = "com_github_ben_manes_caffeine_caffeine",
        artifact = "com.github.ben-manes.caffeine:caffeine:jar:3.1.8",
        artifact_sha256 = "7dd15f9df1be238ffaa367ce6f556737a88031de4294dad18eef57c474ddf1d3",
        artifact_sha1 = "24795585df8afaf70a2cd534786904ea5889c047",
        server_urls = maven_servers,
        deps = [
            "@com_google_errorprone_error_prone_annotations",
            "@org_checkerframework_checker_qual",
        ],
        srcjar_sha256 = "7c8237f5d8f23654e7091056316a3730636b7a0f2e6fce450e2bd522090d6b7f",
        srcjar_sha1 = "352209f02df83046de6ebfd647b22d726ca3a954",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_github_ben_manes_caffeine_guava",
        artifact = "com.github.ben-manes.caffeine:guava:jar:3.1.8",
        artifact_sha256 = "e45c7c2db18810644c12bb3396cd38dbf4efaa1fa2402f27aaef6e662d8a0af5",
        artifact_sha1 = "b31d9027ac6f6793aaea27ef6dc6fed1b4120ccd",
        server_urls = maven_servers,
        deps = [
            "@com_github_ben_manes_caffeine_caffeine",
            "@com_google_guava_guava",
        ],
        srcjar_sha256 = "990a975c90d7070fc11c035474577ebd0a9ebf93e30bcb17abd413804f60f14b",
        srcjar_sha1 = "e2a144d58c38e6613dc3359d55d918d1eed600ca",
        fetch_sources = True,
    )

def maven_repo_names_com_github():
    """Returns the list of repository names of all Maven dependencies in group com_github."""

    return [
        "com_github_ben_manes_caffeine_caffeine",
        "com_github_ben_manes_caffeine_guava",
    ]
