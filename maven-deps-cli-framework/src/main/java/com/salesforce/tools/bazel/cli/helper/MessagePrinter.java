package com.salesforce.tools.bazel.cli.helper;

/**
 * Prints messages either to a {@link #toConsole() console} or to a {@link #toLog()log} file.
 * <p>
 * Must be {@link #close() closed} when no longer needed.
 * </p>
 * <p>
 * Unless otherwise indicated each call automatically appends a newline at the end of any printed text.
 * </p>
 */
public abstract class MessagePrinter implements AutoCloseable {

    /**
     * @param maxRenderedLength
     *            max rendered length (set to -1 to use max terminal width)
     * @return a new {@link MessagePrinter} writing to the console
     */
    public static MessagePrinter toConsole(int maxRenderedLength) {
        return new ConsoleMessagePrinter(maxRenderedLength);
    }

    /**
     * @return a new {@link MessagePrinter} writing to the {@link UnifiedLogger}
     */
    public static MessagePrinter toLog() {
        return new Slf4jMessagePrinter(UnifiedLogger.getLogger());
    }

    /**
     * Closes the {@link MessagePrinter}.
     *
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public abstract void close(); // only RuntimeExceptions or Errors

    /**
     * Allows to report additional error information.
     * <p>
     * Note, this method should be used with care. Based on our experience it's better to fail any CLI execution in case
     * of errors with an exception and a stack trace. The CLI framework will report this properly.
     * </p>
     *
     * @param text
     *            prints out an error text
     */
    public abstract void error(String text);

    /**
     * @param text
     *            prints out an important texts
     */
    public abstract void important(String text);

    /**
     * @param text
     *            the text to print
     */
    public abstract void info(String text);

    /**
     * @param text
     *            prints out a less important texts
     */
    public abstract void notice(String text);

    public abstract ProgressMonitor progressMonitor(String taskName);

    /**
     * @param text
     *            prints out a warning text
     */
    public abstract void warning(String text);
}
