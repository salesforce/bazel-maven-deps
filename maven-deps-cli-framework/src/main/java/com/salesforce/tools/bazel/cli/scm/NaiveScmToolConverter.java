package com.salesforce.tools.bazel.cli.scm;

import static java.lang.String.format;

import picocli.CommandLine.ITypeConverter;

public class NaiveScmToolConverter implements ITypeConverter<NaiveScmTool> {

    @Override
    public NaiveScmTool convert(String value) throws Exception {
        if (value == null) {
            return NaiveScmTool.noop;
        }

        switch (value) {
            case "p4":
            case "perforce":
                return NaiveScmTool.p4;

            case "git":
                return NaiveScmTool.git;

            case "noop":
            case "none":
                return NaiveScmTool.noop;

            default:
                if (value.startsWith("p4:")) {
                    // special handling, expect p4:clientspec:changelist
                    final var tokens = value.split(":");
                    if (tokens.length == 2) {
                        return new NaiveScmTool.Perforce(tokens[1], null);
                    }
                    if (tokens.length == 3) {
                        return new NaiveScmTool.Perforce(tokens[1], tokens[2]);
                        // fall-through and fail
                    }
                }
                throw new IllegalArgumentException(format("SCM '%s' not supported!", value));
        }
    }
}
