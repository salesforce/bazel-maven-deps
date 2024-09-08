load("//tools/build/bazel/sfdc/3pp:defs.bzl", "maven")

#
# Maven dependencies used in the CRM Core app build
#

# Any dependency needed in the build must be defined here.
# A tool is used to ensure transitive dependencies will be
# resolved. The versions as defined here take priority when
# resolving version conflicts.

#
# This file is manipulated using tools.
#   -> Formatting and comments will not be preserved.
#   -> Create a TODO file or GUS work item to capture technical debt.
#
# https://sfdc.co/graph-tool
#

_COMMONS_COLLECTIONS_VERSION = "3.2.1"

MAVEN_BOM_IMPORTS = maven.imports([
])

MAVEN_DEPENDENCIES = maven.dependencies([
    "commons-collections:commons-collections:" + _COMMONS_COLLECTIONS_VERSION,
])
