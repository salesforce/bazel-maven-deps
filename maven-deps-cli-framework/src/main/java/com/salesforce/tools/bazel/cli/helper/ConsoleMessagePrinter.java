package com.salesforce.tools.bazel.cli.helper;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.AnsiConsole;

class ConsoleMessagePrinter extends MessagePrinter {

    private final int maxRenderedLength;

    public ConsoleMessagePrinter(int maxRenderedLength) {
        this.maxRenderedLength = maxRenderedLength;
        AnsiConsole.systemInstall();
    }

    @Override
    public void close() {
        AnsiConsole.systemUninstall();
    }

    @Override
    public void error(String text) {
        AnsiConsole.out()
                .println(Ansi.ansi().fgBrightRed().bold().a("ERROR: ").boldOff().fgRed().a(text).reset().toString());
    }

    @Override
    public void important(String text) {
        AnsiConsole.out().println(Ansi.ansi().bold().a(text).boldOff().toString());
    }

    @Override
    public void info(String text) {
        AnsiConsole.out().println(Ansi.ansi().reset().a(text).toString());
    }

    @Override
    public void notice(String text) {
        AnsiConsole.out().println(Ansi.ansi().a(Attribute.INTENSITY_FAINT).a(text).boldOff().toString());

    }

    @Override
    public ProgressMonitor progressMonitor(String taskName) {
        return new ProgressBarProgressMonitor(taskName, maxRenderedLength);
    }

    @Override
    public void warning(String text) {
        AnsiConsole.out()
                .println(
                    Ansi.ansi().fgBrightYellow().bold().a("WARNING: ").boldOff().fgYellow().a(text).reset().toString());
    }

}
