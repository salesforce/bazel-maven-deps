package com.salesforce.tools.bazel.mavendependencies.helper;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readString;
import static java.nio.file.Files.writeString;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.salesforce.tools.bazel.cli.helper.ScmSupport;

public final class NoScmWritableFilesystem implements ScmSupport {
    @Override
    public boolean removeFile(Path obsoletePath) throws IOException {
        if (exists(obsoletePath)) {
            delete(obsoletePath);
            return true;
        }
        return false;
    }

    @Override
    public boolean writeFile(Path path, CharSequence content, Charset charset) throws IOException {
        var isNewFile = !exists(path);
        if (!isNewFile) {
            var existingContent = readString(path, charset);
            if (existingContent.equals(content)) {
                return false;
            }
        }

        writeString(path, content, charset, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return true;
    }
}