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
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractSourcePluginTestCase {

    protected static final String FINAL_NAME_PREFIX = "maven-source-plugin-test-";

    protected static final String FINAL_NAME_SUFFIX = "-99.0";

    protected void assertSourceArchive(final File testTargetDir, final String projectName) {
        final File expectedFile = getSourceArchive(testTargetDir, projectName);
        assertTrue(expectedFile.exists(), "Source archive does not exist[" + expectedFile.getAbsolutePath() + "]");
    }

    protected void assertTestSourceArchive(final File testTargetDir, final String projectName) {
        final File expectedFile = getTestSourceArchive(testTargetDir, projectName);
        assertTrue(expectedFile.exists(), "Test source archive does not exist[" + expectedFile.getAbsolutePath() + "]");
    }

    protected File getSourceArchive(final File testTargetDir, final String projectName) {
        return new File(testTargetDir, buildFinalSourceName(projectName) + ".jar");
    }

    protected File getTestSourceArchive(final File testTargetDir, final String projectName) {
        return new File(testTargetDir, buildFinalTestSourceName(projectName) + ".jar");
    }

    protected String buildFinalSourceName(final String projectName) {
        return FINAL_NAME_PREFIX + projectName + FINAL_NAME_SUFFIX + "-sources";
    }

    protected String buildFinalTestSourceName(final String projectName) {
        return FINAL_NAME_PREFIX + projectName + FINAL_NAME_SUFFIX + "-test-sources";
    }

    protected void assertJarContent(final File jarFile, final String[] expectedFiles) throws IOException {
        try (ZipFile jar = new ZipFile(jarFile)) {
            Enumeration<? extends ZipEntry> entries = jar.entries();

            if (expectedFiles.length == 0) {
                assertFalse(entries.hasMoreElements(), "Jar file should not contain any entry");
            } else {
                assertTrue(entries.hasMoreElements());

                Set<String> expected = new TreeSet<>(Arrays.asList(expectedFiles));

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

                    assertTrue(expected.remove(entry.getName()), "Not expecting " + entry.getName() + " in " + jarFile);
                }

                assertTrue(expected.isEmpty(), "Missing entries " + expected + " in " + jarFile);
            }
        }
    }

    protected File getTestTargetDir(String projectName) {
        return new File(getBasedir(), "target/test-classes/unit/" + projectName + "/target");
    }
}
