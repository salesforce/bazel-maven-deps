load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_codehaus(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_codehaus."""

    jvm_maven_import_external(
        name = "bazel_maven_deps__org_codehaus_plexus_plexus_cipher",
        artifact = "org.codehaus.plexus:plexus-cipher:jar:2.0",
        artifact_sha256 = "9a7f1b5c5a9effd61eadfd8731452a2f76a8e79111fac391ef75ea801bea203a",
        artifact_sha1 = "425ea8e534716b4bff1ea90f39bd76be951d651b",
        server_urls = maven_servers,
        deps = ["@javax_inject_javax_inject"],
        srcjar_sha256 = "0a98fd1e8d3e9f104f1d158946f5c063ed66af2d85127b70092003c885fe7b0c",
        srcjar_sha1 = "2550c3b8ecf65d1981b7a9096b1fa2ffb437b95d",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "bazel_maven_deps__org_codehaus_plexus_plexus_classworlds",
        artifact = "org.codehaus.plexus:plexus-classworlds:jar:2.7.0",
        artifact_sha256 = "c60ae538ba66adbc06aae205fbe2306211d3d213ab6df3239ec03cdde2458ad6",
        artifact_sha1 = "fe6f1acefd1a302dd4bc6d9d7f15358828d2a44d",
        server_urls = maven_servers,
        srcjar_sha256 = "64baec90c74f76500c556b800dd596481115fe4e7d33b5b06ed13cc0bb06af47",
        srcjar_sha1 = "d5566eb57efd1296299265e6b43e7aefc947868a",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "bazel_maven_deps__org_codehaus_plexus_plexus_component_annotations",
        artifact = "org.codehaus.plexus:plexus-component-annotations:jar:2.1.0",
        artifact_sha256 = "bde3617ce9b5bcf9584126046080043af6a4b3baea40a3b153f02e7bbc32acac",
        artifact_sha1 = "2f2147a6cc6a119a1b51a96f31d45c557f6244b9",
        server_urls = maven_servers,
        srcjar_sha256 = "3896689e1df0a4e2707ecdce4946e37c3037fbebbb3d730873c4d9dfb6d25174",
        srcjar_sha1 = "ec57f4c03eca1ebf1a9de9b49a8d54a7e4057ba7",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "bazel_maven_deps__org_codehaus_plexus_plexus_interpolation",
        artifact = "org.codehaus.plexus:plexus-interpolation:jar:1.26",
        artifact_sha256 = "b3b5412ce17889103ea564bcdfcf9fb3dfa540344ffeac6b538a73c9d7182662",
        artifact_sha1 = "25b919c664b79795ccde0ede5cee0fd68b544197",
        server_urls = maven_servers,
        srcjar_sha256 = "048ec9a9ae5fffbe8fa463824b852ea60d9cebd7397446f6a516fcde05863366",
        srcjar_sha1 = "7182d32d452bbfedfe88ecd86245ef7b90048460",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "bazel_maven_deps__org_codehaus_plexus_plexus_sec_dispatcher",
        artifact = "org.codehaus.plexus:plexus-sec-dispatcher:jar:2.0",
        artifact_sha256 = "873139960c4c780176dda580b003a2c4bf82188bdce5bb99234e224ef7acfceb",
        artifact_sha1 = "f89c5080614ffd0764e49861895dbedde1b47237",
        server_urls = maven_servers,
        deps = [
            "@bazel_maven_deps__org_codehaus_plexus_plexus_cipher",
            "@bazel_maven_deps__org_codehaus_plexus_plexus_utils",
            "@javax_inject_javax_inject",
        ],
        srcjar_sha256 = "ba4508f478d47717c8aeb41cf0ad9bc67e3c6bc7bf8f8bded2ca77b5885435a2",
        srcjar_sha1 = "76c86bce1880407b21bb614274ae23294eedb1b1",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "bazel_maven_deps__org_codehaus_plexus_plexus_utils",
        artifact = "org.codehaus.plexus:plexus-utils:jar:3.5.1",
        artifact_sha256 = "86e0255d4c879c61b4833ed7f13124e8bb679df47debb127326e7db7dd49a07b",
        artifact_sha1 = "c6bfb17c97ecc8863e88778ea301be742c62b06d",
        server_urls = maven_servers,
        srcjar_sha256 = "11b9ff95f1ade7cff0a45cf483c7cd84a8f8a542275a3d612779fffacdf43f00",
        srcjar_sha1 = "b9890d32e9293bf8e1e792d17346ff5482d3acb4",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_org_codehaus():
    """Returns the list of repository names of all Maven dependencies in group org_codehaus."""

    return [
        "bazel_maven_deps__org_codehaus_plexus_plexus_cipher",
        "bazel_maven_deps__org_codehaus_plexus_plexus_classworlds",
        "bazel_maven_deps__org_codehaus_plexus_plexus_component_annotations",
        "bazel_maven_deps__org_codehaus_plexus_plexus_interpolation",
        "bazel_maven_deps__org_codehaus_plexus_plexus_sec_dispatcher",
        "bazel_maven_deps__org_codehaus_plexus_plexus_utils",
    ]
