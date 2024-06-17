/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.source;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;

/**
 * This goal bundles all the test sources into a jar archive.  This goal functions the same
 * as the test-jar goal but does not fork the build, and is suitable for attaching
 * to the build lifecycle.
 *
 * @since 2.1
 */
@Mojo(name = "test-jar-no-fork", defaultPhase = "package")
public class TestSourceJarNoForkMojo extends AbstractSourceJarMojo {
    /**
     * @since 2.2
     */
    @Parameter(property = "maven.source.test.classifier", defaultValue = "test-sources")
    protected String classifier;

    /**
     * {@inheritDoc}
     */
    protected List<Path> getSources(Project p) {
        return projectManager.getCompileSourceRoots(p, ProjectScope.TEST);
    }

    /**
     * {@inheritDoc}
     */
    protected List<Resource> getResources(Project p) {
        if (excludeResources) {
            return Collections.emptyList();
        }

        return projectManager.getResources(p, ProjectScope.TEST);
    }

    /**
     * {@inheritDoc}
     */
    protected String getClassifier() {
        return classifier;
    }
}
