package com.salesforce.tools.bazel.cli;

import static com.salesforce.tools.bazel.cli.SharedConsoleMessagePrinter.sharedMessagePrinterInstance;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.cli.helper.UnifiedLogger;
import com.salesforce.tools.bazel.cli.scm.NaiveScmTool;
import com.salesforce.tools.bazel.cli.scm.NaiveScmToolConverter;

import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Help;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

/**
 * Base class for CLI commands using PicoCLI.
 * <p>
 * Command can use one of the static <code>execute</code> methods to benefit from some consistent behavior.
 * </p>
 */
public abstract class BaseCommand implements Callable<Integer> {

    /**
     * Extends StringWriter to use ColorScheme. Allows separating exception messages from stack traces by intercepting
     * write method.
     */
    static class ColoredStackTraceWriter extends StringWriter {
        Help.ColorScheme colorScheme;

        public ColoredStackTraceWriter(Help.ColorScheme colorScheme) {
            this.colorScheme = colorScheme;
        }

        @Override
        public void write(String str, int off, int len) {
            final var styles = str.startsWith("\t") ? colorScheme.stackTraceStyles() : colorScheme.errorStyles();
            super.write(colorScheme.apply(str.substring(off, len), styles).toString());
        }
    }

    /**
     * Logger for debug output. Debug output is more verbose than verbose output. Subclasses should use {@link #verbose}
     * and write to {@link MessagePrinter} for verbose command output. Debug logging is really more for development.
     */
    protected static final Logger LOG = UnifiedLogger.getLogger();

    /**
     * Convenience method to execute the specified command object.
     *
     * @param command
     *            the command object
     * @param args
     *            the arguments
     */
    public static void execute(Callable<Integer> command, String[] args) {
        execute(new CommandLine(command).setUnmatchedArgumentsAllowed(true), args);
    }

    /**
     * Convenience method to execute the specified command line.
     * <p>
     * This method will call
     * {@link CommandLine#setExecutionExceptionHandler(picocli.CommandLine.IExecutionExceptionHandler)} and
     * {@link CommandLine#execute(String...)} on the given configured command line.
     * </p>
     *
     * @param configuredCommandLine
     *            a picocli command line
     * @param args
     *            the arguments
     */
    public static void execute(CommandLine configuredCommandLine, String[] args) {
        try {
            final var exitCode = configuredCommandLine.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                Exception cause;
                if (ex instanceof ExecutionException) {
                    cause = ex.getCause() instanceof Exception ? (Exception) ex.getCause() : ex;
                } else {
                    cause = ex;
                }

                final var err = commandLine.getErr();
                err.println();
                err.println(commandLine.getColorScheme().text("***********************************"));
                err.println(commandLine.getColorScheme().text("* @|bold ERROR: Command execution failed|@ *"));
                err.println(commandLine.getColorScheme().text("***********************************"));
                err.println();
                if (cause.getMessage() != null) {
                    err.println(commandLine.getColorScheme().errorText(cause.getMessage()));
                }
                err.flush();
                if (shouldAlwaysPrintDetails(cause)
                        || Stream.of(args).anyMatch(s -> "--verbose".equals(s) || "-v".equals(s))) {
                    err.println();
                    err.println("Details:");
                    err.println(commandLine.getColorScheme().richStackTraceString(cause));
                }
                return -1;
            }).execute(args);
            System.exit(exitCode);
        } catch (final Exception e) {
            System.err.println();
            System.err.println("********************************");
            System.err.println("* ERROR: Abnormal program exit *");
            System.err.println("********************************");
            System.err.println();
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static boolean shouldAlwaysPrintDetails(Exception cause) {
        if ((cause instanceof IllegalArgumentException) || (cause instanceof IllegalStateException)) {
            return (cause.getMessage() == null) || cause.getMessage().isBlank(); // only when message is empty
        }
        return true; // always print details
    }

    @Option(
            names = { "-h", "--help" },
            usageHelp = true,
            description = "Prints this help text",
            scope = ScopeType.INHERIT)
    protected boolean helpRequested;

    @Option(
            names = { "-b", "--batch-mode" },
            description = "indicates that the tool is called from a non-interactive script (eg., disables color output)",
            scope = ScopeType.INHERIT)
    protected boolean batchMode;

    @Option(
            names = { "-v", "--verbose" },
            description = "enable verbose command output; multiple -v options to increase verbosity",
            scope = ScopeType.INHERIT)
    private boolean[] verbosity;

    @Option(
            names = { "--max-progress-bar-length" },
            description = "max rendered length for the progress bar",
            defaultValue = "-1",
            hidden = true,
            scope = ScopeType.INHERIT)
    private int maxRenderedLength;

    /** if verbose command output is wanted (subclasses should use this to check) */
    protected boolean verbose;

    @Option(
            names = "--scm",
            description = "simple SCM integration for making files writable, either p4, git or none",
            defaultValue = "none",
            converter = NaiveScmToolConverter.class,
            scope = ScopeType.INHERIT)
    private NaiveScmTool scmTool;

    @Option(
            names = "--no-print-feedback-notice",
            description = "Don't print out a feeback notice at the end. By default it will be printed.",
            negatable = true,
            scope = ScopeType.INHERIT,
            hidden = true)
    protected boolean printFeedbackNotice = true;

    /**
     * Hook to be called after {@link #executeCommand(MessagePrinter)}.
     * <p>
     * Default implementation prints a feedback notice unless <code>--no-print-feedback-notice</code> was specified (see
     * {@link BaseCommand#printFeedbackNotice}) or the script is running in batch mode.
     * </p>
     *
     * @param returnCode
     *            the return code
     * @param out
     *            output printer
     */
    protected void afterExecuteCommand(int returnCode, MessagePrinter out) {
        if (printFeedbackNotice && !batchMode && (returnCode == 0)) {
            out.notice(
                format(
                    "%n%nLike what you are seeing?%nPlease leave feedback at https://github.com/salesforce/bazel-maven-deps."));
        }
    }

    /**
     * Hook to be called before {@link #executeCommand(MessagePrinter)}.
     * <p>
     * Although the default implementation does nothing, subclasses overriding this method must call <code>super</code>.
     * This method is typically used to initialize common parameters/values.
     * </p>
     *
     * @param out
     *            output printer
     */
    protected void beforeExecuteCommand(MessagePrinter out) {
        // no-op
    }

    @Override
    public final Integer call() throws Exception {
        if (verbosity != null) {
            verbose = verbosity.length >= 1;
            if (verbosity.length >= 2) {
                UnifiedLogger.enableDebugLogging();
            }
        }

        try (var out = createMessagePrinter()) {
            // hook for sub classes
            beforeExecuteCommand(out);

            final var returnCode = executeCommand(out);

            // hook for sub classes
            afterExecuteCommand(returnCode, out);

            return returnCode;
        }
    }

    /**
     * Initializes and returns a new {@link MessagePrinter}.
     * <p>
     * This method should be used and modified with care. It's very uncommon for commands to call it because it is
     * intended to be called by {@link BaseCommand} only. It's really only visible for testing because in tests you
     * might want to mock the message printer.
     * </p>
     *
     * @return a new {@link MessagePrinter} (must be used within try-with-resources)
     */
    @VisibleForTesting
    protected MessagePrinter createMessagePrinter() {
        synchronized (BaseCommand.class) {
            // re-use the shared one if we have it
            if (sharedMessagePrinterInstance != null) {
                sharedMessagePrinterInstance.incrementUsageCount();
                return sharedMessagePrinterInstance;
            }

            if (batchMode) {
                // never initialize the shared one in batch mode, this would risk circular
                // logging
                return MessagePrinter.toLog();
            }

            return sharedMessagePrinterInstance = new SharedConsoleMessagePrinter(maxRenderedLength);
        }
    }

    protected abstract int executeCommand(MessagePrinter out) throws Exception;

    protected NaiveScmTool getScmTool() {
        return requireNonNull(scmTool, "SCM tool not set!");
    }

    /**
     * @return Whether this command is interactive (allow input from the user)
     */
    protected final boolean isInteractive() {
        return (System.console() != null) && !batchMode;
    }

    protected String[] newCommandLineWithVerbosity(String... args) {
        final List<String> arguments = new ArrayList<>();

        if (args != null) {
            Collections.addAll(arguments, args);
        }

        if (verbosity != null) {
            for (final boolean verbose : verbosity) {
                if (verbose) {
                    arguments.add("-v");
                }
            }
        }

        return arguments.toArray(new String[arguments.size()]);
    }

    /**
     * Queries the {@link #getScmTool() NaiveScmTool} for any written or obsolete files and prints a summary.
     *
     * @param out
     * @param saveResult
     * @param obsoleteFiles
     */
    protected void printScmActivity(MessagePrinter out) {
        var modifiedFiles = getScmTool().getModifiedFiles();
        if (!modifiedFiles.isEmpty()) {
            out.info(
                format(
                    "%nThe following files were updated:%n%s%n",
                    modifiedFiles.stream()
                            .map(Path::toString)
                            .collect(joining(System.lineSeparator() + " > ", " > ", ""))));
        }

        var obsoleteFiles = getScmTool().getObsoleteFiles();
        if (!obsoleteFiles.isEmpty()) {
            out.info(
                format(
                    "%nPlease delete the following obsolete files:%n%s%n",
                    obsoleteFiles.stream()
                            .map(Path::toString)
                            .collect(joining(System.lineSeparator() + " > ", " > ", ""))));
        }
    }
}
