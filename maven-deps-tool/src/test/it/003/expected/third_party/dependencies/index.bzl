load("//third_party/dependencies:commons_collections.bzl", _commons_collections_boms = "MAVEN_BOM_IMPORTS", _commons_collections_deps = "MAVEN_DEPENDENCIES")

#
# Index of Maven dependencies used in the CRM Core app build
#

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Create a TODO file or GUS work item to capture technical debt.
#
# https://sfdc.co/graph-tool
#

MAVEN_BOM_IMPORTS = []
MAVEN_BOM_IMPORTS += commons_collections_boms

MAVEN_DEPENDENCIES = []
MAVEN_DEPENDENCIES += commons_collections_deps
