load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_net_bytebuddy(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group net_bytebuddy."""

    jvm_maven_import_external(
        name = "net_bytebuddy_byte_buddy",
        artifact = "net.bytebuddy:byte-buddy:jar:1.14.12",
        artifact_sha256 = "970636134d61c183b19f8f58fa631e30d2f2abca344b37848a393cac7863dd70",
        artifact_sha1 = "6e37f743dc15a8d7a4feb3eb0025cbc612d5b9e1",
        server_urls = maven_servers,
        testonly_ = True,
        srcjar_sha256 = "44eb695beef187be9a3b42f0a930d50dd1e91bcc1363e3565ccc17b15a20440c",
        srcjar_sha1 = "887a4881279830607a6179560b17a44d62fc1997",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "net_bytebuddy_byte_buddy_agent",
        artifact = "net.bytebuddy:byte-buddy-agent:jar:1.14.12",
        artifact_sha256 = "2b309a9300092e0b696f7c471fd51d9969001df784c8ab9f07997437d757ad6d",
        artifact_sha1 = "be4984cb6fd1ef1d11f218a648889dfda44b8a15",
        server_urls = maven_servers,
        testonly_ = True,
        srcjar_sha256 = "4973308b6309e6ce0e98223a38c1c123c8b2429d1f1b68dea286d3c06e50a73b",
        srcjar_sha1 = "c9f6bb63cb973888eb2b2f139c6bfe5f3cc46ea3",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_net_bytebuddy():
    """Returns the list of repository names of all Maven dependencies in group net_bytebuddy."""

    return [
        "net_bytebuddy_byte_buddy",
        "net_bytebuddy_byte_buddy_agent",
    ]
