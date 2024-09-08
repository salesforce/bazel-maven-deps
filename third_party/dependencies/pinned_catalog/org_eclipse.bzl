load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_eclipse(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_eclipse."""

    jvm_maven_import_external(
        name = "org_eclipse_sisu_org_eclipse_sisu_inject",
        artifact = "org.eclipse.sisu:org.eclipse.sisu.inject:jar:0.9.0.M2",
        artifact_sha256 = "9b62bcfc352a2ec87da8b01e37c952a54d358bbb1af3f212648aeafe7ab2dbb5",
        artifact_sha1 = "5ace70e1ea696d156f5034a42a615df13a52003a",
        server_urls = maven_servers,
        srcjar_sha256 = "071d842e8e51fb889a19997b414eff75ebb06f6d4dc79d3f062c03dc5cd2bd51",
        srcjar_sha1 = "616a449f1e1894b67b23ca64197a4cf4019e18a6",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "org_eclipse_sisu_org_eclipse_sisu_plexus",
        artifact = "org.eclipse.sisu:org.eclipse.sisu.plexus:jar:0.9.0.M2",
        artifact_sha256 = "9500d303ce467e26d129dda8559c3f3a91277d41ab49d2c4b4a5779536a62fc1",
        artifact_sha1 = "31456dd2293197bb282c03168f6767acca3dec96",
        server_urls = maven_servers,
        deps = [
            "@bazel_maven_deps__org_codehaus_plexus_plexus_classworlds",
            "@bazel_maven_deps__org_codehaus_plexus_plexus_component_annotations",
            "@bazel_maven_deps__org_codehaus_plexus_plexus_utils",
            "@javax_annotation_javax_annotation_api",
            "@org_eclipse_sisu_org_eclipse_sisu_inject",
        ],
        srcjar_sha256 = "c4dd6836110ee23aef5a5af0d0c9315782d707734aa799e8e3f3735e35bd8974",
        srcjar_sha1 = "8d79efa43d3e2a69748ad2e083efca604fe61d41",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_org_eclipse():
    """Returns the list of repository names of all Maven dependencies in group org_eclipse."""

    return [
        "org_eclipse_sisu_org_eclipse_sisu_inject",
        "org_eclipse_sisu_org_eclipse_sisu_plexus",
    ]
