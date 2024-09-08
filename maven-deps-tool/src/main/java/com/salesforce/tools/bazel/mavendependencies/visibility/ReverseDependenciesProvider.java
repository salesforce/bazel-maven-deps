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
package com.salesforce.tools.bazel.mavendependencies.visibility;

import java.util.Collection;

/**
 * A provider to collect all direct reverse dependencies for a
 * {@link com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport}.
 * <p>
 * This is provided by the Maven deps tool and allows a visibility provider to query for reverse dependencies
 * <b>within</b> the pinned catalog.
 * </p>
 */
@FunctionalInterface
public interface ReverseDependenciesProvider {

    /**
     * @param name
     *            the name of the
     *            {@link com.salesforce.tools.bazel.mavendependencies.pinnedcatalog.BazelJavaDependencyImport} (must not
     *            be <code>null</code>)
     * @return a collection of direct reverse dependencies <b>within</b> the pinned catalog
     */
    Collection<String> getDirectReverseDependencies(String name);

}
