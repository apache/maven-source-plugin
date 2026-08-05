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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.api.Project;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.plugin.Log;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AbstractSourceJarMojoTest {

    private static final String SHARED_ARCHIVE_RESOURCES = "maven-shared-archive-resources";

    @Test
    void testSharedArchiveResourcesDirectoryIsAdded(@TempDir Path tempDir) throws Exception {
        Path sharedArchiveResources = Files.createDirectory(tempDir.resolve(SHARED_ARCHIVE_RESOURCES));

        JarArchiver jarArchiver = mock(JarArchiver.class);
        AbstractSourceJarMojo mojo = mojoWithResourceDirectories(jarArchiver, sharedArchiveResources);

        mojo.createArchiver();

        ArgumentCaptor<FileSet> fileSet = ArgumentCaptor.forClass(FileSet.class);
        verify(jarArchiver).addFileSet(fileSet.capture());
        assertEquals(sharedArchiveResources.toFile(), fileSet.getValue().getDirectory());
    }

    /**
     * A directory is the shared archive resources directory only when that is its name. A path that merely ends with
     * those characters, such as one produced by appending a suffix to an unrelated directory, is a different directory
     * and must not be added.
     */
    @Test
    void testDirectoryEndingWithSharedArchiveResourcesIsNotAdded(@TempDir Path tempDir) throws Exception {
        Path suffixedName = Files.createDirectory(tempDir.resolve("tmp-" + SHARED_ARCHIVE_RESOURCES));

        JarArchiver jarArchiver = mock(JarArchiver.class);
        AbstractSourceJarMojo mojo = mojoWithResourceDirectories(jarArchiver, suffixedName);

        mojo.createArchiver();

        verifyNoInteractions(jarArchiver);
    }

    @Test
    void testOnlySharedArchiveResourcesDirectoryIsAddedWhenBothArePresent(@TempDir Path tempDir) throws Exception {
        Path suffixedName = Files.createDirectory(tempDir.resolve("tmp-" + SHARED_ARCHIVE_RESOURCES));
        Path sharedArchiveResources = Files.createDirectory(tempDir.resolve(SHARED_ARCHIVE_RESOURCES));

        JarArchiver jarArchiver = mock(JarArchiver.class);
        AbstractSourceJarMojo mojo = mojoWithResourceDirectories(jarArchiver, suffixedName, sharedArchiveResources);

        mojo.createArchiver();

        ArgumentCaptor<FileSet> fileSet = ArgumentCaptor.forClass(FileSet.class);
        verify(jarArchiver).addFileSet(fileSet.capture());
        assertEquals(sharedArchiveResources.toFile(), fileSet.getValue().getDirectory());
    }

    private static AbstractSourceJarMojo mojoWithResourceDirectories(JarArchiver jarArchiver, Path... directories) {
        List<Resource> resources = Arrays.stream(directories)
                .map(directory ->
                        Resource.newBuilder().directory(directory.toString()).build())
                .collect(Collectors.toList());

        Project project = mock(Project.class);
        when(project.getBuild())
                .thenReturn(Build.newBuilder().resources(resources).build());

        AbstractSourceJarMojo mojo = new SourceJarMojo();
        mojo.project = project;
        mojo.jarArchiver = jarArchiver;
        mojo.log = mock(Log.class);
        return mojo;
    }
}
