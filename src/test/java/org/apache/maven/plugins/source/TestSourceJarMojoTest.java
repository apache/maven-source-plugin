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
public class TestSourceJarMojoTest extends AbstractSourcePluginTestCase {

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("${basedir}/target/test-classes/unit/project-001")
    @MojoParameter(name = "classifier", value = "test-sources")
    public void testDefaultConfiguration(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(getBasedir(), "target");
        assertTestSourceArchive(target, "project-001");
        assertJarContent(getTestSourceArchive(target, "project-001"), new String[] {
            "test-default-configuration.properties",
            "foo/project001/AppTest.java",
            "foo/project001/",
            "foo/",
            "META-INF/MANIFEST.MF",
            "META-INF/",
            "META-INF/maven/",
            "META-INF/maven/source/",
            "META-INF/maven/source/maven-source-plugin-test-project-001/",
            "META-INF/maven/source/maven-source-plugin-test-project-001/pom.properties",
            "META-INF/maven/source/maven-source-plugin-test-project-001/pom.xml"
        });
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("${basedir}/target/test-classes/unit/project-003")
    @MojoParameter(name = "classifier", value = "test-sources")
    public void testExcludes(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(getBasedir(), "target");
        assertTestSourceArchive(target, "project-003");
        assertJarContent(getTestSourceArchive(target, "project-003"), new String[] {
            "test-default-configuration.properties",
            "foo/project003/AppTest.java",
            "foo/project003/",
            "foo/",
            "META-INF/MANIFEST.MF",
            "META-INF/",
            "META-INF/maven/",
            "META-INF/maven/source/",
            "META-INF/maven/source/maven-source-plugin-test-project-003/",
            "META-INF/maven/source/maven-source-plugin-test-project-003/pom.properties",
            "META-INF/maven/source/maven-source-plugin-test-project-003/pom.xml"
        });
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("${basedir}/target/test-classes/unit/project-005")
    @MojoParameter(name = "classifier", value = "test-sources")
    public void testNoSources(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        // Now make sure that no archive got created
        final File expectedFile = getTestTargetDir("project-005");
        assertFalse(
                expectedFile.exists(),
                "Test source archive should not have been created[" + expectedFile.getAbsolutePath() + "]");
    }

    @Test
    @InjectMojo(goal = "test-jar")
    @Basedir("${basedir}/target/test-classes/unit/project-010")
    @MojoParameter(name = "classifier", value = "test-sources")
    public void testIncludeMavenDescriptorWhenExplicitlyConfigured(AbstractSourceJarMojo mojo) throws Exception {
        mojo.execute();

        File target = new File(getBasedir(), "target");
        assertTestSourceArchive(target, "project-010");
        assertJarContent(getTestSourceArchive(target, "project-010"), new String[] {
            "test-default-configuration.properties",
            "foo/project010/AppTest.java",
            "foo/project010/",
            "foo/",
            "META-INF/MANIFEST.MF",
            "META-INF/",
            "META-INF/maven/",
            "META-INF/maven/source/",
            "META-INF/maven/source/maven-source-plugin-test-project-010/",
            "META-INF/maven/source/maven-source-plugin-test-project-010/pom.xml",
            "META-INF/maven/source/maven-source-plugin-test-project-010/pom" + ".properties"
        });
    }

    @Provides
    InternalSession createSession() {
        InternalSession session = SessionMock.getMockSession("target/local-repo");
        ProjectManager projectManager = mock(ProjectManager.class);
        when(session.getService(ProjectManager.class)).thenReturn(projectManager);
        when(projectManager.getCompileSourceRoots(any(), eq(ProjectScope.TEST))).thenAnswer(iom -> {
            Project p = iom.getArgument(0, Project.class);
            return Collections.singletonList(
                    Paths.get(getBasedir()).resolve(p.getModel().getBuild().getTestSourceDirectory()));
        });
        when(projectManager.getResources(any(), eq(ProjectScope.TEST))).thenAnswer(iom -> {
            Project p = iom.getArgument(0, Project.class);
            return p.getBuild().getTestResources().stream()
                    .map(r -> r.withDirectory(
                            Paths.get(getBasedir()).resolve(r.getDirectory()).toString()))
                    .toList();
        });
        return session;
    }
}
