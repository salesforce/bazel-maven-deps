package com.salesforce.tools.bazel.cli;

import static org.slf4j.spi.LocationAwareLogger.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

/**
 * Logger printing to System.out/err or the {@link SharedConsoleMessagePrinter}.
 */
public class ConsoleLogger extends AbstractLogger {

    private static final long serialVersionUID = 1L;

    DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    // maintain compatibility to SimpleLogger for UnifiedLogger */
    protected int currentLogLevel = INFO_INT;

    public ConsoleLogger(String name) {
        this.name = name;
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(
            Level level,
            Marker marker,
            String messagePattern,
            Object[] arguments,
            Throwable throwable) {

        final var formattedMessage = MessageFormatter.basicArrayFormat(messagePattern, arguments);

        final var messagePrinter = SharedConsoleMessagePrinter.sharedMessagePrinterInstance;
        if (messagePrinter != null) {
            // when using the message printer we ignore all the logging format and print the
            // message directly
            switch (level) {
                case TRACE:
                case DEBUG:
                    messagePrinter.notice(formattedMessage);
                    break;

                case INFO:
                    messagePrinter.info(formattedMessage);
                    break;
                case WARN:
                    messagePrinter.warning(formattedMessage);
                    break;
                case ERROR:
                default:
                    System.err.println(formattedMessage);
                    break;
            }

            return;
        }

        // otherwise we wrap it into the layout and log to System.out/err

        final var message = new StringBuilder(120);
        message.append('(');
        dateFormat.formatTo(LocalDateTime.now(), message);
        //		message.append(") [");
        //		message.append(Thread.currentThread().getName());
        //		message.append("] ");
        //		message.append(level.name());
        message.append(") ");
        //		message.append(String.valueOf(name)).append(" - ");
        //		if (marker != null) {
        //			message.append(" ").append(marker.getName()).append(" ");
        //		}
        message.append(formattedMessage);

        final var fullMessage = message.toString();

        switch (level) {
            case TRACE:
            case DEBUG:
            case INFO:
            case WARN:
                System.out.println(fullMessage);
                break;
            case ERROR:
            default:
                System.err.println(fullMessage);
                break;
        }

    }

    @Override
    public boolean isDebugEnabled() {
        return isLevelEnabled(DEBUG_INT);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isLevelEnabled(DEBUG_INT);
    }

    @Override
    public boolean isErrorEnabled() {
        return isLevelEnabled(ERROR_INT);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isLevelEnabled(ERROR_INT);
    }

    @Override
    public boolean isInfoEnabled() {
        return isLevelEnabled(INFO_INT);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isLevelEnabled(INFO_INT);
    }

    protected boolean isLevelEnabled(int logLevel) {
        return (logLevel >= currentLogLevel);
    }

    @Override
    public boolean isTraceEnabled() {
        return isLevelEnabled(TRACE_INT);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isLevelEnabled(TRACE_INT);
    }

    @Override
    public boolean isWarnEnabled() {
        return isLevelEnabled(WARN_INT);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isLevelEnabled(WARN_INT);
    }

}
