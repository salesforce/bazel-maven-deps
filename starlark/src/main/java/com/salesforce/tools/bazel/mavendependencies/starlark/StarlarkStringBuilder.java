package com.salesforce.tools.bazel.mavendependencies.starlark;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A simple {@link StringBuilder} like class that provides additional helpers for appending Starlark constructs.
 * <p>
 * For example, intercepts line breaks (LF) (dropping CRs) and inserts an indention with spaces based on the current
 * indention level.
 * </p>
 */
public class StarlarkStringBuilder implements Appendable {

    private static final char DOUBLE_QUOTE = '"';
    private final StringBuilder stringBuilder = new StringBuilder();
    private final int numberOfSpacesPerIndentionLevel;

    private int currentIndentionLevel = 0;
    private boolean appendIndentionBeforeNextChar;

    public StarlarkStringBuilder(int numberOfSpacesPerIndentionLevel) {
        this.numberOfSpacesPerIndentionLevel = numberOfSpacesPerIndentionLevel;

        // on a fresh instance, the first char will trigger an indention
        appendIndentionBeforeNextChar = true;
    }

    @Override
    public StarlarkStringBuilder append(char c) {
        append((int) c);
        return this;
    }

    @Override
    public StarlarkStringBuilder append(CharSequence csq) {
        csq.chars().forEachOrdered(this::append);
        return this;
    }

    @Override
    public StarlarkStringBuilder append(CharSequence csq, int start, int end) {
        csq.subSequence(start, end).chars().forEachOrdered(this::append);
        return this;
    }

    private void append(int codePoint) {
        if (appendIndentionBeforeNextChar) {
            if (((stringBuilder.length() == 0) || (stringBuilder.charAt(stringBuilder.length() - 1) == '\n'))
                    && (codePoint != '\n')) {
                // only indent when we are not writing an empty line
                stringBuilder.append(" ".repeat(currentIndentionLevel * numberOfSpacesPerIndentionLevel));
            }
            appendIndentionBeforeNextChar = false;
        }

        switch (codePoint) {
            case '\n':
                stringBuilder.append(System.lineSeparator());
                appendIndentionBeforeNextChar = true;
                break;

            case '\r':
                // we will skip those
                break;

            default:
                stringBuilder.appendCodePoint(codePoint);
                break;
        }
    }

    public StarlarkStringBuilder appendAttribute(String attributeName, String value) {
        return append(attributeName).append(" = ").appendQuoted(value);
    }

    public StarlarkStringBuilder appendBooleanAttribute(String attributeName, boolean value) {
        return append(attributeName).append(" = ").append(asStarlarkBooleanString(value));
    }

    public StarlarkStringBuilder appendCommaFollowedByNewline() {
        return append(',').append(System.lineSeparator());
    }

    /**
     * Appends a multi-line dictionary.
     * <p>
     * NOTE: Although the {@link Map} allows {@link Object} as value types, the types should be limited to
     * {@link String}, {@link Number}, {@link Boolean} or {@link List} of those types. <code>null</code> values are not
     * permitted either.
     * </p>
     *
     * @param dict
     *            the dictionary
     * @return this instance for convenience
     */
    public StarlarkStringBuilder appendDictionaryMultiLine(Map<String, Object> dict) {
        append("{").appendNewline();
        increaseIndention();

        var entrySet = dict.entrySet();
        for (var entryStream = entrySet.iterator(); entryStream.hasNext();) {
            var entry = entryStream.next();
            var key = requireNonNull(entry.getKey(), "null key not supported");
            var value = requireNonNull(entry.getValue(), () -> "null value not supported; problematic key: " + key);

            appendQuoted(key).append(": ").appendObject(value, "key " + key);

            if (entryStream.hasNext()) {
                append(',');
            }

            appendNewline();
        }
        decreaseIndention();
        append("}");
        return this;
    }

    private StarlarkStringBuilder appendEscaped(String text) {
        text.chars().forEach(c -> {
            if (DOUBLE_QUOTE == (char) c) {
                append('\\').append(DOUBLE_QUOTE);
            } else {
                append(c);
            }
        });
        return this;
    }

    public StarlarkStringBuilder appendGlobAttribute(String attributeName, Collection<String> globPatterns) {
        append(attributeName).append(" = glob([").append(System.lineSeparator());
        increaseIndention();
        globPatterns.forEach(value -> {
            appendQuoted(value).append(",").append(System.lineSeparator());
        });
        decreaseIndention();
        append("])");
        return this;
    }

    public StarlarkStringBuilder appendGlobAttribute(
            String attributeName,
            Collection<String> globPatterns,
            String excludePattern) {
        append(attributeName).append(" = glob([").append(System.lineSeparator());
        increaseIndention();
        increaseIndention();
        globPatterns.forEach(value -> {
            appendQuoted(value).append(",").append(System.lineSeparator());
        });
        decreaseIndention();
        append("]").appendCommaFollowedByNewline();
        appendListAttribute("exclude", List.of("**/*.java")).appendCommaFollowedByNewline();
        decreaseIndention();
        append(")");
        return this;
    }

    public StarlarkStringBuilder appendListAttribute(String attributeName, Collection<String> listValues) {
        return append(attributeName).append(" = ").appendListQuotedWithWrappingWhenNecessary(listValues);
    }

    public StarlarkStringBuilder appendListAttribute(String name, String singleListValue) {
        return appendListAttribute(name, List.of(singleListValue));
    }

    public StarlarkStringBuilder appendListQuotedMultiLine(Stream<String> listOfStrings) {
        append("[").append(System.lineSeparator());
        increaseIndention();
        listOfStrings.forEach(value -> {
            appendQuoted(value).append(",").append(System.lineSeparator());
        });
        decreaseIndention();
        append("]");
        return this;
    }

    public StarlarkStringBuilder appendListQuotedNoNewline(List<String> listOfStrings) {
        append("[");
        for (var stream = listOfStrings.iterator(); stream.hasNext();) {
            var value = stream.next();
            appendQuoted(value);
            if (stream.hasNext()) {
                append(", ");
            }
        }
        append("]");
        return this;
    }

    public StarlarkStringBuilder appendListQuotedSingleItemNoNewline(String singleValue) {
        return append('[').append(DOUBLE_QUOTE).append(singleValue).append(DOUBLE_QUOTE).append(']');
    }

    public StarlarkStringBuilder appendListQuotedWithWrappingWhenNecessary(Collection<String> listOfStrings) {
        if (listOfStrings.size() > 1) {
            return appendListQuotedMultiLine(listOfStrings.stream());
        }
        if (listOfStrings.size() == 1) {
            return appendListQuotedSingleItemNoNewline(listOfStrings.stream().findFirst().get());
        }

        return append("[]"); // empty list
    }

    public StarlarkStringBuilder appendNewline() {
        append(System.lineSeparator());
        return this;
    }

    private StarlarkStringBuilder appendObject(Object value, String positionalIndexForErrorReporting) {
        if (value instanceof String) {
            appendQuoted((String) value);
        } else if (value instanceof List) {
            appendObjectListMultiLineWhenNecessary((List<?>) value, positionalIndexForErrorReporting);
        } else if (value instanceof Boolean) {
            append(Boolean.TRUE.equals(value) ? "True" : "False");
        } else if (value instanceof Number) {
            append(((Number) value).toString());
        } else if (value instanceof Map) {
            if (!((Map<?, ?>) value).keySet().stream().allMatch(String.class::isInstance)) {
                throw new IllegalArgumentException(
                        "only Map<String,Object> supported as value; problematic index: "
                                + positionalIndexForErrorReporting);
            }
            @SuppressWarnings("unchecked")
            var map = (Map<String, Object>) value;
            appendDictionaryMultiLine(map);
        } else {
            throw new IllegalArgumentException(
                    format(
                        "unsupported value type '%s'; problematic index: %s",
                        value.getClass().getSimpleName(),
                        positionalIndexForErrorReporting));
        }
        return this;
    }

    private StarlarkStringBuilder appendObjectListMultiLineWhenNecessary(
            List<?> listOfObjects,
            String positionalIndexForErrorReporting) {
        if (listOfObjects.size() > 1) {
            append("[").appendNewline();
            increaseIndention();
            for (var i = 0; i < listOfObjects.size(); i++) {
                Object value = listOfObjects.get(i);
                appendObject(value, positionalIndexForErrorReporting + ", index " + positionalIndexForErrorReporting);
                if ((i + 1) < listOfObjects.size()) {
                    append(',');
                }
                appendNewline(); // new-line after each value (also the last
                                 // one)
            }
            decreaseIndention();
            append("]");
            return this;
        }
        if (listOfObjects.size() == 1) {
            return append("[").appendObject(listOfObjects.stream().findFirst().get(), positionalIndexForErrorReporting)
                    .append("]");
        }

        return append("[]"); // empty list
    }

    public StarlarkStringBuilder appendQuoted(String text) {
        if (text.indexOf('\n') == -1) {
            return append(DOUBLE_QUOTE).appendEscaped(text).append(DOUBLE_QUOTE);
        }

        // special handling for multiple line string
        return append(DOUBLE_QUOTE).append("\\n")
                .append(DOUBLE_QUOTE)
                .append(".join(")
                .appendListQuotedMultiLine(text.lines())
                .append(')');
    }

    public StarlarkStringBuilder appendWithoutIndentionHandling(CharSequence csq) {
        stringBuilder.append(csq);
        return this;
    }

    private String asStarlarkBooleanString(boolean value) {
        if (value) {
            return "True";
        }
        return "False";
    }

    public StarlarkStringBuilder decreaseIndention() {
        currentIndentionLevel--;
        if (currentIndentionLevel < 0) {
            throw new IllegalStateException(
                    "decreaseIdention called more often than increaseIdention; check your code for errors");
        }
        return this;
    }

    public int getNumberOfSpacesPerIndentionLevel() {
        return numberOfSpacesPerIndentionLevel;
    }

    public StarlarkStringBuilder increaseIndention() {
        currentIndentionLevel++;
        return this;
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }
}
