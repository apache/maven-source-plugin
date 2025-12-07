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

import javax.inject.Inject;

import java.io.File;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugins.source.stubs.ProjectStub;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
@MojoTest
class TestSourceJarMojoTest extends AbstractSourcePluginTest {

    @TempDir
    private File tempDir;

    @Inject
    private MavenProject project;

    @Provides
    @SuppressWarnings("unused")
    private MavenProject projectProvides() {
        return new ProjectStub(tempDir);
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("/unit/project-001")
    void testDefaultConfiguration(TestSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(tempDir, "target");
        assertTestSourceArchive(target);
        assertJarContent(
                getTestSourceArchive(target),
                addMavenDescriptor(
                        "test-default-configuration.properties",
                        "foo/project001/AppTest.java",
                        "foo/project001/",
                        "foo/"));
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("/unit/project-003")
    void testExcludes(TestSourceJarMojo mojo) throws Exception {
        project.getTestResources().get(0).addExclude("excluded-file.txt");

        mojo.execute();

        File target = new File(tempDir, "target");
        assertTestSourceArchive(target);
        assertJarContent(
                getTestSourceArchive(target),
                addMavenDescriptor(
                        "test-default-configuration.properties",
                        "foo/project003/AppTest.java",
                        "foo/project003/",
                        "foo/"));
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("/unit/project-005")
    void testNoSources(TestSourceJarMojo mojo) throws Exception {
        mojo.execute();

        // Now make sure that no archive got created
        File expectedFile = new File(tempDir, "target");
        assertFalse(
                expectedFile.exists(),
                "Test source archive should not have been created[" + expectedFile.getAbsolutePath() + "]");
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("/unit/project-010")
    void testIncludeMavenDescriptorWhenExplicitlyConfigured(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(tempDir, "target");
        assertTestSourceArchive(target);
        assertJarContent(
                getTestSourceArchive(target),
                addMavenDescriptor(
                        "test-default-configuration.properties",
                        "foo/project010/AppTest.java",
                        "foo/project010/",
                        "foo/"));
    }
}
