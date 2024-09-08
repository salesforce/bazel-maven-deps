package com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver;

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

import java.io.FileNotFoundException;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.slf4j.Logger;

import com.salesforce.tools.bazel.cli.helper.ProgressMonitor;
import com.salesforce.tools.bazel.cli.helper.UnifiedLogger;

/**
 * Logs repository events like installed and unresolved artifacts and metadata.
 */
class MavenDepsRepositoryListener extends AbstractRepositoryListener {

    private static final Logger LOG = UnifiedLogger.getLogger();
    private final ProgressMonitor monitor;

    MavenDepsRepositoryListener(ProgressMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void artifactDescriptorInvalid(final RepositoryEvent event) {
        LOG.debug(
            "The POM for " + event.getArtifact() + " is invalid"
                    + ", transitive dependencies (if any) will not be available: " + event.getException().getMessage(),
            event.getException());
    }

    @Override
    public void artifactDescriptorMissing(final RepositoryEvent event) {
        LOG.debug("The POM for " + event.getArtifact() + " is missing, no dependency information available");
    }

    @Override
    public void artifactDownloaded(RepositoryEvent event) {
        if (monitor != null) {
            monitor.progressBy(1);
        }
    }

    @Override
    public void artifactDownloading(RepositoryEvent event) {
        if ((monitor != null) && (event.getArtifact() != null)) {
            monitor.additionalMessage("↕ " + event.getArtifact());
        }
    }

    @Override
    public void artifactInstalling(final RepositoryEvent event) {
        LOG.debug("Installing " + event.getArtifact().getFile() + " to " + event.getFile());
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        if (monitor != null) {
            monitor.progressBy(1);
        }
    }

    @Override
    public void artifactResolving(RepositoryEvent event) {
        if ((monitor != null) && (event.getArtifact() != null)) {
            monitor.additionalMessage(
                "♲ " + event.getArtifact().getGroupId() + ":" + event.getArtifact().getArtifactId());
        }
    }

    @Override
    public void metadataDownloaded(RepositoryEvent event) {
        if (monitor != null) {
            monitor.progressBy(1);
        }
    }

    @Override
    public void metadataDownloading(RepositoryEvent event) {
        if ((monitor != null) && (event.getArtifact() != null)) {
            monitor.additionalMessage("↕ " + event.getMetadata());
        }
    }

    @Override
    public void metadataInstalling(final RepositoryEvent event) {
        LOG.debug("Installing " + event.getMetadata() + " to " + event.getFile());
    }

    @Override
    public void metadataInvalid(final RepositoryEvent event) {
        final var exception = event.getException();

        final var buffer = new StringBuilder(256);
        buffer.append("The metadata ");
        if (event.getMetadata().getFile() != null) {
            buffer.append(event.getMetadata().getFile());
        } else {
            buffer.append(event.getMetadata());
        }

        if (exception instanceof FileNotFoundException) {
            buffer.append(" is inaccessible");
        } else {
            buffer.append(" is invalid");
        }

        if (exception != null) {
            buffer.append(": ");
            buffer.append(exception.getMessage());
        }

        LOG.debug(buffer.toString(), exception);
    }

    @Override
    public void metadataResolved(final RepositoryEvent event) {
        final var e = event.getException();
        if (e != null) {
            if (e instanceof MetadataNotFoundException) {
                LOG.debug(e.getMessage());
            } else {
                LOG.debug(e.getMessage(), e);
            }
        }
    }
}