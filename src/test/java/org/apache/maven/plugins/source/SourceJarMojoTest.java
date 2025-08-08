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

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.api.plugin.testing.stubs.SessionMock;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.internal.impl.InternalSession;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
@MojoTest
public class SourceJarMojoTest extends AbstractSourcePluginTestCase {

    private String[] addMavenDescriptor(String project, String... listOfElements) {
        final String metainf = "META-INF/";
        final String mavensource = "maven/source/maven-source-plugin-test-";
        int length = listOfElements.length;
        String[] result = new String[length + 5];
        System.arraycopy(listOfElements, 0, result, 0, length);
        result[length] = metainf + "maven/";
        result[length + 1] = metainf + "maven/source/";
        result[length + 2] = metainf + mavensource + project + "/";
        result[length + 3] = metainf + mavensource + project + "/pom.properties";
        result[length + 4] = metainf + mavensource + project + "/pom.xml";
        return result;
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("${basedir}/target/test-classes/unit/project-001")
    @MojoParameter(name = "classifier", value = "sources")
    void testDefaultConfiguration(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(getBasedir(), "target");
        assertSourceArchive(target, "project-001");
        assertJarContent(
                getSourceArchive(target, "project-001"),
                addMavenDescriptor(
                        "project-001",
                        "default-configuration.properties",
                        "foo/project001/App.java",
                        "foo/project001/",
                        "foo/",
                        "META-INF/MANIFEST.MF",
                        "META-INF/"));
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("${basedir}/target/test-classes/unit/project-003")
    public void testExcludes(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(getBasedir(), "target");
        assertSourceArchive(target, "project-003");
        assertJarContent(
                getSourceArchive(target, "project-003"),
                addMavenDescriptor(
                        "project-003",
                        "default-configuration.properties",
                        "foo/project003/App.java",
                        "foo/project003/",
                        "foo/",
                        "META-INF/MANIFEST.MF",
                        "META-INF/"));
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("${basedir}/target/test-classes/unit/project-005")
    public void testNoSources(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        // Now make sure that no archive got created
        final File expectedFile = getTestTargetDir("project-005");
        assertFalse(
                expectedFile.exists(),
                "Source archive should not have been created[" + expectedFile.getAbsolutePath() + "]");
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("${basedir}/target/test-classes/unit/project-007")
    public void testIncludes(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(getBasedir(), "target");
        assertSourceArchive(target, "project-007");
        assertJarContent(
                getSourceArchive(target, "project-007"),
                addMavenDescriptor(
                        "project-007",
                        "templates/configuration-template.properties",
                        "foo/project007/App.java",
                        "templates/",
                        "foo/project007/",
                        "foo/",
                        "META-INF/MANIFEST.MF",
                        "META-INF/"));
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("${basedir}/target/test-classes/unit/project-009")
    public void testIncludePom(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(getBasedir(), "target");
        assertSourceArchive(target, "project-009");
        assertJarContent(
                getSourceArchive(target, "project-009"),
                addMavenDescriptor(
                        "project-009",
                        "default-configuration.properties",
                        "pom.xml",
                        "foo/project009/App.java",
                        "foo/project009/",
                        "foo/",
                        "META-INF/MANIFEST.MF",
                        "META-INF/"));
    }

    @Test
    @InjectMojo(goal = "jar")
    @Basedir("${basedir}/target/test-classes/unit/project-010")
    public void testIncludeMavenDescriptorWhenExplicitlyConfigured(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(getBasedir(), "target");
        assertSourceArchive(target, "project-010");
        assertJarContent(
                getSourceArchive(target, "project-010"),
                addMavenDescriptor(
                        "project-010",
                        "default-configuration.properties",
                        "foo/project010/App.java",
                        "foo/project010/",
                        "foo/",
                        "META-INF/MANIFEST.MF",
                        "META-INF/"));
    }

    @Provides
    InternalSession createSession() {
        InternalSession session = SessionMock.getMockSession("target/local-repo");
        ProjectManager projectManager = mock(ProjectManager.class);
        when(session.getService(ProjectManager.class)).thenReturn(projectManager);
        when(projectManager.getCompileSourceRoots(any(), eq(ProjectScope.MAIN))).thenAnswer(iom -> {
            Project p = iom.getArgument(0, Project.class);
            return Collections.singletonList(
                    Paths.get(getBasedir()).resolve(p.getModel().getBuild().getSourceDirectory()));
        });
        when(projectManager.getResources(any(), eq(ProjectScope.MAIN))).thenAnswer(iom -> {
            Project p = iom.getArgument(0, Project.class);
            return p.getBuild().getResources().stream()
                    .map(r -> r.withDirectory(
                            Paths.get(getBasedir()).resolve(r.getDirectory()).toString()))
                    .toList();
        });
        return session;
    }
}
