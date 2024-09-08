load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_info_picocli(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group info_picocli."""

    jvm_maven_import_external(
        name = "info_picocli_picocli",
        artifact = "info.picocli:picocli:jar:4.7.5",
        artifact_sha256 = "e83a906fb99b57091d1d68ac11f7c3d2518bd7a81a9c71b259e2c00d1564c8e8",
        artifact_sha1 = "a6f99ec0a97aeb3be63a9f55703b28f2cf08788f",
        server_urls = maven_servers,
        srcjar_sha256 = "46a5297a256b66d6945dd8f4f9df905fe52635bc2321e2bdb0ad22ef983c0227",
        srcjar_sha1 = "2edc75f087faa2f5dc8c6317b784737ca1373301",
        fetch_sources = True,
    )

def maven_repo_names_info_picocli():
    """Returns the list of repository names of all Maven dependencies in group info_picocli."""

    return ["info_picocli_picocli"]
