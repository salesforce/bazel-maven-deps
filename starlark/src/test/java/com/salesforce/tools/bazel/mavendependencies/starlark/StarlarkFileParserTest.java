package com.salesforce.tools.bazel.mavendependencies.starlark;

import static java.nio.file.Files.writeString;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.starlark.java.syntax.AssignmentStatement;
import net.starlark.java.syntax.Expression.Kind;
import net.starlark.java.syntax.Identifier;

public class StarlarkFileParserTest {

	@TempDir
	Path tempDir;

	public static class TestableStarlarkFileParser extends StarlarkFileParser<String> {
		public TestableStarlarkFileParser(Path starlarkFilePath) throws IOException {
			super(starlarkFilePath);
		}

		@Override
		public String read() throws ParseException {
			fail("Not expected to call this method!");
			return null;
		}
	}

	@Test
	public void parse_multiline_string() throws IOException {
		TestableStarlarkFileParser parser = createParser("# empty " + System.lineSeparator() + System.lineSeparator()
				+ "extra_build_file_content = \"\\n\".join([\n" + System.lineSeparator()
				+ "            \"\",\n"
				+ "            \"\",\n"
				+ "            \"# test that extra build file content is preserved in the pinned catalog\",\n"
				+ "            \"genrule(\",\n"
				+ "            \"    name = \\\"test\\\",\",\n"
				+ "            \"    srcs = [\\\"@commons_collections_commons_collections\\\"],\",\n"
				+ "            \"    outs = [\\\"moana-interactive-spark.proto\\\"],\",\n"
				+ "            \"    visibility = [\\\"//visibility:public\\\"],\",\n"
				+ "            \"    cmd = \\\"unzip -q $< moana-interactive-spark.proto; cp moana-interactive-spark.proto $@\\\",\",\n"
				+ "            \")\",\n"
				+ "            \"\",\n"
				+ "            \"\",\n"
				+ "        ])");

		List<AssignmentStatement> assignments = parser.starlarkFile.getStatements()
				.stream()
				.filter(AssignmentStatement.class::isInstance)
				.map(AssignmentStatement.class::cast)
				.collect(toList());
		for (AssignmentStatement assignment : assignments) {
			if (assignment.getLHS().kind() != Kind.IDENTIFIER)
				throw new ParseException("left hand side of assignment must be an identifier", assignment.getLHS());

			String identifier = ((Identifier) assignment.getLHS()).getName();
			assertEquals("extra_build_file_content", identifier);

			String value = parser.parseStringLiteralOrMultilineStringExpression(assignment.getRHS());

			assertEquals("\n"
					+ "\n"
					+ "# test that extra build file content is preserved in the pinned catalog\n"
					+ "genrule(\n"
					+ "    name = \"test\",\n"
					+ "    srcs = [\"@commons_collections_commons_collections\"],\n"
					+ "    outs = [\"moana-interactive-spark.proto\"],\n"
					+ "    visibility = [\"//visibility:public\"],\n"
					+ "    cmd = \"unzip -q $< moana-interactive-spark.proto; cp moana-interactive-spark.proto $@\",\n"
					+ ")\n"
					+ "\n"
					+ "\n", value);

		}


	}

	private TestableStarlarkFileParser createParser(String content) throws IOException {
		Path starlarkFile = tempDir.resolve("test_" + System.nanoTime() + ".bzl");
		writeString(starlarkFile, content);
		return new TestableStarlarkFileParser(starlarkFile);
	}

}
