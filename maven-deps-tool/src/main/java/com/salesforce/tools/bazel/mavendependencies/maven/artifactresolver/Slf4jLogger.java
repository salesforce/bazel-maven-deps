package com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver;

import org.eclipse.aether.spi.log.Logger;

@SuppressWarnings("deprecation")
class Slf4jLogger implements Logger {

    private final org.slf4j.Logger logger;

    Slf4jLogger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String msg, Throwable error) {
        logger.debug(msg, error);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void warn(String msg, Throwable error) {
        logger.warn(msg, error);
    }

}