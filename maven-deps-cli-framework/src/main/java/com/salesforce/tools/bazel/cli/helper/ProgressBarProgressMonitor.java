package com.salesforce.tools.bazel.cli.helper;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;

public class ProgressBarProgressMonitor implements ProgressMonitor {

    private final ProgressBar progressBar;

    public ProgressBarProgressMonitor(String taskName, int maxRenderedLength) {
        progressBar = new ProgressBarBuilder().setTaskName(taskName).setMaxRenderedLength(maxRenderedLength).build();
    }

    @Override
    public void additionalMessage(String text) {
        while (text.length() < 60) {
            text += " ";
        }
        progressBar.setExtraMessage(text);
    }

    @Override
    public void close() throws Exception {
        progressBar.close();
    }

    @Override
    public void done() {
        progressBar.stepTo(progressBar.getMax());
    }

    @Override
    public long max() {
        return progressBar.getMax();
    }

    @Override
    public void maxHint(long maxHint) {
        progressBar.maxHint(maxHint);
    }

    @Override
    public void progressBy(long steps) {
        progressBar.stepBy(steps);
    }
}
