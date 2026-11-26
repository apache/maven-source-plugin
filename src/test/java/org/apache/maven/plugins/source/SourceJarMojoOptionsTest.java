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
import java.nio.file.Files;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugins.source.stubs.ProjectStub;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Per-option checks of the jar and test-jar goals: which entries end up in the archive, which file name
 * the classifier gives it, and when no archive is created.
 */
@MojoTest
class SourceJarMojoOptionsTest extends AbstractSourcePluginTest {

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
    @Basedir("/unit/project-011")
    void testJarIncludes(SourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(tempDir, "target");
        assertSourceArchive(target);
        assertJarContent(getSourceArchive(target), addMavenDescriptor("MyClass2.java"));
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("/unit/project-012")
    void testTestJarExcludes(TestSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(tempDir, "target");
        assertTestSourceArchive(target);
        assertJarContent(getTestSourceArchive(target), addMavenDescriptor("MyTest.java", "test.properties"));
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("/unit/project-001")
    @MojoParameter(name = "classifier", value = "foo")
    void testJarClassifier(SourceJarMojo mojo) throws Exception {
        mojo.execute();

        File archive = new File(tempDir, "target/maven-source-plugin-test-99.0-foo.jar");
        assertTrue(archive.isFile(), "Archive with the configured classifier is missing: " + archive);
        assertFalse(getSourceArchive(new File(tempDir, "target")).exists());
        assertEquals(1, project.getAttachedArtifacts().size());
        assertEquals("foo", project.getAttachedArtifacts().get(0).getClassifier());
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("/unit/project-001")
    @MojoParameter(name = "classifier", value = "bar")
    void testTestJarClassifier(TestSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File archive = new File(tempDir, "target/maven-source-plugin-test-99.0-bar.jar");
        assertTrue(archive.isFile(), "Archive with the configured classifier is missing: " + archive);
        assertFalse(getTestSourceArchive(new File(tempDir, "target")).exists());
        assertEquals(1, project.getAttachedArtifacts().size());
        assertEquals("bar", project.getAttachedArtifacts().get(0).getClassifier());
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("/unit/project-001")
    void testJarIncludesGeneratedSources(SourceJarMojo mojo) throws Exception {
        File generated = new File(tempDir, "generated");
        Files.createDirectories(generated.toPath());
        Files.createFile(new File(generated, "generated.properties").toPath());
        project.addCompileSourceRoot(generated.getAbsolutePath());

        mojo.execute();

        File target = new File(tempDir, "target");
        assertJarContent(
                getSourceArchive(target),
                addMavenDescriptor(
                        "default-configuration.properties",
                        "generated.properties",
                        "foo/project001/App.java",
                        "foo/project001/",
                        "foo/"));
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("/unit/project-001")
    void testTestJarIncludesGeneratedSources(TestSourceJarMojo mojo) throws Exception {
        File generated = new File(tempDir, "generated");
        Files.createDirectories(generated.toPath());
        Files.createFile(new File(generated, "generated.properties").toPath());
        project.addTestCompileSourceRoot(generated.getAbsolutePath());

        mojo.execute();

        File target = new File(tempDir, "target");
        assertJarContent(
                getTestSourceArchive(target),
                addMavenDescriptor(
                        "test-default-configuration.properties",
                        "generated.properties",
                        "foo/project001/AppTest.java",
                        "foo/project001/",
                        "foo/"));
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("/unit/project-005")
    void testJarWithoutSourcesCreatesNothing(SourceJarMojo mojo) throws Exception {
        mojo.execute();

        assertFalse(getSourceArchive(new File(tempDir, "target")).exists());
        assertTrue(project.getAttachedArtifacts().isEmpty());
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("/unit/project-005")
    void testTestJarWithoutSourcesCreatesNothing(TestSourceJarMojo mojo) throws Exception {
        mojo.execute();

        assertFalse(getTestSourceArchive(new File(tempDir, "target")).exists());
        assertTrue(project.getAttachedArtifacts().isEmpty());
    }
}
