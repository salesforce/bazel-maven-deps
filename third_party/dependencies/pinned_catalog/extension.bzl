load("//third_party/dependencies/pinned_catalog:index.bzl", "maven_repo_names", "setup_maven_dependencies")

#
# Member of the Bazel dependency catalog. DO NOT EDIT.
#

#
# This file is generated using tools.
#   -> Edits will be overridden at any time.
#

# to use this add the following to your MODULE.bazel:
# maven_dependencies = use_extension("//third_party/dependencies/pinned_catalog:extension.bzl", "maven_dependencies")

def _maven_dependencies_impl(module_ctx):
    """Setup all repositories for Maven dependencies (using default servers, use Bazel downloader config to change URLs)."""

    setup_maven_dependencies()
    return module_ctx.extension_metadata(
        root_module_direct_deps = maven_repo_names(),
        root_module_direct_dev_deps = []
    )

maven_dependencies = module_extension(
    implementation = _maven_dependencies_impl,
)
