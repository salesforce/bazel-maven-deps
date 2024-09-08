package com.salesforce.tools.bazel.cli.helper;

/**
 * Allows to report progress for long running tasks.
 * <p>
 * Must be used in a try-with-resources block to prevent leaking threads.
 * </p>
 */
public interface ProgressMonitor extends AutoCloseable {

    /**
     * @param text
     *            the name (or description) of a subtask to display (Subtasks are optional; the main task might not have
     *            subtasks.)
     */
    void additionalMessage(String text);

    /**
     * Convenience method to mark a progress monitor as complete
     */
    void done();

    /**
     * @return the current max hint (-1 for indefinite)
     */
    long max();

    /**
     * @param maxHint
     *            the max hint to set (-1 for indefinite)
     */
    void maxHint(long maxHint);

    /**
     * @param steps
     *            the progress count to record (towards {@link #maxHint(long)})
     */
    void progressBy(long steps);

}
