package com.salesforce.tools.bazel.cli;

import java.util.concurrent.atomic.AtomicInteger;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.cli.helper.ProgressMonitor;

/**
 * A special version of {@link MessagePrinter} allowing to share one for all commands and with the logging framework for
 * logging to console.
 * <p>
 * Use with care, this is really only designed for use within this tool
 * </p>
 */
final class SharedConsoleMessagePrinter extends MessagePrinter {

    /**
     * This is only guaranteed to be set during {@link BaseCommand#executeCommand(MessagePrinter)} and
     * {@link BaseCommand#afterExecuteCommand(int, MessagePrinter)}.
     * <p>
     * Used by our own logger implementation to print better output during execution
     * </p>
     */
    static volatile SharedConsoleMessagePrinter sharedMessagePrinterInstance;

    final MessagePrinter delegate;
    final AtomicInteger usageCount = new AtomicInteger(1);

    public SharedConsoleMessagePrinter(int maxRenderedLength) {
        delegate = MessagePrinter.toConsole(maxRenderedLength);
    }

    @Override
    public void close() {
        delegate.close();
        final var decrementAndGet = usageCount.decrementAndGet();
        if (decrementAndGet == 0) {
            sharedMessagePrinterInstance = null;
        }
    }

    @Override
    public void error(String text) {
        delegate.error(text);
    }

    @Override
    public void important(String text) {
        delegate.important(text);
    }

    int incrementUsageCount() {
        return usageCount.incrementAndGet();
    }

    @Override
    public void info(String text) {
        delegate.info(text);
    }

    @Override
    public void notice(String text) {
        delegate.notice(text);
    }

    @Override
    public ProgressMonitor progressMonitor(String taskName) {
        return delegate.progressMonitor(taskName);
    }

    @Override
    public void warning(String text) {
        delegate.warning(text);
    }
}