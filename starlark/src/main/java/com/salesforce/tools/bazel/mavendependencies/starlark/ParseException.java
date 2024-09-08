package com.salesforce.tools.bazel.mavendependencies.starlark;

import static java.lang.String.format;

import java.io.IOException;

import net.starlark.java.syntax.Node;

public class ParseException extends IOException {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public ParseException(String message, Node node) {
        super(format("Invalid file: %s (%s)%n %1.50s", message, node.getStartLocation(), node));
    }

}