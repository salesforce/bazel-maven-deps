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
        artifact = "commons-collections:commons-collections:jar:3.2.1",
        artifact_sha256 = "fixme",
        artifact_sha1 = "fixme",
        fetch_sources = True,
        srcjar_sha256 = "fixme",
        srcjar_sha1 = "fixme",
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
