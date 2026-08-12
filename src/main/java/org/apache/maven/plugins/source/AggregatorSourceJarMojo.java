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

import org.apache.maven.api.Type;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Execute;
import org.apache.maven.api.plugin.annotations.Mojo;

/**
 * Aggregate sources for all modules in an aggregator project.
 *
 * @since 2.0.3
 */
@Mojo(name = AggregatorSourceJarMojo.AGGREGATE_GOAL, defaultPhase = "package", aggregator = true)
@Execute(phase = "generate-sources")
public class AggregatorSourceJarMojo extends SourceJarMojo {

    static final String AGGREGATE_GOAL = "aggregate";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doExecute() throws MojoException {
        String packaging = getProject().getPackaging().type().id();
        if (Type.POM.equals(packaging)) {
            packageSources(reactorProjects);
        } else {
            getLog().warn("Not packaging aggregated sources: the " + AGGREGATE_GOAL + " goal requires \"" + Type.POM
                    + "\" packaging, but this project uses \"" + packaging + "\" packaging.");
        }
    }
}
