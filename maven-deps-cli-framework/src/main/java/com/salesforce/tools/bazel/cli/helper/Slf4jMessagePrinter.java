package com.salesforce.tools.bazel.cli.helper;

import org.slf4j.Logger;

class Slf4jMessagePrinter extends MessagePrinter {

    final static class LoggingProgressMonitor implements ProgressMonitor {

        private final Logger log;
        private final String task;

        LoggingProgressMonitor(Logger log, String task) {
            this.log = log;
            this.task = task;
        }

        @Override
        public void additionalMessage(String text) {
            log.debug("{}: {}", task, text);
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

    private final Logger log;

    public Slf4jMessagePrinter(Logger log) {
        this.log = log;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void error(String text) {
        log.error(text);
    }

    @Override
    public void important(String text) {
        log.info(text);
    }

    @Override
    public void info(String text) {
        log.info(text);
    }

    @Override
    public void notice(String text) {
        log.debug(text);
    }

    @Override
    public ProgressMonitor progressMonitor(String taskName) {
        log.info(taskName);
        return new LoggingProgressMonitor(log, taskName);
    }

    @Override
    public void warning(String text) {
        log.warn(text);
    }
}
