package com.salesforce.tools.bazel.cli.slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import com.salesforce.tools.bazel.cli.ConsoleLogger;

public class ConsoleLoggerFactory implements ILoggerFactory {

    private final ConcurrentMap<String, Logger> loggerByName = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger(String name) {
        var logger = loggerByName.get(name);
        while (logger == null) {
            loggerByName.putIfAbsent(name, new ConsoleLogger(name));
            logger = loggerByName.get(name);
        }
        return logger;

    }

}
