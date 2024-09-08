package com.salesforce.tools.bazel.mavendependencies.pinnedcatalog;

import java.io.IOException;
import java.nio.file.Path;

import com.salesforce.tools.bazel.mavendependencies.starlark.ParseException;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkFileParser;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;

/**
 * The extension file of the {@link BazelDependenciesCatalog} .
 * <p>
 * This class is intentionally package private, it should not be used/modified outside of
 * {@link BazelDependenciesCatalog}.
 * </p>
 */
class BazelCatalogModuleExtensionFile {

    static class Reader extends StarlarkFileParser<BazelCatalogModuleExtensionFile> {

        public Reader(Path indexFile) throws IOException {
            super(indexFile);
        }

        @Override
        public BazelCatalogModuleExtensionFile read() throws ParseException {
            // this method is effectively a no-op
            return new BazelCatalogModuleExtensionFile();
        }

    }

    public static BazelCatalogModuleExtensionFile read(Path existingFile) throws IOException {
        return new Reader(existingFile).read();
    }

    public BazelCatalogModuleExtensionFile() {}

    public CharSequence prettyPrint(String catalogDirectory, String preamble) {
        var output = new StarlarkStringBuilder(4);

        output.append("load(\"//")
                .append(catalogDirectory)
                .append(":index.bzl\", \"maven_repo_names\", \"setup_maven_dependencies\")")
                .appendNewline();
        output.appendNewline();

        if ((preamble != null) && !preamble.isBlank()) {
            output.append(preamble);
            output.appendNewline();
        }

        output.append("# to use this add the following to your MODULE.bazel:").appendNewline();
        output.append("# maven_dependencies = use_extension(\"//")
                .append(catalogDirectory)
                .append(":extension.bzl\", \"maven_dependencies\")")
                .appendNewline();

        output.appendNewline();
        output.append("def _maven_dependencies_impl(module_ctx):").appendNewline();
        output.increaseIndention();
        output.append(
            "\"\"\"Setup all repositories for Maven dependencies (using default servers, use Bazel downloader config to change URLs).\"\"\"")
                .appendNewline();
        output.appendNewline();
        output.append("setup_maven_dependencies()").appendNewline();
        output.append("return module_ctx.extension_metadata(").appendNewline();
        output.increaseIndention();
        output.append("root_module_direct_deps = maven_repo_names(),").appendNewline();
        output.append("root_module_direct_dev_deps = []").appendNewline();
        output.decreaseIndention();
        output.append(")").appendNewline();
        output.decreaseIndention();
        output.appendNewline();

        output.append("maven_dependencies = module_extension(").appendNewline();
        output.increaseIndention();
        output.append("implementation = _maven_dependencies_impl,").appendNewline();
        output.decreaseIndention();
        output.append(")").appendNewline();
        return output.toString();
    }
}
