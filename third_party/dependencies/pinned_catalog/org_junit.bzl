load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_org_junit(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group org_junit."""

    jvm_maven_import_external(
        name = "org_junit_jupiter_junit_jupiter",
        artifact = "org.junit.jupiter:junit-jupiter:jar:5.10.2",
        artifact_sha256 = "263e43447f4b40f126ad6b1dcbd7df379448413bdedb8e0d240c5bcbba7c7a4f",
        artifact_sha1 = "831c0b86ddc2ce38391c5b81ea662b0cfdc02cce",
        server_urls = maven_servers,
        testonly_ = True,
        deps = [
            "@org_junit_jupiter_junit_jupiter_api",
            "@org_junit_jupiter_junit_jupiter_params",
        ],
        runtime_deps = ["@org_junit_jupiter_junit_jupiter_engine"],
        srcjar_sha256 = "bd628b06b83b0d8830b53747b7f466beebca12d4808f117660ac06e47af99d1c",
        srcjar_sha1 = "4e79ee610aff59d79b5f229252ded392b7d48fd1",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "org_junit_jupiter_junit_jupiter_api",
        artifact = "org.junit.jupiter:junit-jupiter-api:jar:5.10.2",
        artifact_sha256 = "afff77c186cd317275803872fa5133aa801fd6ac40bd91c78a6cf8009b4b17cc",
        artifact_sha1 = "fb55d6e2bce173f35fd28422e7975539621055ef",
        server_urls = maven_servers,
        testonly_ = True,
        deps = [
            "@org_apiguardian_apiguardian_api",
            "@org_junit_platform_junit_platform_commons",
            "@org_opentest4j_opentest4j",
        ],
        srcjar_sha256 = "551e054c2e84b79d087f0410c8e6e3dd3e83ae54129593380d48550da441b5ef",
        srcjar_sha1 = "b281dbb5921ce56b37b6e07a2502f663ca6c8db6",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "org_junit_jupiter_junit_jupiter_engine",
        artifact = "org.junit.jupiter:junit-jupiter-engine:jar:5.10.2",
        artifact_sha256 = "b6df35da750a546ae932376f11b3c0df841f0c90c7cb2944cd39adb432886e4b",
        artifact_sha1 = "f1f8fe97bd58e85569205f071274d459c2c4f8cd",
        server_urls = maven_servers,
        testonly_ = True,
        deps = ["@org_junit_platform_junit_platform_engine"],
        runtime_deps = [
            "@org_apiguardian_apiguardian_api",
            "@org_junit_jupiter_junit_jupiter_api",
        ],
        srcjar_sha256 = "00ffb37a2d4a1f5ab224c4cf44b2d050b3d132ca650e9c4251605daf9cd6da9b",
        srcjar_sha1 = "624f71a8d76185f2ac8d234c686bbdab0bd28ae0",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "org_junit_jupiter_junit_jupiter_params",
        artifact = "org.junit.jupiter:junit-jupiter-params:jar:5.10.2",
        artifact_sha256 = "edb1e43ff0b8067626ffb55e5e9eeca1d9ab2478141a7c7f253d115b29cc7cf2",
        artifact_sha1 = "359132c82a9d3fa87a325db6edd33b5fdc67a3d8",
        server_urls = maven_servers,
        testonly_ = True,
        deps = [
            "@org_apiguardian_apiguardian_api",
            "@org_junit_jupiter_junit_jupiter_api",
        ],
        srcjar_sha256 = "532dce2ef436152405567ea75f076dc908c4d21641224ecc53913c9f09bfb131",
        srcjar_sha1 = "1add8e4310a408675f46eae8acafe6b2b72232e5",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "org_junit_platform_junit_platform_commons",
        artifact = "org.junit.platform:junit-platform-commons:jar:1.10.2",
        artifact_sha256 = "b56a5ec000a479df4973b18bba24c98fe0db8faa14c8907d3ef451d8c71fd8ae",
        artifact_sha1 = "3197154a1f0c88da46c47a9ca27611ac7ec5d797",
        server_urls = maven_servers,
        testonly_ = True,
        deps = ["@org_apiguardian_apiguardian_api"],
        srcjar_sha256 = "e1435cf8f4843619a16c2c1393d39a5170db1376af048a76cdcd7bf9aad72ad6",
        srcjar_sha1 = "513097c1d045c39db2fa42a54f830d4358936dc3",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "org_junit_platform_junit_platform_engine",
        artifact = "org.junit.platform:junit-platform-engine:jar:1.10.2",
        artifact_sha256 = "905cba9b4998ccc29d1239085a7fb1fe0e28024d7526152356d810edec0a49a3",
        artifact_sha1 = "d53bb4e0ce7f211a498705783440614bfaf0df2e",
        server_urls = maven_servers,
        testonly_ = True,
        deps = [
            "@org_apiguardian_apiguardian_api",
            "@org_junit_platform_junit_platform_commons",
            "@org_opentest4j_opentest4j",
        ],
        srcjar_sha256 = "17ac74964fcd82c57130623afe72a99105ca107fc96cb53f169b3a8c9c444c83",
        srcjar_sha1 = "886c197f5fcfe9eaf0d1dde20aa1bd3abd653f44",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "org_junit_platform_junit_platform_launcher",
        artifact = "org.junit.platform:junit-platform-launcher:jar:1.10.2",
        artifact_sha256 = "aed4f42fb90ada9b347c231f13656fc09121ba20dab6dc646a6bd9d4da31e4aa",
        artifact_sha1 = "8125dd29e847ca274dd1a7a9ca54859acc284cb3",
        server_urls = maven_servers,
        testonly_ = True,
        deps = [
            "@org_apiguardian_apiguardian_api",
            "@org_junit_platform_junit_platform_engine",
        ],
        srcjar_sha256 = "d3c84fbec86b224e0d2bd49e9335978bdeb6aead9f17cbee4f0e2e8885c7ab89",
        srcjar_sha1 = "cab5a912975af7dc5d195970f8f1ca27f5caf35a",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "org_junit_platform_junit_platform_reporting",
        artifact = "org.junit.platform:junit-platform-reporting:jar:1.10.2",
        artifact_sha256 = "ea8f781f69b2205f5b70947eaa2182db5e7436e4abc9deae0277324ab258312d",
        artifact_sha1 = "7d7cc6890547a7535060c53506d8659d8a07ec3d",
        server_urls = maven_servers,
        testonly_ = True,
        deps = [
            "@org_apiguardian_apiguardian_api",
            "@org_junit_platform_junit_platform_launcher",
        ],
        srcjar_sha256 = "3da58799df48f4a280f4e4ea7a92d5e97d2e357ac1b98885afc04a4445457713",
        srcjar_sha1 = "7c528b954593bfaf25245dc2714b71580b344a6e",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "org_junit_platform_junit_platform_runner",
        artifact = "org.junit.platform:junit-platform-runner:jar:1.10.2",
        artifact_sha256 = "608a4333796cb97dea30b221b8b9497764b799cebfabde21146079d2710842f2",
        artifact_sha1 = "1c85444ca6c8ddcc1bd6539ef6bc45826e4952db",
        server_urls = maven_servers,
        testonly_ = True,
        deps = [
            "@junit_junit",
            "@org_apiguardian_apiguardian_api",
            "@org_junit_platform_junit_platform_launcher",
            "@org_junit_platform_junit_platform_suite_api",
        ],
        runtime_deps = ["@org_junit_platform_junit_platform_suite_commons"],
        srcjar_sha256 = "29a70e68ca06f1cd65dc4247693bbf1fbf8a92a1612ced7449c5aad5e571a30d",
        srcjar_sha1 = "5647b2932adb4821a7f26d104cd2aa57f32d33b2",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "org_junit_platform_junit_platform_suite",
        artifact = "org.junit.platform:junit-platform-suite:jar:1.10.2",
        artifact_sha256 = "632a69a72cebbda3902e23a9f96c67ccd082ead36e1fd82ea76236db654928d5",
        artifact_sha1 = "3234255109e0654a8785dcdbe1d35f8efe9fa77c",
        server_urls = maven_servers,
        testonly_ = True,
        deps = ["@org_junit_platform_junit_platform_suite_api"],
        runtime_deps = ["@org_junit_platform_junit_platform_suite_engine"],
        srcjar_sha256 = "71828fc6b56fa12daa926bed4642d9283d542509a339ca00c7ca868f1b66b8e2",
        srcjar_sha1 = "977cef92fa6cd3b233b23c8bb76a2404565d99d8",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "org_junit_platform_junit_platform_suite_api",
        artifact = "org.junit.platform:junit-platform-suite-api:jar:1.10.2",
        artifact_sha256 = "cdfc2b9b34f02fb0fb12db5e5a579c19bc111a2aed6f4c6e9a638d7a652961eb",
        artifact_sha1 = "174bba1574c37352b0eb2c06e02b6403738ad57c",
        server_urls = maven_servers,
        testonly_ = True,
        deps = [
            "@org_apiguardian_apiguardian_api",
            "@org_junit_platform_junit_platform_commons",
        ],
        srcjar_sha256 = "0da2dad4f5d546410cd9e0d88a1c556ccb4c1fbfe6812fa7c0423e881f014e3c",
        srcjar_sha1 = "30c82117d4c85668262bec729bbc6d39e3b332f0",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "org_junit_platform_junit_platform_suite_commons",
        artifact = "org.junit.platform:junit-platform-suite-commons:jar:1.10.2",
        artifact_sha256 = "d7a4e1fcb05ca0b08c5055ed363d5353fded4207b4137f2d45e156110ee97287",
        artifact_sha1 = "c4d607c0032ebd2755daf9838435788d7b7234f4",
        server_urls = maven_servers,
        testonly_ = True,
        runtime_deps = [
            "@org_apiguardian_apiguardian_api",
            "@org_junit_platform_junit_platform_engine",
            "@org_junit_platform_junit_platform_launcher",
            "@org_junit_platform_junit_platform_suite_api",
        ],
        srcjar_sha256 = "66418b3c6f94701ad4c09472b3b31de2eb2a0e3bf9e987dd657c5d7f3c07fd30",
        srcjar_sha1 = "afc22c7aca5079fe60881f424aa77d4ce81ca28f",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "org_junit_platform_junit_platform_suite_engine",
        artifact = "org.junit.platform:junit-platform-suite-engine:jar:1.10.2",
        artifact_sha256 = "d8fdbea92c7dee8573f32fe886b5adfb77ee49788be5c9a2b1e03e1e6cc7a3e2",
        artifact_sha1 = "867b962ce6eca5b105d8ae585fb5518d9d676b38",
        server_urls = maven_servers,
        testonly_ = True,
        runtime_deps = [
            "@org_apiguardian_apiguardian_api",
            "@org_junit_platform_junit_platform_engine",
            "@org_junit_platform_junit_platform_suite_api",
            "@org_junit_platform_junit_platform_suite_commons",
        ],
        srcjar_sha256 = "da79df61d98bb1f8345eccb9f1b4cc37a189a3988625552f3e97e673e17107f5",
        srcjar_sha1 = "4a7a95c8438a70efe3a1cfaa35010fe1c7f0aaca",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_org_junit():
    """Returns the list of repository names of all Maven dependencies in group org_junit."""

    return [
        "org_junit_jupiter_junit_jupiter",
        "org_junit_jupiter_junit_jupiter_api",
        "org_junit_jupiter_junit_jupiter_engine",
        "org_junit_jupiter_junit_jupiter_params",
        "org_junit_platform_junit_platform_commons",
        "org_junit_platform_junit_platform_engine",
        "org_junit_platform_junit_platform_launcher",
        "org_junit_platform_junit_platform_reporting",
        "org_junit_platform_junit_platform_runner",
        "org_junit_platform_junit_platform_suite",
        "org_junit_platform_junit_platform_suite_api",
        "org_junit_platform_junit_platform_suite_commons",
        "org_junit_platform_junit_platform_suite_engine",
    ]
