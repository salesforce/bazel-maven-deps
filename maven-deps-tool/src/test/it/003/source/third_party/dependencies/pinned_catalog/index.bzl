load("//third_party/dependencies/pinned_catalog:commons_collections.bzl", "setup_maven_dependencies_commons_collections")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#
# https://sfdc.co/graph-tool
#

def setup_maven_dependencies(
        maven_servers = ["https://salesforce.nexus.placeholder.to-rewrite-in.bazel_downloader.cfg/"]):
    setup_maven_dependencies_commons_collections(maven_servers)
