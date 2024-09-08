load("@bazel_maven_deps//bazel:jvm.bzl", "jvm_maven_import_external")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies_com_google(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines repositories for Maven dependencies in group com_google."""

    jvm_maven_import_external(
        name = "com_google_auth_google_auth_library_credentials",
        artifact = "com.google.auth:google-auth-library-credentials:jar:1.19.0",
        artifact_sha256 = "095984b0594888a47f311b3c9dcf6da9ed86feeea8f78140c55e14c27b0593e5",
        artifact_sha1 = "403601f0f42fb7099236a76188cd69c03d4fba11",
        server_urls = maven_servers,
        srcjar_sha256 = "f83533db6683adaf971f98dad16d74e8ac34909e5085b720e3c45542a3f1552c",
        srcjar_sha1 = "418050e95b6f65abc8666ca580f868b96b23bd5e",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_google_auto_value_auto_value",
        artifact = "com.google.auto.value:auto-value:jar:1.10.2",
        artifact_sha256 = "276ba82816fab66ff057e94a599c4bbdd3ab70700602b540ea17ecfe82a2986a",
        artifact_sha1 = "ec0b68a285b67ea83e54cd4cc0fb5652dcefc2e9",
        server_urls = maven_servers,
        srcjar_sha256 = "a6863414b233e03398c7e0e1cf21d98a0447d8226e21435911b896633b69b60b",
        srcjar_sha1 = "29db1e5db0fe0c804b31528141173996e71f3133",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_google_auto_value_auto_value_annotations",
        artifact = "com.google.auto.value:auto-value-annotations:jar:1.10.2",
        artifact_sha256 = "3f3b7edfaf7fbbd88642f7bd5b09487b8dcf2b9e5f3a19f1eb7b3e53f20f14ba",
        artifact_sha1 = "337954851fc17058d9b4b692b6e67e57b20e14f0",
        server_urls = maven_servers,
        srcjar_sha256 = "851f68287b05e16b1eaee48a8ee0ddcb00b63c5a4087e77085101f8c45943448",
        srcjar_sha1 = "ea66616c97c6cb765f37819f59e77217e684a3b7",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_google_code_findbugs_jsr305",
        artifact = "com.google.code.findbugs:jsr305:jar:3.0.2",
        artifact_sha256 = "766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7",
        artifact_sha1 = "25ea2e8b0c338a877313bd4672d3fe056ea78f0d",
        server_urls = maven_servers,
        srcjar_sha256 = "1c9e85e272d0708c6a591dc74828c71603053b48cc75ae83cce56912a2aa063b",
        srcjar_sha1 = "b19b5927c2c25b6c70f093767041e641ae0b1b35",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "com_google_code_gson_gson",
        artifact = "com.google.code.gson:gson:jar:2.11.0",
        artifact_sha256 = "57928d6e5a6edeb2abd3770a8f95ba44dce45f3b23b7a9dc2b309c581552a78b",
        artifact_sha1 = "527175ca6d81050b53bdd4c457a6d6e017626b0e",
        server_urls = maven_servers,
        deps = ["@com_google_errorprone_error_prone_annotations"],
        srcjar_sha256 = "49a853f71bc874ee1898a4ad5009b57d0c536e5a998b3890253ffbf4b7276ad3",
        srcjar_sha1 = "bbd772a8634d3d6fea7ffea693535a4bd1c773c0",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_google_errorprone_error_prone_annotations",
        artifact = "com.google.errorprone:error_prone_annotations:jar:2.21.1",
        artifact_sha256 = "d1f3c66aa91ac52549e00ae3b208ba4b9af7d72d68f230643553beb38e6118ac",
        artifact_sha1 = "6d9b10773b5237df178a7b3c1b4208df7d0e7f94",
        server_urls = maven_servers,
        srcjar_sha256 = "29012a51bb8d7d7bdd7db882f46dec8cccb36b40f34b6e5de2ec7dd647b372ca",
        srcjar_sha1 = "68e62df0cf0c3927fa98f6d795543ecbeb919521",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_google_flogger_flogger",
        artifact = "com.google.flogger:flogger:jar:0.8",
        artifact_sha256 = "bebe7cd82be6c8d5208d6e960cd4344ea10672132ef06f5d4c71a48ab442b963",
        artifact_sha1 = "753f5ef5b084dbff3ab3030158ed128711745b06",
        server_urls = maven_servers,
        deps = ["@org_checkerframework_checker_compat_qual"],
        srcjar_sha256 = "46ec404ace4db71b3657bae1219e58e2b4a65917afe2ba6de0b1eee0a8778e07",
        srcjar_sha1 = "7675d345986241e838fbf4f199dcd7821b286391",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_google_flogger_flogger_slf4j_backend",
        artifact = "com.google.flogger:flogger-slf4j-backend:jar:0.8",
        artifact_sha256 = "f269897dd8e5570e63fa3a2f731d0f4e2cf01cc0583de827275d7b1356fd22f4",
        artifact_sha1 = "60ff0071653d079450d087c82dc4da396e401a6f",
        server_urls = maven_servers,
        deps = [
            "@com_google_flogger_flogger",
            "@com_google_flogger_flogger_system_backend",
            "@org_slf4j_slf4j_api",
        ],
        srcjar_sha256 = "407a3b23b59ed4f8f3a41cc51baed96d92a6718e22b5e50a7049696c88f13e57",
        srcjar_sha1 = "35206b159c1b3d1584fc4ef72fcf9d861ccfa393",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_google_flogger_flogger_system_backend",
        artifact = "com.google.flogger:flogger-system-backend:jar:0.8",
        artifact_sha256 = "eb4428e483c5332381778d78c6a19da63b4fef3fa7e40f62dadabea0d7600cb4",
        artifact_sha1 = "24b2a20600b1f313540ead4b393813efa13ce14a",
        server_urls = maven_servers,
        deps = [
            "@com_google_flogger_flogger",
            "@org_checkerframework_checker_compat_qual",
        ],
        srcjar_sha256 = "8985d66e053713e4f8125dfb77256aee2d290f692dc9564d8f2bd4a6027f1655",
        srcjar_sha1 = "b08c9bac59c9fa6adf23eaa667e52c133898d5a4",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "com_google_flogger_google_extensions",
        artifact = "com.google.flogger:google-extensions:jar:0.8",
        artifact_sha256 = "0dcaf56444dd96f97713e43619cd3342b4ff8532bda21ffdf7a9eb7eb37e6c5a",
        artifact_sha1 = "42781a3d970e18c96bb0a8d3ddd94d6237aa0612",
        server_urls = maven_servers,
        deps = [
            "@com_google_flogger_flogger",
            "@com_google_flogger_flogger_system_backend",
        ],
        srcjar_sha256 = "bf52cbcbf9576a84b5652c6913f708ff55ae41cd5ad07d44a9b07ec5477a1947",
        srcjar_sha1 = "32a7abb5400cd1ae1f955c0ae0a8fb96eb14aa1c",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_google_guava_failureaccess",
        artifact = "com.google.guava:failureaccess:jar:1.0.2",
        artifact_sha256 = "8a8f81cf9b359e3f6dfa691a1e776985c061ef2f223c9b2c80753e1b458e8064",
        artifact_sha1 = "c4a06a64e650562f30b7bf9aaec1bfed43aca12b",
        server_urls = maven_servers,
        srcjar_sha256 = "dd3bfa5e2ec5bc5397efb2c3cef044c192313ff77089573667ff97a60c6978e0",
        srcjar_sha1 = "6acdafcf2dfc3b548e59dd3a4b454552aeb6ff65",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_google_guava_guava",
        artifact = "com.google.guava:guava:jar:33.0.0-jre",
        artifact_sha256 = "f4d85c3e4d411694337cb873abea09b242b664bb013320be6105327c45991537",
        artifact_sha1 = "161ba27964a62f241533807a46b8711b13c1d94b",
        server_urls = maven_servers,
        deps = [
            "@com_google_code_findbugs_jsr305",
            "@com_google_errorprone_error_prone_annotations",
            "@com_google_guava_failureaccess",
            "@com_google_guava_listenablefuture",
            "@com_google_j2objc_j2objc_annotations",
            "@org_checkerframework_checker_qual",
        ],
        srcjar_sha256 = "0c17d911785e8a606d091aa6740d6d520f307749c2bddf6e35066d52fe0036e5",
        srcjar_sha1 = "2cca40652185734d5a89349c471a54367fb98067",
        fetch_sources = True,
    )
    jvm_maven_import_external(
        name = "com_google_guava_listenablefuture",
        artifact = "com.google.guava:listenablefuture:jar:9999.0-empty-to-avoid-conflict-with-guava",
        artifact_sha256 = "b372a037d4230aa57fbeffdef30fd6123f9c0c2db85d0aced00c91b974f33f99",
        artifact_sha1 = "b421526c5f297295adef1c886e5246c39d4ac629",
        server_urls = maven_servers,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "com_google_inject_guice",
        artifact = "com.google.inject:guice:jar:5.1.0",
        artifact_sha256 = "4130e50bfac48099c860f0d903b91860c81a249c90f38245f8fed58fc817bc26",
        artifact_sha1 = "da25056c694c54ba16e78e4fc35f17fc60f0d1b4",
        server_urls = maven_servers,
        deps = [
            "@aopalliance_aopalliance",
            "@javax_inject_javax_inject",
        ],
        srcjar_sha256 = "79484227656350f8ea315198ed2ebdc8583e7ba42ecd90d367d66a7e491de52e",
        srcjar_sha1 = "3615134568ad698ea689b2cc5578c8274cad371d",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )
    jvm_maven_import_external(
        name = "com_google_j2objc_j2objc_annotations",
        artifact = "com.google.j2objc:j2objc-annotations:jar:2.8",
        artifact_sha256 = "f02a95fa1a5e95edb3ed859fd0fb7df709d121a35290eff8b74dce2ab7f4d6ed",
        artifact_sha1 = "c85270e307e7b822f1086b93689124b89768e273",
        server_urls = maven_servers,
        srcjar_sha256 = "7413eed41f111453a08837f5ac680edded7faed466cbd35745e402e13f4cc3f5",
        srcjar_sha1 = "74fb66e276fc42784d884d51f841e35232bc90de",
        fetch_sources = True,
        tags = ["not_in_collection"],
    )

def maven_repo_names_com_google():
    """Returns the list of repository names of all Maven dependencies in group com_google."""

    return [
        "com_google_auth_google_auth_library_credentials",
        "com_google_auto_value_auto_value",
        "com_google_auto_value_auto_value_annotations",
        "com_google_code_findbugs_jsr305",
        "com_google_code_gson_gson",
        "com_google_errorprone_error_prone_annotations",
        "com_google_flogger_flogger",
        "com_google_flogger_flogger_slf4j_backend",
        "com_google_flogger_flogger_system_backend",
        "com_google_flogger_google_extensions",
        "com_google_guava_failureaccess",
        "com_google_guava_guava",
        "com_google_guava_listenablefuture",
        "com_google_inject_guice",
        "com_google_j2objc_j2objc_annotations",
    ]
