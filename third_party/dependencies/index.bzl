load("//third_party/dependencies:com_github.bzl", _com_github_boms = "MAVEN_BOM_IMPORTS", _com_github_deps = "MAVEN_DEPENDENCIES", _com_github_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:com_google.bzl", _com_google_boms = "MAVEN_BOM_IMPORTS", _com_google_deps = "MAVEN_DEPENDENCIES", _com_google_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:info_picocli.bzl", _info_picocli_boms = "MAVEN_BOM_IMPORTS", _info_picocli_deps = "MAVEN_DEPENDENCIES", _info_picocli_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:me_tongfei.bzl", _me_tongfei_boms = "MAVEN_BOM_IMPORTS", _me_tongfei_deps = "MAVEN_DEPENDENCIES", _me_tongfei_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:org_apache.bzl", _org_apache_boms = "MAVEN_BOM_IMPORTS", _org_apache_deps = "MAVEN_DEPENDENCIES", _org_apache_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:org_checkerframework.bzl", _org_checkerframework_boms = "MAVEN_BOM_IMPORTS", _org_checkerframework_deps = "MAVEN_DEPENDENCIES", _org_checkerframework_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:org_fusesource.bzl", _org_fusesource_boms = "MAVEN_BOM_IMPORTS", _org_fusesource_deps = "MAVEN_DEPENDENCIES", _org_fusesource_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:org_hamcrest.bzl", _org_hamcrest_boms = "MAVEN_BOM_IMPORTS", _org_hamcrest_deps = "MAVEN_DEPENDENCIES", _org_hamcrest_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:org_junit.bzl", _org_junit_boms = "MAVEN_BOM_IMPORTS", _org_junit_deps = "MAVEN_DEPENDENCIES", _org_junit_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:org_mockito.bzl", _org_mockito_boms = "MAVEN_BOM_IMPORTS", _org_mockito_deps = "MAVEN_DEPENDENCIES", _org_mockito_exclusions = "MAVEN_EXCLUSIONS")
load("//third_party/dependencies:org_slf4j.bzl", _org_slf4j_boms = "MAVEN_BOM_IMPORTS", _org_slf4j_deps = "MAVEN_DEPENDENCIES", _org_slf4j_exclusions = "MAVEN_EXCLUSIONS")

#
# Index of Maven dependencies used in the CRM Core App build
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Create a TODO file or GUS work item to capture technical debt.
#
# https://sfdc.co/graph-tool
#


MAVEN_BOM_IMPORTS = []
MAVEN_BOM_IMPORTS += com_github_boms
MAVEN_BOM_IMPORTS += com_google_boms
MAVEN_BOM_IMPORTS += info_picocli_boms
MAVEN_BOM_IMPORTS += me_tongfei_boms
MAVEN_BOM_IMPORTS += org_apache_boms
MAVEN_BOM_IMPORTS += org_checkerframework_boms
MAVEN_BOM_IMPORTS += org_fusesource_boms
MAVEN_BOM_IMPORTS += org_hamcrest_boms
MAVEN_BOM_IMPORTS += org_junit_boms
MAVEN_BOM_IMPORTS += org_mockito_boms
MAVEN_BOM_IMPORTS += org_slf4j_boms

MAVEN_DEPENDENCIES = []
MAVEN_DEPENDENCIES += com_github_deps
MAVEN_DEPENDENCIES += com_google_deps
MAVEN_DEPENDENCIES += info_picocli_deps
MAVEN_DEPENDENCIES += me_tongfei_deps
MAVEN_DEPENDENCIES += org_apache_deps
MAVEN_DEPENDENCIES += org_checkerframework_deps
MAVEN_DEPENDENCIES += org_fusesource_deps
MAVEN_DEPENDENCIES += org_hamcrest_deps
MAVEN_DEPENDENCIES += org_junit_deps
MAVEN_DEPENDENCIES += org_mockito_deps
MAVEN_DEPENDENCIES += org_slf4j_deps

MAVEN_EXCLUSIONS = []
MAVEN_EXCLUSIONS += com_github_exclusions
MAVEN_EXCLUSIONS += com_google_exclusions
MAVEN_EXCLUSIONS += info_picocli_exclusions
MAVEN_EXCLUSIONS += me_tongfei_exclusions
MAVEN_EXCLUSIONS += org_apache_exclusions
MAVEN_EXCLUSIONS += org_checkerframework_exclusions
MAVEN_EXCLUSIONS += org_fusesource_exclusions
MAVEN_EXCLUSIONS += org_hamcrest_exclusions
MAVEN_EXCLUSIONS += org_junit_exclusions
MAVEN_EXCLUSIONS += org_mockito_exclusions
MAVEN_EXCLUSIONS += org_slf4j_exclusions
