package com.salesforce.tools.bazel.cli.helper;

public class NullProgressMonitor implements ProgressMonitor {

    public NullProgressMonitor() {
        // empty
    }

    @Override
    public void additionalMessage(String text) {
        // no-op
    }

    @Override
    public void close() throws Exception {
        // no-op
    }

    @Override
    public void done() {
        // no-op
    }

    @Override
    public long max() {
        return -1;
    }

    @Override
    public void maxHint(long maxHint) {
        // no-op
    }

    @Override
    public void progressBy(long steps) {
        // no-op
    }
}
