/*-
 * Copyright (c) 2024 Salesforce.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.salesforce.tools.bazel.mavendependencies.maven.artifactresolver;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.transfer.TransferEvent;

/**
 * Wrapper around {@link org.eclipse.aether.resolution.DependencyResult} to provide additional diagnostics information
 */
public class DependencyResultWithTransferInfo {

    static final DependencyResultWithTransferInfo create(
            DependencyResult dependencyResult,
            RepositorySystemSession session) {
        var transferListener = session.getTransferListener();

        return new DependencyResultWithTransferInfo(
                dependencyResult,
                transferListener instanceof MavenDepsTransferListener
                        ? ((MavenDepsTransferListener) transferListener).getFailures() : Collections.emptyList());
    }

    public static Stream<String> toSortedStreamOfMessages(List<TransferEvent> transferFailures) {
        return transferFailures.stream().map(event -> {
            final var resource = event.getResource();
            return event.getException().getMessage() + " for " + resource.getRepositoryUrl()
                    + resource.getResourceName();
        }).distinct().sorted();
    }

    private final DependencyResult dependencyResult;

    private final List<TransferEvent> transferFailures;

    public DependencyResultWithTransferInfo(DependencyResult dependencyResult, List<TransferEvent> transferFailures) {
        this.dependencyResult = dependencyResult;
        this.transferFailures = transferFailures;
    }

    /**
     * @return the dependencyResult
     */
    public DependencyResult getDependencyResult() {
        return dependencyResult;
    }

    /**
     * @return the transferFailures
     */
    public List<TransferEvent> getTransferFailures() {
        return transferFailures;
    }
}
