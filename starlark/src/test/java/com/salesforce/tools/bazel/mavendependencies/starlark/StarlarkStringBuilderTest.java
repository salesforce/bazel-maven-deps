package com.salesforce.tools.bazel.mavendependencies.starlark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class StarlarkStringBuilderTest {

    @Test
    public void testAppendListAttribute_singleValue() throws Exception {
        StarlarkStringBuilder stringBuilder = new StarlarkStringBuilder(2);

        stringBuilder.appendListAttribute("a", "b");

        assertEquals("a = [\"b\"]", stringBuilder.toString());
    }

    @Test
    public void testAppendListQuotedWithWrappingWhenNecessary_empty() throws Exception {
        StarlarkStringBuilder stringBuilder = new StarlarkStringBuilder(2);

        stringBuilder.appendListQuotedWithWrappingWhenNecessary(List.of());

        assertEquals("[]", stringBuilder.toString());
    }

    @Test
    public void testAppendListQuotedWithWrappingWhenNecessary_multiple() throws Exception {
        StarlarkStringBuilder stringBuilder = new StarlarkStringBuilder(2);

        stringBuilder.appendListQuotedWithWrappingWhenNecessary(List.of("a", "b", "c"));

        assertEquals( // @formatter:off
                                 "[" + System.lineSeparator() +
                                 "  \"a\"," + System.lineSeparator() +
                                 "  \"b\"," + System.lineSeparator() +
                                 "  \"c\"," + System.lineSeparator() +
                                 "]",
                              stringBuilder.toString()); // @formatter:on
    }

    @Test
    public void testAppendListQuotedWithWrappingWhenNecessary_single_no_linebreaks() throws Exception {
        StarlarkStringBuilder stringBuilder = new StarlarkStringBuilder(2);

        stringBuilder.appendListQuotedWithWrappingWhenNecessary(List.of("single"));

        assertEquals("[\"single\"]", stringBuilder.toString());
    }

    @Test
    public void testIndention_everthing_indented() throws Exception {
        StarlarkStringBuilder stringBuilder = new StarlarkStringBuilder(2);

        stringBuilder.increaseIndention();
        stringBuilder.append('a');
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("b");
        stringBuilder.append(System.lineSeparator());

        assertEquals( // @formatter:off
                      "  a" + System.lineSeparator() +
                      "  b" + System.lineSeparator(),
                   stringBuilder.toString()); // @formatter:on
    }

    @Test
    public void testIndention_multiple_levels() throws Exception {
        StarlarkStringBuilder stringBuilder = new StarlarkStringBuilder(2);

        stringBuilder.append('a');
        stringBuilder.append(System.lineSeparator());
        stringBuilder.increaseIndention();
        stringBuilder.append("b");
        stringBuilder.append(System.lineSeparator());
        stringBuilder.increaseIndention();
        stringBuilder.append("c");
        stringBuilder.append(System.lineSeparator());
        stringBuilder.decreaseIndention();
        stringBuilder.append("d");
        stringBuilder.append(System.lineSeparator());
        stringBuilder.decreaseIndention();
        stringBuilder.append("e");
        stringBuilder.append(System.lineSeparator());

        assertEquals( // @formatter:off
                         "a" + System.lineSeparator() +
                         "  b" + System.lineSeparator() +
                         "    c" + System.lineSeparator() +
                         "  d" + System.lineSeparator() +
                         "e" + System.lineSeparator(),
                      stringBuilder.toString()); // @formatter:on
    }

    @Test
    public void testIndention_no_indention() throws Exception {
        StarlarkStringBuilder stringBuilder = new StarlarkStringBuilder(2);

        stringBuilder.append('a');
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append("b");
        stringBuilder.append(System.lineSeparator());

        assertEquals( // @formatter:off
                      "a" + System.lineSeparator() +
                      "b" + System.lineSeparator(),
                   stringBuilder.toString()); // @formatter:on
    }

}
