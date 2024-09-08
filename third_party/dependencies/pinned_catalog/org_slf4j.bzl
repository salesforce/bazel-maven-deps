load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_slf4j(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_slf4j."""

    jvm_maven_import_external(
        name = "org_slf4j_jcl_over_slf4j",
        artifact = "org.slf4j:jcl-over-slf4j:jar:2.0.7",
        artifact_sha256 = "41806757e1d26dae5d6db2ca7d4a5176eed2d6e709cd86564d4a11dab0601742",
        artifact_sha1 = "f127fe5ee53404a8b3697cdd032dd1dd6a29dd77",
        server_urls = maven_servers,
        deps = ["@org_slf4j_slf4j_api"],
        srcjar_sha256 = "47476588188c4097ec315bd5ea3d74fdba29cf778b849131b7793c7280398691",
        srcjar_sha1 = "d9237c3a64be631631b746359c7cd7726b4445c5",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "org_slf4j_slf4j_api",
        artifact = "org.slf4j:slf4j-api:jar:2.0.7",
        artifact_sha256 = "5d6298b93a1905c32cda6478808ac14c2d4a47e91535e53c41f7feeb85d946f4",
        artifact_sha1 = "41eb7184ea9d556f23e18b5cb99cad1f8581fc00",
        server_urls = maven_servers,
        srcjar_sha256 = "2d6c1e7bc70fdbce8e5c6ffaaaa6673ec1a05e1cf5b9d7ae3285bf19cc81a8f1",
        srcjar_sha1 = "f887f95694cd20d51a062446b6e3d09dd02d98ff",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "org_slf4j_slf4j_simple",
        artifact = "org.slf4j:slf4j-simple:jar:2.0.7",
        artifact_sha256 = "50eae3f1cc9a78a970970518e005d3f43d5cd3262d234f47ebdf3ca3f8bc01a7",
        artifact_sha1 = "bfa4d4dad645a5b11c022ae0043bac2df6cf16b5",
        server_urls = maven_servers,
        deps = ["@org_slf4j_slf4j_api"],
        srcjar_sha256 = "39b106707861ee39c6ed19d2e8a751489f30d89ad3db06981fc169f44438df88",
        srcjar_sha1 = "3e2e41f62c6f91c9e8783f2b916d82d8981a3e6c",
        fetch_sources = True,
    )

def maven_repo_names_org_slf4j():
    """Returns the list of repository names of all Maven dependencies in group org_slf4j."""

    return [
        "org_slf4j_jcl_over_slf4j",
        "org_slf4j_slf4j_api",
        "org_slf4j_slf4j_simple",
    ]
