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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.spi.ToolProvider;

import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.shared.archiver.MavenArchiver;

/**
 * Creates a JAR archive using the JDK {@code jar} tool.
 */
final class SourceToolExecutor {

    /** Minimum JDK feature version supporting {@code --date} in the jar tool. */
    static final int JDK_SUPPORTING_DATE = 19;

    private SourceToolExecutor() {}

    /**
     * Creates a JAR file using the JDK jar tool with a staging directory.
     *
     * @param outputFile      the JAR file to create
     * @param files           map of JAR entry name to absolute file path
     * @param manifest        the manifest for the JAR
     * @param compress        whether to compress entries
     * @param outputTimestamp the build output timestamp (may be null)
     * @param log             the mojo logger
     * @throws MojoException if the jar tool is not found or fails
     * @throws IOException   if an I/O error occurs
     */
    static void createJar(
            Path outputFile,
            Map<String, Path> files,
            Manifest manifest,
            boolean compress,
            String outputTimestamp,
            Log log)
            throws MojoException, IOException {

        ToolProvider jarTool =
                ToolProvider.findFirst("jar").orElseThrow(() -> new MojoException("JDK jar tool not found"));

        // Ensure output directory exists
        Path outputDir = outputFile.getParent();
        if (outputDir != null) {
            Files.createDirectories(outputDir);
        }

        // Create staging directory
        Path stagingDir = Files.createTempDirectory(outputDir, "sources-staging-");
        Path manifestFile = null;
        Path argFile = null;

        try {
            // Copy all files to staging maintaining entry name structure
            for (Map.Entry<String, Path> entry : files.entrySet()) {
                String entryName = entry.getKey();
                Path absPath = entry.getValue();
                Path target = stagingDir.resolve(entryName);
                Path targetParent = target.getParent();
                if (targetParent != null) {
                    Files.createDirectories(targetParent);
                }
                Files.copy(absPath, target);
            }

            // Write manifest to temp file
            manifestFile = Files.createTempFile(outputDir, "MANIFEST", ".MF");
            try (OutputStream os = Files.newOutputStream(manifestFile)) {
                manifest.write(os);
            }

            // Build argfile
            argFile = Files.createTempFile(outputDir, "jar-args", ".txt");
            try (BufferedWriter w = Files.newBufferedWriter(argFile, StandardCharsets.UTF_8)) {
                w.write("--create");
                w.newLine();
                if (!compress) {
                    w.write("--no-compress");
                    w.newLine();
                }

                Optional<Instant> ts = MavenArchiver.parseBuildOutputTimestamp(outputTimestamp);
                if (ts.isPresent() && Runtime.version().feature() >= JDK_SUPPORTING_DATE) {
                    w.write("--date");
                    w.newLine();
                    // Format as ISO-8601 with UTC offset
                    String isoDate = ts.get().toString();
                    if (isoDate.endsWith("Z")) {
                        isoDate = isoDate.substring(0, isoDate.length() - 1) + "+00:00";
                    }
                    w.write(isoDate);
                    w.newLine();
                }

                w.write("--manifest");
                w.newLine();
                w.write(manifestFile.toAbsolutePath().toString());
                w.newLine();
                w.write("--file");
                w.newLine();
                w.write(outputFile.toAbsolutePath().toString());
                w.newLine();
                w.write("-C");
                w.newLine();
                w.write(stagingDir.toAbsolutePath().toString());
                w.newLine();
                w.write(".");
                w.newLine();
            }

            // Run jar tool
            StringWriter outWriter = new StringWriter();
            StringWriter errWriter = new StringWriter();
            PrintWriter out = new PrintWriter(outWriter);
            PrintWriter err = new PrintWriter(errWriter);

            log.debug("Running jar tool with @" + argFile);
            int rc = jarTool.run(out, err, "@" + argFile.toAbsolutePath());

            out.flush();
            err.flush();

            String stdout = outWriter.toString().trim();
            String stderr = errWriter.toString().trim();

            if (!stdout.isEmpty()) {
                log.debug("jar tool stdout: " + stdout);
            }
            if (!stderr.isEmpty()) {
                log.debug("jar tool stderr: " + stderr);
            }

            if (rc != 0) {
                throw new MojoException("JDK jar tool failed (exit code " + rc + "): " + stderr);
            }

            log.debug("Created JAR: " + outputFile);
        } finally {
            // Clean up staging directory
            deleteRecursively(stagingDir);
            // Clean up temp files
            if (manifestFile != null) {
                Files.deleteIfExists(manifestFile);
            }
            if (argFile != null) {
                Files.deleteIfExists(argFile);
            }
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
