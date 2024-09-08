package com.salesforce.tools.bazel.mavendependencies.starlark;

import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.starlark.java.syntax.Argument;
import net.starlark.java.syntax.Argument.Keyword;
import net.starlark.java.syntax.Argument.Positional;
import net.starlark.java.syntax.CallExpression;
import net.starlark.java.syntax.Expression;
import net.starlark.java.syntax.Expression.Kind;
import net.starlark.java.syntax.Identifier;
import net.starlark.java.syntax.ListExpression;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.StringLiteral;

/**
 * Base class for parsers of Starlark files providing utility methods.
 */
public abstract class StarlarkFileParser<T> {

    protected final StarlarkFile starlarkFile;
    protected final Path starlarkFilePath;

    public StarlarkFileParser(Path starlarkFilePath) throws IOException {
        this.starlarkFilePath = starlarkFilePath;

        var input = ParserInput.readFile(starlarkFilePath.toString());
        starlarkFile = StarlarkFile.parse(input);
        if (!starlarkFile.ok()) {
            throw new IllegalArgumentException(
                    "Starlark syntax errors in file '" + starlarkFilePath + "': "
                            + starlarkFile.errors()
                                    .stream()
                                    .map(e -> e.message() + " (" + e.location().toString() + ")")
                                    .collect(joining(System.lineSeparator())));
        }
    }

    protected Map<String, Keyword> keywordArgumentsAsMap(List<Argument> arguments) {
        return arguments.stream()
                .filter(Keyword.class::isInstance)
                .map(Keyword.class::cast)
                .collect(toMap(Keyword::getName, Function.identity()));
    }

    protected boolean parseBoolArgument(Argument argument) throws ParseException {
        if (argument.getValue().kind() != Kind.IDENTIFIER) {
            throw new ParseException("expected either True or False", argument);
        }

        var identifier = ((Identifier) argument.getValue()).getName();
        if ("True".equals(identifier)) {
            return true;
        }
        if ("False".equals(identifier)) {
            return false;
        }

        throw new ParseException("expected either True or False", argument);
    }

    protected boolean parseOptionalBoolArgument(Argument argument, boolean defaultValue) throws ParseException {
        if (argument == null) {
            return defaultValue;
        }
        return parseBoolArgument(argument);
    }

    protected String parseOptionalStringArgument(Argument argument) throws ParseException {
        if (argument == null) {
            return null;
        }
        return parseStringArgument(argument);
    }

    protected List<String> parseOptionalStringListArgument(Argument argument) throws ParseException {
        if (argument == null) {
            return null;
        }

        return parseStringListArgument(argument);
    }

    protected String parseStringArgument(Argument argument) throws ParseException {
        return parseStringLiteralOrMultilineStringExpression(argument.getValue());
    }

    protected List<String> parseStringListArgument(Argument argument) throws ParseException {
        if (argument.getValue().kind() != Kind.LIST_EXPR) {
            throw new ParseException("expected list expression", argument);
        }

        return parseStringListExpression((ListExpression) argument.getValue());
    }

    protected List<String> parseStringListExpression(ListExpression listExpression) throws ParseException {
        var elements = listExpression.getElements();
        if (!elements.stream().allMatch(StringLiteral.class::isInstance)) {
            throw new ParseException(
                    "expected list of string but not all elements match string literals",
                    listExpression);
        }

        return elements.stream().map(StringLiteral.class::cast).map(StringLiteral::getValue).collect(toList());
    }

    protected String parseStringLiteralOrMultilineStringExpression(Expression expression) throws ParseException {
        if (expression.kind() == Kind.CALL) {
            // this is a multi-line string: "\n".join([..])
            var call = (CallExpression) expression;
            if (!"\"\\n\".join".equals(call.getFunction().prettyPrint())) {
                throw new ParseException(
                        "expected multi-line string \"\\n\".join or simple string literal",
                        expression);
            }
            if ((call.getArguments().size() != 1) || !(call.getArguments().get(0) instanceof Positional)) {
                throw new ParseException("\"\\n\".join must have only one positional argument", expression);
            }
            var callExpression = call.getArguments().get(0).getValue();
            if ((callExpression.kind() != Kind.LIST_EXPR) || !((ListExpression) callExpression).getElements()
                    .stream()
                    .allMatch(StringLiteral.class::isInstance)) {
                throw new ParseException("list of strings expected", callExpression);
            }
            return ((ListExpression) callExpression).getElements()
                    .stream()
                    .map(StringLiteral.class::cast)
                    .map(StringLiteral::getValue)
                    .collect(joining("\n", "", "\n" /* last entry also gets a newline */));
        }

        if (expression.kind() != Kind.STRING_LITERAL) {
            throw new ParseException("expected string literal", expression);
        }
        return ((StringLiteral) expression).getValue();
    }

    public abstract T read() throws ParseException;

    @Override
    public String toString() {
        return "StarlarkFileParser<" + getClass().getCanonicalName() + "> [" + starlarkFilePath + ", " + starlarkFile
                + "]";
    }

}
