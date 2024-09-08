package com.salesforce.tools.bazel.cli.helper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * A little helper for making dependency saving work nicely with SCM systems.
 */
public interface ScmSupport {

    /**
     * Removes a file that is now obsolete.
     * <p>
     * In case removal is not possible the file content is replaced with an obsolete marker.
     * </p>
     *
     * @param obsoletePath
     *            the file path
     * @return <code>true</code> if the file was deleted
     */
    boolean removeFile(Path obsoletePath) throws IOException;

    /**
     * Writes a file, ensures it's writable and registered with SCM.
     *
     * @param path
     *            the file path
     * @param content
     *            the content
     * @param charset
     *            the char set
     * @return <code>true</code> if the file was updated
     */
    boolean writeFile(Path path, CharSequence content, Charset charset) throws IOException;
}
