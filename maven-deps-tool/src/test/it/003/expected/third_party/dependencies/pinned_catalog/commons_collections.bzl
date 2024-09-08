load("//tools/build/bazel/sfdc/3pp:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#
# https://sfdc.co/graph-tool
#

def setup_maven_dependencies_commons_collections(
        maven_servers = ["https://salesforce.nexus.placeholder.to-rewrite-in.bazel_downloader.cfg/"]):
    jvm_maven_import_external(
        name = "commons_collections_commons_collections",
        artifact = "commons-collections:commons-collections:jar:3.2.2",
        artifact_sha256 = "eeeae917917144a68a741d4c0dff66aa5c5c5fd85593ff217bced3fc8ca783b8",
        artifact_sha1 = "8ad72fe39fa8c91eaaf12aadb21e0c3661fe26d5",
        fetch_sources = True,
        srcjar_sha256 = "a5b5ee16a02edadf7fe637f250217c19878bc6134f15eb55635c48996f6fed1d",
        srcjar_sha1 = "78c50ebda5784937ca1615fc0e1d0cb35857d572",
        extra_build_file_content = "\n".join([
            "",
            "",
            "# test that extra build file content is preserved in the pinned catalog",
            "genrule(",
            "    name = \"test\",",
            "    srcs = [\"@commons_collections_commons_collections\"],",
            "    outs = [\"moana-interactive-spark.proto\"],",
            "    visibility = [\"//visibility:public\"],",
            "    cmd = \"unzip -q $< moana-interactive-spark.proto; cp moana-interactive-spark.proto $@\",",
            ")",
            "",
            "",
        ]),
        server_urls = maven_servers,
    )
