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

import org.apache.maven.api.Project;
import org.apache.maven.api.Type;
import org.apache.maven.api.plugin.Log;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that the aggregate goal reports why it did nothing when the project
 * does not use "pom" packaging, instead of failing silently.
 */
class AggregatorSourceJarMojoTest {

    private AggregatorSourceJarMojo mojoForPackaging(String packaging, Log log) {
        Project project = mock(Project.class, RETURNS_DEEP_STUBS);
        when(project.getPackaging().type().id()).thenReturn(packaging);

        AggregatorSourceJarMojo mojo = new AggregatorSourceJarMojo();
        mojo.project = project;
        mojo.log = log;
        return mojo;
    }

    @Test
    void warnsWhenPackagingIsNotPom() {
        Log log = mock(Log.class);
        AggregatorSourceJarMojo mojo = mojoForPackaging("jar", log);

        mojo.doExecute();

        ArgumentCaptor<CharSequence> message = ArgumentCaptor.forClass(CharSequence.class);
        verify(log).warn(message.capture());

        String warning = message.getValue().toString();
        assertTrue(warning.contains("jar"), "warning should name the actual packaging, was: " + warning);
        assertTrue(warning.contains(Type.POM), "warning should name the required packaging, was: " + warning);
        assertTrue(
                warning.contains(AggregatorSourceJarMojo.AGGREGATE_GOAL),
                "warning should name the goal, was: " + warning);
    }

    @Test
    void doesNotWarnWhenPackagingIsPom() {
        Log log = mock(Log.class);
        AggregatorSourceJarMojo mojo = mojoForPackaging(Type.POM, log);

        // reactorProjects is left null: packageSources would throw before any warning
        // is emitted, which is enough to show the non-pom branch was not taken.
        try {
            mojo.doExecute();
        } catch (RuntimeException expected) {
            // not the subject of this test
        }

        verify(log, never()).warn(org.mockito.ArgumentMatchers.<CharSequence>any());
    }
}
