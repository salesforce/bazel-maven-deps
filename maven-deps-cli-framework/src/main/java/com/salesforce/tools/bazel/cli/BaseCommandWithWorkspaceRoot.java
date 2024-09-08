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
package com.salesforce.tools.bazel.cli;

import static java.lang.String.format;

import java.nio.file.Path;

import com.salesforce.tools.bazel.cli.helper.MessagePrinter;

import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

/**
 * Extension which detects the current Bazel workspace directory when using <code>bazel run</code> for execution.
 */
public abstract class BaseCommandWithWorkspaceRoot extends BaseCommand {
    @Option(names = {
            "--workspace-root" }, description = "Bazel Workspace root directory (default will be computed)", paramLabel = "WORKSPACE_ROOT", scope = ScopeType.INHERIT, required = false)
    protected Path workspaceRoot;

    /**
     * Initializes the {@link #workspaceRoot}. {@inheritDoc}
     */
    @Override
    protected void beforeExecuteCommand(MessagePrinter out) {
        // check for runfiles
        if ((System.getenv("RUNFILES_DIR") == null) && (System.getenv("JAVA_RUNFILES") == null)) {
            out.warning(
                " $RUNFILES_DIR and $JAVA_RUNFILES are both unset. Are you executing the command via 'bazel run'?");
        } else if (LOG.isDebugEnabled()) {
            // when debug log is enabled print the runfiles
            if (System.getenv("RUNFILES_DIR") != null) {
                out.notice(format("RUNFILES_DIR=%s", System.getenv("RUNFILES_DIR")));
            }
            if (System.getenv("JAVA_RUNFILES") != null) {
                out.notice(format("JAVA_RUNFILES=%s", System.getenv("JAVA_RUNFILES")));
            }
        }

        // check for workspace root
        initializeWorkspaceRoot(out);
    }

    private void initializeWorkspaceRoot(MessagePrinter out) {
        if (workspaceRoot == null) {
            final var buildWorkspaceDirectory = System.getenv("BUILD_WORKSPACE_DIRECTORY");
            if (buildWorkspaceDirectory == null) {
                throw new IllegalStateException(
                        "Unable to compute workspace. Please use 'bazel run' for this tool or add '--workspace-root' option");
            }
            workspaceRoot = Path.of(buildWorkspaceDirectory);
        }
        if (verbose) {
            out.notice(format("Using workspace at %s", workspaceRoot));
        }
    }
}
