package com.salesforce.tools.bazel.cli.scm;

import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isWritable;
import static java.nio.file.Files.move;
import static java.nio.file.Files.readString;
import static java.nio.file.Files.writeString;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;

import com.salesforce.tools.bazel.cli.helper.ScmSupport;
import com.salesforce.tools.bazel.cli.helper.UnifiedLogger;

/**
 * A naive implementation of {@link ScmSupport} which includes some rudimentary file tracking.
 */
public class NaiveScmTool implements ScmSupport {

    static final class Perforce extends NaiveScmTool {

        final String clientSpec;
        final String changeList;

        public Perforce(String clientSpec, String changeList) {
            this.clientSpec = clientSpec;
            this.changeList = changeList;
        }

        @Override
        protected void add(Path path) throws IOException {
            runP4Command("add", path);
        }

        @Override
        protected boolean delete(Path obsoletePath) throws IOException {
            runP4Command("delete", obsoletePath);
            return exists(obsoletePath);
        }

        @Override
        protected void makeWritable(Path path) throws IOException {
            if (!isWritable(path)) {
                runP4Command("edit", path);
            }
        }

        @Override
        public void moveFile(Path from, Path to) throws IOException {
            runP4Command("move", from.getParent(), List.of("-r", from.toString(), to.toString()));
        }

        protected void runP4Command(String subcommand, Path path) throws IOException {
            runP4Command(subcommand, path.getParent(), List.of(path.toString()));
        }

        protected void runP4Command(
                String subcommand,
                Path workingDir,
                List<String> additionalArgs) throws IOException {
            final List<String> commandLine = new ArrayList<>();

            commandLine.add("p4");
            if ((clientSpec != null)) {
                commandLine.add("-c");
                commandLine.add(clientSpec);
            }

            commandLine.add(subcommand);

            if ((changeList != null)) {
                commandLine.add("-c");
                commandLine.add(changeList);
            }

            commandLine.addAll(additionalArgs);

            runProcess(workingDir, commandLine);
        }
    }

    private static final Logger LOG = UnifiedLogger.getLogger();

    static final NaiveScmTool noop = new NaiveScmTool();
    static final NaiveScmTool p4 = new Perforce(null, null);
    static final NaiveScmTool git = new NaiveScmTool() {
        @Override
        protected void add(Path path) throws IOException {
            runProcess(path.getParent(), "git", "add", path.toString());
        }

        @Override
        protected boolean delete(Path obsoletePath) throws IOException {
            runProcess(obsoletePath.getParent(), "git", "rm", "-f", obsoletePath.toString());
            return exists(obsoletePath);
        }
    };

    static void runProcess(Path directory, List<String> command) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Running {} (in '{}'", command.stream().collect(joining(" ")), directory);
        }
        final var processBuilder = new ProcessBuilder(command).directory(directory.toFile());
        if (LOG.isDebugEnabled()) {
            processBuilder.redirectOutput(Redirect.INHERIT);
        } else {
            processBuilder.redirectOutput(Redirect.DISCARD);
        }
        processBuilder.redirectError(Redirect.INHERIT);
        final var process = processBuilder.start();
        try {
            process.waitFor();
        } catch (final InterruptedException e) {
            // ignore, just reset interrupt flag
            Thread.currentThread().interrupt();
        }
    }

    static void runProcess(Path directory, String... commandLine) throws IOException {
        final List<String> command = new ArrayList<>(commandLine.length);
        Collections.addAll(command, commandLine);

        runProcess(directory, command);
    }

    private final SortedSet<Path> modifiedFiles = new TreeSet<>();
    private final SortedSet<Path> obsoleteFiles = new TreeSet<>();

    private NaiveScmTool() {
        // empty
    }

    protected void add(Path path) throws IOException {
        // no-op
    }

    protected boolean delete(Path obsoletePath) throws IOException {
        return false; // no-op
    }

    /**
     * @return the modifiedFiles
     */
    public SortedSet<Path> getModifiedFiles() {
        return modifiedFiles;
    }

    /**
     * @return the obsoleteFiles
     */
    public SortedSet<Path> getObsoleteFiles() {
        return obsoleteFiles;
    }

    protected void makeWritable(Path path) throws IOException {
        if (!isWritable(path)) {
            throw new IOException(format("Cannot write to '%s'. Make the file writable!", path));
        }
    }

    public void moveFile(Path from, Path to) throws IOException {
        move(from, to);
    }

    @Override
    public boolean removeFile(Path obsoletePath) throws IOException {
        if (exists(obsoletePath)) {
            if (delete(obsoletePath)) {
                return true; // file was deleted
            }

            // ensure the content is removed
            writeString(
                obsoletePath,
                "# obsolete (please delete)",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            obsoleteFiles.add(obsoletePath);
        }
        return false;
    }

    @Override
    public boolean writeFile(Path path, CharSequence content, Charset charset) throws IOException {
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("path must be absolute: " + path);
        }

        final var isNewFile = !exists(path);
        if (!isNewFile) {
            final var existingContent = readString(path, charset);
            if (existingContent.equals(content)) {
                LOG.debug("File '{}' not modified", path);
                return false;
            }

            makeWritable(path);

            if (!isWritable(path)) {
                throw new IOException(
                        format("Unable to make file '%s' writable. Please check SCM configuration!", path));
            }
        }

        if (!isDirectory(path.getParent())) {
            createDirectories(path.getParent());
        }

        writeString(path, content, charset, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        if (isNewFile) {
            add(path);
        }

        modifiedFiles.add(path);
        return true;
    }
}
