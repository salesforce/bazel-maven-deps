package com.salesforce.tools.bazel.mavendependencies.collection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.SortedSet;

import com.salesforce.tools.bazel.mavendependencies.starlark.ParseException;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkFileParser;
import com.salesforce.tools.bazel.mavendependencies.starlark.StarlarkStringBuilder;

/**
 * The index file of the {@link MavenDependenciesCollection} .
 * <p>
 * This file is intentionally package private, it should not be used/modified outside of
 * {@link MavenDependenciesCollection}.
 * </p>
 */
class MavenDependenciesCollectionIndexFile {

    static class Reader extends StarlarkFileParser<MavenDependenciesCollectionIndexFile> {

        public Reader(Path indexFile) throws IOException {
            super(indexFile);
        }

        @Override
        public MavenDependenciesCollectionIndexFile read() throws ParseException {
            // ignore the content in the file
            return new MavenDependenciesCollectionIndexFile();
        }

    }

    public static MavenDependenciesCollectionIndexFile read(Path existingFile) throws IOException {
        return new Reader(existingFile).read();
    }

    public MavenDependenciesCollectionIndexFile() {}

    public CharSequence prettyPrint(SortedSet<String> fileGroups, String collectionDirectory) {
        var output = new StarlarkStringBuilder(4);

        for (String fileGroup : fileGroups) {
            var fileName = fileGroup.concat(".bzl");
            output.append("load(\"//")
                    .append(collectionDirectory)
                    .append(':')
                    .append(fileName)
                    .append("\", _")
                    .append(fileGroup)
                    .append("_boms = \"MAVEN_BOM_IMPORTS\", _")
                    .append(fileGroup)
                    .append("_deps = \"MAVEN_DEPENDENCIES\", _")
                    .append(fileGroup)
                    .append("_exclusions = \"MAVEN_EXCLUSIONS\")")
                    .appendNewline();
        }
        output.appendNewline();
        output.append("#").appendNewline();
        output.append("# Index of Maven dependencies used in the CRM Core App build").appendNewline();
        output.append("#").appendNewline();
        output.appendNewline();
        output.append("#").appendNewline();
        output.append("# This file is manipulated using tools.").appendNewline();
        output.append("#   -> Formatting and comments will not be preserved.").appendNewline();
        output.append("#   -> Create a TODO file or GUS work item to capture technical debt.").appendNewline();
        output.append("#").appendNewline();
        output.append("# https://sfdc.co/graph-tool").appendNewline();
        output.append("#").appendNewline();

        output.appendNewline();
        output.appendNewline();
        output.append("MAVEN_BOM_IMPORTS = []").appendNewline();
        for (String fileGroup : fileGroups) {
            output.append("MAVEN_BOM_IMPORTS += ").append(fileGroup).append("_boms").appendNewline();
        }
        output.appendNewline();
        output.append("MAVEN_DEPENDENCIES = []").appendNewline();
        for (String fileGroup : fileGroups) {
            output.append("MAVEN_DEPENDENCIES += ").append(fileGroup).append("_deps").appendNewline();
        }
        output.appendNewline();
        output.append("MAVEN_EXCLUSIONS = []").appendNewline();
        for (String fileGroup : fileGroups) {
            output.append("MAVEN_EXCLUSIONS += ").append(fileGroup).append("_exclusions").appendNewline();
        }

        return output.toString();
    }
}
