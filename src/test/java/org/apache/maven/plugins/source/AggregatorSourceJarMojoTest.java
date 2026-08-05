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

import java.util.Collections;
import java.util.List;

import org.apache.maven.api.Packaging;
import org.apache.maven.api.Project;
import org.apache.maven.api.Type;
import org.apache.maven.api.plugin.Log;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AggregatorSourceJarMojoTest {

    /**
     * Records the projects handed to {@code packageSources} instead of building an archive, so the test can tell
     * whether the goal did its work or skipped.
     */
    private static class RecordingAggregatorSourceJarMojo extends AggregatorSourceJarMojo {
        private List<Project> packagedProjects;

        @Override
        protected void packageSources(List<Project> theProjects) {
            packagedProjects = theProjects;
        }
    }

    @Test
    void testPomPackagingPackagesTheReactorProjects() {
        Log log = mock(Log.class);
        RecordingAggregatorSourceJarMojo mojo = mojoWithPackaging(Type.POM, log);
        List<Project> reactorProjects = Collections.singletonList(mock(Project.class));
        mojo.reactorProjects = reactorProjects;

        mojo.doExecute();

        assertEquals(reactorProjects, mojo.packagedProjects);
        verifyNoInteractions(log);
    }

    /**
     * The goal cannot aggregate anything for non-POM packaging, but skipping without a word leaves the user with no
     * source archive and no reason why.
     */
    @Test
    void testNonPomPackagingWarnsInsteadOfSkippingSilently() {
        Log log = mock(Log.class);
        RecordingAggregatorSourceJarMojo mojo = mojoWithPackaging("jar", log);

        mojo.doExecute();

        assertNull(mojo.packagedProjects, "Sources should not be packaged for non-POM packaging");

        ArgumentCaptor<CharSequence> warning = ArgumentCaptor.forClass(CharSequence.class);
        verify(log).warn(warning.capture());
        String message = warning.getValue().toString();
        assertTrue(message.contains("jar"), "Warning should name the actual packaging: " + message);
        assertTrue(message.contains("aggregate:1.0:pom"), "Warning should name the project: " + message);
    }

    private static RecordingAggregatorSourceJarMojo mojoWithPackaging(String type, Log log) {
        Type packagingType = mock(Type.class);
        when(packagingType.id()).thenReturn(type);

        Packaging packaging = mock(Packaging.class);
        when(packaging.type()).thenReturn(packagingType);

        Project project = mock(Project.class);
        when(project.getPackaging()).thenReturn(packaging);
        when(project.getId()).thenReturn("aggregate:1.0:pom");

        RecordingAggregatorSourceJarMojo mojo = new RecordingAggregatorSourceJarMojo();
        mojo.project = project;
        mojo.log = log;
        return mojo;
    }
}
