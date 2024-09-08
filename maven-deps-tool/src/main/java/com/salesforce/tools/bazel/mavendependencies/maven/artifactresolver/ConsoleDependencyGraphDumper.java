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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

/**
 * A dependency visitor that dumps the graph to the console.
 */
public class ConsoleDependencyGraphDumper implements DependencyVisitor {

    private static class ChildInfo {

        final int count;

        int index;

        ChildInfo(int count) {
            this.count = count;
        }

        public String formatIndentation(boolean end) {
            final var last = (index + 1) >= count;
            if (end) {
                return last ? "\\- " : "+- ";
            }
            return last ? "   " : "|  ";
        }

    }

    private final PrintStream out;

    private final List<ChildInfo> childInfos = new ArrayList<>();

    public ConsoleDependencyGraphDumper() {
        this(null);
    }

    public ConsoleDependencyGraphDumper(PrintStream out) {
        this.out = (out != null) ? out : System.out;
    }

    private String formatIndentation() {
        final var buffer = new StringBuilder(128);
        for (final var it = childInfos.iterator(); it.hasNext();) {
            buffer.append(it.next().formatIndentation(!it.hasNext()));
        }
        return buffer.toString();
    }

    private String formatNode(DependencyNode node) {
        final var buffer = new StringBuilder(128);
        final var a = node.getArtifact();
        final var d = node.getDependency();
        buffer.append(a);
        if ((d != null) && (d.getScope().length() > 0)) {
            buffer.append(" [").append(d.getScope());
            if (d.isOptional()) {
                buffer.append(", optional");
            }
            buffer.append("]");
        }
        {
            final var premanaged = DependencyManagerUtils.getPremanagedVersion(node);
            if ((premanaged != null) && !premanaged.equals(a.getBaseVersion())) {
                buffer.append(" (version managed from ").append(premanaged).append(")");
            }
        }
        {
            final var premanaged = DependencyManagerUtils.getPremanagedScope(node);
            if ((premanaged != null) && !premanaged.equals(d.getScope())) {
                buffer.append(" (scope managed from ").append(premanaged).append(")");
            }
        }
        final var winner = (DependencyNode) node.getData().get(ConflictResolver.NODE_DATA_WINNER);
        if ((winner != null) && !ArtifactIdUtils.equalsId(a, winner.getArtifact())) {
            final var w = winner.getArtifact();
            buffer.append(" (conflicts with ");
            if (ArtifactIdUtils.toVersionlessId(a).equals(ArtifactIdUtils.toVersionlessId(w))) {
                buffer.append(w.getVersion());
            } else {
                buffer.append(w);
            }
            buffer.append(")");
        }
        return buffer.toString();
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        out.println(formatIndentation() + formatNode(node));
        childInfos.add(new ChildInfo(node.getChildren().size()));
        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        if (!childInfos.isEmpty()) {
            childInfos.remove(childInfos.size() - 1);
        }
        if (!childInfos.isEmpty()) {
            childInfos.get(childInfos.size() - 1).index++;
        }
        return true;
    }

}
