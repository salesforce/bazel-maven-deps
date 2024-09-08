package com.salesforce.tools.bazel.mavendependencies.maven;

import static java.lang.String.format;
import static java.nio.file.Files.*;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.aether.artifact.Artifact;

public class MavenShaComputer {

    public enum Algorithm {

        SHA256("SHA-256", ".sha256"), SHA1("SHA-1", ".sha1");

        private final String algorithm;
        private final String extension;

        Algorithm(String algorithm, String extension) {
            this.algorithm = algorithm;
            this.extension = extension;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public String getExtension() {
            return extension;
        }
    }

    private String checksum(Path jarFile, Algorithm algorithm) throws IOException {
        final var checksumFile =
                jarFile.resolveSibling(jarFile.getFileName().toString().concat(algorithm.getExtension()));
        if (isRegularFile(checksumFile)) {
            return readChecksumFromFile(checksumFile);
        }

        final var buffer = new byte[8192];
        int count;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm.getAlgorithm());
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(format("Message Digest %s not available.", algorithm), e);
        }
        try (var in = newInputStream(jarFile)) {
            final var bis = new BufferedInputStream(in);
            while ((count = bis.read(buffer)) > 0) {
                digest.update(buffer, 0, count);
            }
        }

        final var sb = new StringBuilder();
        for (final byte b : digest.digest()) {
            sb.append(String.format("%02x", b));
        }

        // cache checksum
        writeString(checksumFile, sb, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return sb.toString();
    }

    public String getSha(Artifact artifact, Algorithm algorithm) throws IOException {
        final var file = artifact.getFile();
        if ((file == null) || !file.isFile()) {
            throw new FileNotFoundException(format("Artifact '%s' is not resolved!", artifact));
        }

        return checksum(file.toPath(), algorithm);
    }

    private String readChecksumFromFile(Path checksumFile) throws IOException {
        // the file is expected to contain just the checksum
        // sometimes it may contain additional stuff, thus we stop reading after the
        // first tab/space/newline
        try (var reader = Files.newBufferedReader(checksumFile)) {
            final var checksum = new StringBuilder();

            while (reader.ready()) {
                final var c = reader.read();
                if (c == -1) {
                    return checksum.toString();
                }

                switch (c) {
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        // stop reading
                        return checksum.toString();

                    default:
                        checksum.append((char) c);
                        continue;
                }
            }

            return checksum.toString();
        }
    }
}
