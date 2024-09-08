package com.salesforce.tools.bazel.mavendependencies.pinnedcatalog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedSet;

import com.salesforce.tools.bazel.mavendependencies.starlark.ParseException;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkFileParser;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;

/**
 * The index file of the {@link BazelDependenciesCatalog} .
 * <p>
 * This file is intentionally package private, it should not be used/modified outside of
 * {@link BazelDependenciesCatalog}.
 * </p>
 */
class BazelCatalogIndexFile {

    static class Reader extends StarlarkFileParser<BazelCatalogIndexFile> {

        public Reader(Path indexFile) throws IOException {
            super(indexFile);
        }

        @Override
        public BazelCatalogIndexFile read() throws ParseException {
            // this method is effectively a no-op
            return new BazelCatalogIndexFile();
        }

    }

    public static BazelCatalogIndexFile read(Path existingFile) throws IOException {
        return new Reader(existingFile).read();
    }

    public BazelCatalogIndexFile() {}

    public CharSequence prettyPrint(
            SortedSet<String> fileGroups,
            String catalogDirectory,
            SortedSet<String> mavenServers,
            String preamble) {
        var output = new StarlarkStringBuilder(4);

        for (String fileGroup : fileGroups) {
            var fileName = fileGroup.concat(".bzl");
            output.append("load(\"//")
                    .append(catalogDirectory)
                    .append(':')
                    .append(fileName)
                    .append("\", \"setup_maven_dependencies_")
                    .append(fileGroup)
                    .append("\", \"maven_repo_names_")
                    .append(fileGroup)
                    .append("\")")
                    .appendNewline();
        }
        output.appendNewline();

        if ((preamble != null) && !preamble.isBlank()) {
            output.append(preamble);
            output.appendNewline();
        }

        output.append("def setup_maven_dependencies(").appendNewline();
        output.increaseIndention().increaseIndention();
        output.append("maven_servers = ").appendListQuotedWithWrappingWhenNecessary(mavenServers);
        output.append("):").appendNewline();
        output.decreaseIndention();
        output.append("\"\"\"Defines all repositories for Maven dependencies.\"\"\"").appendNewline();
        output.appendNewline();
        if (!fileGroups.isEmpty()) {
            for (String fileGroup : fileGroups) {
                output.append("setup_maven_dependencies_").append(fileGroup).append("(maven_servers)").appendNewline();
            }
        } else {
            output.append("pass").appendNewline();
        }
        output.decreaseIndention();

        // a macro for all repository names
        output.appendNewline();
        output.append("def maven_repo_names():").appendNewline();
        output.increaseIndention();
        output.append("\"\"\"Returns the list of repository names of all Maven dependencies.\"\"\"").appendNewline();
        output.appendNewline();
        if (!fileGroups.isEmpty()) {
            output.append("all_repos = []").appendNewline();
            for (String fileGroup : fileGroups) {
                output.append("all_repos += maven_repo_names_").append(fileGroup).append("()").appendNewline();
            }
            output.appendNewline();
            output.append("return all_repos").appendNewline();
        } else {
            output.append("return []").appendNewline();
        }
        output.decreaseIndention();
        output.appendNewline();

        /*
         * Note: this approach exposes a big problem with long integration cycles
         * and many parallel 3pp updates.
         *
         * It literally introduces contention for a single line in the index.bzl file.
         * This is not going to work for a workspace with thousands of developers.
         *
         * Therefore the code has been deactivated and left in here for documentation
         * purposes. The original idea was to avoid changes and become smart with checksums.
         * But this is introducing scaling issues when working in a large workspace.
         */

        //		String checksum = getOriginChecksum();
        //		if (checksum != null) {
        //			output.appendNewline();
        //			output.append("_origin_checksum = ").appendQuoted(checksum).appendNewline();
        //		}

        return output.toString();
    }
}
