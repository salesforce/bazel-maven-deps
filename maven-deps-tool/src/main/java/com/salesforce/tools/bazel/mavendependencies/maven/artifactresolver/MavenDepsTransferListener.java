package com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver;

import java.nio.file.Path;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.slf4j.Logger;

import com.salesforce.tools.bazel.cli.helper.ProgressMonitor;
import com.salesforce.tools.bazel.cli.helper.UnifiedLogger;

/**
 * Logs up- and downloads.
 */
class MavenDepsTransferListener extends AbstractTransferListener {

    private static final Logger LOG = UnifiedLogger.getLogger();
    private final ProgressMonitor monitor;
    private final List<TransferEvent> failures = new CopyOnWriteArrayList<>();

    MavenDepsTransferListener(ProgressMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * @return the failures
     */
    public List<TransferEvent> getFailures() {
        return failures;
    }

    @Override
    public void transferCorrupted(final TransferEvent event) throws TransferCancelledException {
        failures.add(event);

        final var resource = event.getResource();

        LOG.warn(
            event.getException().getMessage() + " for " + resource.getRepositoryUrl() + resource.getResourceName());
    }

    @Override
    public void transferFailed(TransferEvent event) {
        failures.add(event);
    }

    @Override
    public void transferInitiated(final TransferEvent event) throws TransferCancelledException {
        final var msg = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";
        if (monitor != null) {
            monitor.additionalMessage("↕ " + Path.of(event.getResource().getResourceName()).getFileName().toString());
            LOG.debug("{} {}{}", msg, event.getResource().getRepositoryUrl(), event.getResource().getResourceName());
        } else {
            LOG.info("{} {}{}", msg, event.getResource().getRepositoryUrl(), event.getResource().getResourceName());
        }
    }

    @Override
    public void transferSucceeded(final TransferEvent event) {
        var msg = new StringBuilder()
                .append(event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
        msg.append(" ").append(event.getResource().getRepositoryUrl()).append(event.getResource().getResourceName());

        final var contentLength = event.getTransferredBytes();
        if (contentLength >= 0) {
            final var len = contentLength >= 1024 ? ((contentLength + 1023) / 1024) + " KB" : contentLength + " B";

            var throughput = "";
            final var duration = System.currentTimeMillis() - event.getResource().getTransferStartTime();
            if (duration > 0) {
                final var format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
                final var kbPerSec = (contentLength / 1024.0) / (duration / 1000.0);
                throughput = " at " + format.format(kbPerSec) + " KB/sec";
            }

            msg.append(" (").append(len).append(throughput).append(")");
        }
        LOG.debug(msg.toString());
    }
}