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
import org.apache.maven.api.plugin.testing.MojoParameter;
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
class SourceJarMojoTest extends AbstractSourcePluginTest {

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
    @InjectMojo(goal = "jar")
    @Basedir("/unit/project-001")
    void testDefaultConfiguration(SourceJarMojo mojo) throws Exception {

        mojo.execute();

        File target = new File(tempDir, "target");
        assertSourceArchive(target);
        assertJarContent(
                getSourceArchive(target),
                addMavenDescriptor(
                        "default-configuration.properties", "foo/project001/App.java", "foo/project001/", "foo/"));
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("/unit/project-003")
    void testExcludes(SourceJarMojo mojo) throws Exception {

        project.getResources().get(0).addExclude("excluded-file.txt");

        mojo.execute();

        File target = new File(tempDir, "target");
        assertSourceArchive(target);
        assertJarContent(
                getSourceArchive(target),
                addMavenDescriptor(
                        "default-configuration.properties", "foo/project003/App.java", "foo/project003/", "foo/"));
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("/unit/project-005")
    void testNoSources(SourceJarMojo mojo) throws Exception {
        mojo.execute();

        // Now make sure that no archive got created
        final File expectedFile = new File(tempDir, "target");
        assertFalse(
                expectedFile.exists(),
                "Source archive should not have been created[" + expectedFile.getAbsolutePath() + "]");
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("/unit/project-007")
    void testIncludes(AbstractSourceJarMojo mojo) throws Exception {
        project.getResources().get(0).addInclude("templates/**");

        mojo.execute();

        File target = new File(tempDir, "target");
        assertSourceArchive(target);
        assertJarContent(
                getSourceArchive(target),
                addMavenDescriptor(
                        "templates/configuration-template.properties",
                        "foo/project007/App.java",
                        "templates/",
                        "foo/project007/",
                        "foo/"));
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("/unit/project-009")
    @MojoParameter(name = "includePom", value = "true")
    void testIncludePom(SourceJarMojo mojo) throws Exception {

        mojo.execute();

        File target = new File(tempDir, "target");
        assertSourceArchive(target);
        assertJarContent(
                getSourceArchive(target),
                addMavenDescriptor(
                        "default-configuration.properties",
                        "pom.xml",
                        "foo/project009/App.java",
                        "foo/project009/",
                        "foo/"));
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("/unit/project-010")
    void testIncludeMavenDescriptorWhenExplicitlyConfigured(SourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(tempDir, "target");
        assertSourceArchive(target);
        assertJarContent(
                getSourceArchive(target),
                addMavenDescriptor(
                        "default-configuration.properties", "foo/project010/App.java", "foo/project010/", "foo/"));
    }
}
