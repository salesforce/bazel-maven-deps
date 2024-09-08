load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

def setup_maven_dependencies_catalog_roundtrip_test(
        maven_servers = []):
    """Defines repositories for Maven dependencies in group catalog_roundtrip_test."""

    jvm_maven_import_external(
        name = "com_google_guava",
        artifact = "com.google.guava:guava:jar:31.0.1",
        artifact_sha256 = "sha256 for artifact",
        artifact_sha1 = "sha1 for artifact",
        server_urls = maven_servers,
        extra_build_file_content = "\n".join([
            "java_library(",
            "    name = \"processor\",",
            "    exports = [\":jar\"],",
            "    runtime_deps = [",
            "        \"@com_google_auto_common\",",
            "        \"@com_google_auto_value\",",
            "        \"@com_google_guava\",",
            "        \"@com_google_java_format\",",
            "        \"@com_squareup_javapoet\",",
            "        \"@javax_inject\",",
            "    ],",
            ")",
            "",
            "java_plugin(",
            "    name = \"AutoFactoryProcessor\",",
            "    output_licenses = [\"unencumbered\"],",
            "    processor_class = \"com.google.auto.factory.processor.AutoFactoryProcessor\",",
            "    generates_api = 1,",
            "    tags = [\"annotation=com.google.auto.factory.AutoFactory;genclass=${package}.${outerclasses}@{className|${classname}Factory}\"],",
            "    deps = [\":processor\"],",
            ")",
            "",
            "java_library(",
            "    name = \"com_google_auto_factory\",",
            "    exported_plugins = [\":AutoFactoryProcessor\"],",
            "    exports = [",
            "        \":jar\",",
            "        \"@com_google_code_findbugs_jsr305\",",
            "        \"@javax_annotation_jsr250_api\",",
            "        \"@javax_inject\",",
            "    ],",
            ")",
            "",
            "",
        ]),
        licenses = ["notice"],
    )
    jvm_maven_import_external(
        name = "some_other_name",
        artifact = "random:coordinate:jar:1.2.3",
        artifact_sha256 = "1234567890",
        artifact_sha1 = "11111",
        server_urls = maven_servers,
        extra_build_file_content = "\n".join([
            "",
            "",
            "# test that extra build file content is preserved in the pinned catalog",
            "genrule(",
            "    name = \"test\",",
            "    srcs = [\"@its_me\"],",
            "    outs = [\"hello.proto\"],",
            "    visibility = [\"//visibility:public\"],",
            "    cmd = \"unzip -q $< hello.proto; cp hello.proto $@\",",
            ")",
            "",
            "",
        ]),
        neverlink = True,
        srcjar_sha256 = "srcsha256",
        srcjar_sha1 = "srcsha1",
        fetch_sources = True,
        tags = [
            "hello",
            "tag2",
        ],
    )

def maven_repo_names_catalog_roundtrip_test():
    """Returns the list of repository names of all Maven dependencies in group catalog_roundtrip_test."""

    return [
        "com_google_guava",
        "some_other_name",
    ]
