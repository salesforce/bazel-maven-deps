package com.salesforce.tools.bazel.mavendependencies.tool.cli;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;
import com.salesforce.tools.bazel.cli.helper.NullProgressMonitor;
import com.salesforce.tools.bazel.cli.helper.ProgressMonitor;

/**
 * Instead of printing this one collects messages for testing purposes
 */
public final class MessageCollector extends MessagePrinter {

    public enum Level {
        NOTICE, INFO, IMPORTANT, WARNING, ERROR
    }

    public static class Message {
        public final Level level;
        public final String text;

        public Message(Level level, String text) {
            this.level = level;
            this.text = text;
        }
    }

    private final List<Message> messages = new ArrayList<>();

    public void assertInfo(String text) {
        assertTrue(
            messages.parallelStream().anyMatch(m -> (m.level == Level.INFO) && m.text.contains(text)),
            () -> format("Expected INFO message not found in command output: %s", text));

    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void error(String text) {
        messages.add(new Message(Level.ERROR, text));
    }

    @Override
    public void important(String text) {
        messages.add(new Message(Level.IMPORTANT, text));
    }

    @Override
    public void info(String text) {
        messages.add(new Message(Level.INFO, text));
    }

    @Override
    public void notice(String text) {
        messages.add(new Message(Level.NOTICE, text));
    }

    public void printAllMessages() {
        messages.forEach(message -> System.out.println(message.level + ": " + message.text));
    }

    @Override
    public ProgressMonitor progressMonitor(String taskName) {
        return new NullProgressMonitor();
    }

    @Override
    public void warning(String text) {
        messages.add(new Message(Level.WARNING, text));
    }
}