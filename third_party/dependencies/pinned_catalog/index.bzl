load("//third_party/dependencies/pinned_catalog:aopalliance.bzl", "setup_maven_dependencies_aopalliance", "maven_repo_names_aopalliance")
load("//third_party/dependencies/pinned_catalog:com_github.bzl", "setup_maven_dependencies_com_github", "maven_repo_names_com_github")
load("//third_party/dependencies/pinned_catalog:com_google.bzl", "setup_maven_dependencies_com_google", "maven_repo_names_com_google")
load("//third_party/dependencies/pinned_catalog:commons_codec.bzl", "setup_maven_dependencies_commons_codec", "maven_repo_names_commons_codec")
load("//third_party/dependencies/pinned_catalog:info_picocli.bzl", "setup_maven_dependencies_info_picocli", "maven_repo_names_info_picocli")
load("//third_party/dependencies/pinned_catalog:javax_annotation.bzl", "setup_maven_dependencies_javax_annotation", "maven_repo_names_javax_annotation")
load("//third_party/dependencies/pinned_catalog:javax_inject.bzl", "setup_maven_dependencies_javax_inject", "maven_repo_names_javax_inject")
load("//third_party/dependencies/pinned_catalog:junit.bzl", "setup_maven_dependencies_junit", "maven_repo_names_junit")
load("//third_party/dependencies/pinned_catalog:me_tongfei.bzl", "setup_maven_dependencies_me_tongfei", "maven_repo_names_me_tongfei")
load("//third_party/dependencies/pinned_catalog:net_bytebuddy.bzl", "setup_maven_dependencies_net_bytebuddy", "maven_repo_names_net_bytebuddy")
load("//third_party/dependencies/pinned_catalog:org_apache.bzl", "setup_maven_dependencies_org_apache", "maven_repo_names_org_apache")
load("//third_party/dependencies/pinned_catalog:org_apiguardian.bzl", "setup_maven_dependencies_org_apiguardian", "maven_repo_names_org_apiguardian")
load("//third_party/dependencies/pinned_catalog:org_checkerframework.bzl", "setup_maven_dependencies_org_checkerframework", "maven_repo_names_org_checkerframework")
load("//third_party/dependencies/pinned_catalog:org_codehaus.bzl", "setup_maven_dependencies_org_codehaus", "maven_repo_names_org_codehaus")
load("//third_party/dependencies/pinned_catalog:org_eclipse.bzl", "setup_maven_dependencies_org_eclipse", "maven_repo_names_org_eclipse")
load("//third_party/dependencies/pinned_catalog:org_fusesource.bzl", "setup_maven_dependencies_org_fusesource", "maven_repo_names_org_fusesource")
load("//third_party/dependencies/pinned_catalog:org_hamcrest.bzl", "setup_maven_dependencies_org_hamcrest", "maven_repo_names_org_hamcrest")
load("//third_party/dependencies/pinned_catalog:org_jline.bzl", "setup_maven_dependencies_org_jline", "maven_repo_names_org_jline")
load("//third_party/dependencies/pinned_catalog:org_junit.bzl", "setup_maven_dependencies_org_junit", "maven_repo_names_org_junit")
load("//third_party/dependencies/pinned_catalog:org_mockito.bzl", "setup_maven_dependencies_org_mockito", "maven_repo_names_org_mockito")
load("//third_party/dependencies/pinned_catalog:org_objenesis.bzl", "setup_maven_dependencies_org_objenesis", "maven_repo_names_org_objenesis")
load("//third_party/dependencies/pinned_catalog:org_opentest4j.bzl", "setup_maven_dependencies_org_opentest4j", "maven_repo_names_org_opentest4j")
load("//third_party/dependencies/pinned_catalog:org_slf4j.bzl", "setup_maven_dependencies_org_slf4j", "maven_repo_names_org_slf4j")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

def setup_maven_dependencies(
        maven_servers = ["https://repo1.maven.org/maven2/"]):
    """Defines all repositories for Maven dependencies."""

    setup_maven_dependencies_aopalliance(maven_servers)
    setup_maven_dependencies_com_github(maven_servers)
    setup_maven_dependencies_com_google(maven_servers)
    setup_maven_dependencies_commons_codec(maven_servers)
    setup_maven_dependencies_info_picocli(maven_servers)
    setup_maven_dependencies_javax_annotation(maven_servers)
    setup_maven_dependencies_javax_inject(maven_servers)
    setup_maven_dependencies_junit(maven_servers)
    setup_maven_dependencies_me_tongfei(maven_servers)
    setup_maven_dependencies_net_bytebuddy(maven_servers)
    setup_maven_dependencies_org_apache(maven_servers)
    setup_maven_dependencies_org_apiguardian(maven_servers)
    setup_maven_dependencies_org_checkerframework(maven_servers)
    setup_maven_dependencies_org_codehaus(maven_servers)
    setup_maven_dependencies_org_eclipse(maven_servers)
    setup_maven_dependencies_org_fusesource(maven_servers)
    setup_maven_dependencies_org_hamcrest(maven_servers)
    setup_maven_dependencies_org_jline(maven_servers)
    setup_maven_dependencies_org_junit(maven_servers)
    setup_maven_dependencies_org_mockito(maven_servers)
    setup_maven_dependencies_org_objenesis(maven_servers)
    setup_maven_dependencies_org_opentest4j(maven_servers)
    setup_maven_dependencies_org_slf4j(maven_servers)

def maven_repo_names():
    """Returns the list of repository names of all Maven dependencies."""

    all_repos = []
    all_repos += maven_repo_names_aopalliance()
    all_repos += maven_repo_names_com_github()
    all_repos += maven_repo_names_com_google()
    all_repos += maven_repo_names_commons_codec()
    all_repos += maven_repo_names_info_picocli()
    all_repos += maven_repo_names_javax_annotation()
    all_repos += maven_repo_names_javax_inject()
    all_repos += maven_repo_names_junit()
    all_repos += maven_repo_names_me_tongfei()
    all_repos += maven_repo_names_net_bytebuddy()
    all_repos += maven_repo_names_org_apache()
    all_repos += maven_repo_names_org_apiguardian()
    all_repos += maven_repo_names_org_checkerframework()
    all_repos += maven_repo_names_org_codehaus()
    all_repos += maven_repo_names_org_eclipse()
    all_repos += maven_repo_names_org_fusesource()
    all_repos += maven_repo_names_org_hamcrest()
    all_repos += maven_repo_names_org_jline()
    all_repos += maven_repo_names_org_junit()
    all_repos += maven_repo_names_org_mockito()
    all_repos += maven_repo_names_org_objenesis()
    all_repos += maven_repo_names_org_opentest4j()
    all_repos += maven_repo_names_org_slf4j()

    return all_repos

