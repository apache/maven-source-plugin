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
package org.apache.maven.plugins.source.stubs;

import java.io.File;

import org.apache.maven.api.plugin.testing.MojoExtension;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class ProjectStub extends MavenProject {

    public ProjectStub(File tempDir) {
        setArtifact(new DefaultArtifact(
                "groupId", "artifactId", "1.0", "compile", "jar", null, new DefaultArtifactHandler("jar")));

        setGroupId("groupId");
        setArtifactId("maven-source-plugin-test");
        setVersion("99.0");
        getBuild().setFinalName("maven-source-plugin-test-99.0");
        setFile(MojoExtension.getTestFile("pom.xml"));

        getBuild().setDirectory(new File(tempDir, "target").getAbsolutePath());

        addCompileSourceRoot(MojoExtension.getTestPath("src/main/java"));
        addTestCompileSourceRoot(MojoExtension.getTestPath("src/test/java"));

        Resource mainResource = new Resource();
        mainResource.setDirectory(MojoExtension.getTestPath("src/main/resources"));
        addResource(mainResource);

        Resource testResource = new Resource();
        testResource.setDirectory(MojoExtension.getTestPath("src/test/resources"));
        addTestResource(testResource);
    }
}
