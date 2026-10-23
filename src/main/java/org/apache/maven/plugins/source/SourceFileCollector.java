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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects source files from a directory into a map of absolute file path to JAR entry name.
 */
final class SourceFileCollector {

    /**
     * Default excludes matching SCM metadata and OS-specific files,
     * equivalent to the plexus FileUtils default excludes.
     */
    static final String[] DEFAULT_EXCLUDES = {
        "**/*~",
        "**/#*#",
        "**/.#*",
        "**/%*%",
        "**/._*",
        "**/CVS",
        "**/CVS/**",
        "**/.cvsignore",
        "**/SCCS",
        "**/SCCS/**",
        "**/vssver.scc",
        "**/.svn",
        "**/.svn/**",
        "**/.arch-ids",
        "**/.arch-ids/**",
        "**/.bzr",
        "**/.bzr/**",
        "**/.bzrignore",
        "**/.MySCMServerInfo",
        "**/.DS_Store",
        "**/.metadata",
        "**/.metadata/**",
        "**/.hg",
        "**/.hg/**",
        "**/.hgignore",
        "**/.hgsub",
        "**/.hgsubstate",
        "**/.hgtags",
        "**/.git",
        "**/.git/**",
        "**/.gitignore",
        "**/.gitmodules",
        "**/.gitattributes",
        "**/Thumbs.db",
        "**/thumbs.db",
    };

    private SourceFileCollector() {}

    /**
     * Collect files from a source directory.
     *
     * @param sourceDir           absolute source directory
     * @param targetPathPrefix    prefix to prepend in JAR entry names (e.g. "resources/"), may be null or empty
     * @param includes            include patterns (Ant-style glob), null or empty means match everything
     * @param excludes            exclude patterns (Ant-style glob), null or empty means no excludes
     * @param useDefaultExcludes  whether to add default SCM excludes
     * @param followSymlinks      whether to follow symbolic links
     * @return map of JAR entry name to absolute file path
     * @throws IOException if an I/O error occurs during file walking
     */
    static Map<String, Path> collect(
            Path sourceDir,
            String targetPathPrefix,
            String[] includes,
            String[] excludes,
            boolean useDefaultExcludes,
            boolean followSymlinks)
            throws IOException {

        if (!Files.isDirectory(sourceDir)) {
            return new LinkedHashMap<>();
        }

        String prefix = targetPathPrefix != null ? targetPathPrefix : "";

        FileSystem fs = FileSystems.getDefault();
        List<PathMatcher> includeMatchers = toAntMatchers(fs, includes);
        List<PathMatcher> excludeMatchers = toAntMatchers(fs, excludes);
        if (useDefaultExcludes) {
            for (String pattern : DEFAULT_EXCLUDES) {
                addAntPattern(fs, excludeMatchers, pattern);
            }
        }

        Map<String, Path> result = new LinkedHashMap<>();
        Set<FileVisitOption> opts = followSymlinks ? Set.of(FileVisitOption.FOLLOW_LINKS) : Set.of();

        Files.walkFileTree(sourceDir, opts, Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path rel = sourceDir.relativize(file);
                String relStr = rel.toString().replace(File.separatorChar, '/');
                if (matchesAny(relStr, includeMatchers) && !matchesAny(relStr, excludeMatchers)) {
                    String entryName = prefix + relStr;
                    result.put(entryName, file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(sourceDir)) {
                    Path rel = sourceDir.relativize(dir);
                    String relStr = rel.toString().replace(File.separatorChar, '/');
                    // Check if the directory itself is excluded
                    if (matchesAny(relStr, excludeMatchers) || matchesAny(relStr + "/", excludeMatchers)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }

    /**
     * Converts Ant-style patterns to Java PathMatcher glob matchers.
     * For patterns starting with {@code ** /}, an additional matcher without the prefix is added
     * to handle root-level matching (Ant's {@code ** /} matches zero or more directories,
     * but Java's glob requires at least one path separator for {@code ** /}).
     */
    private static List<PathMatcher> toAntMatchers(FileSystem fs, String[] patterns) {
        List<PathMatcher> matchers = new ArrayList<>();
        if (patterns != null) {
            for (String pattern : patterns) {
                if (pattern != null && !pattern.isEmpty()) {
                    addAntPattern(fs, matchers, pattern);
                }
            }
        }
        return matchers;
    }

    /**
     * Adds an Ant-style pattern as one or two Java glob PathMatchers.
     * Patterns like {@code ** /x} need a secondary matcher for {@code x} alone,
     * because Java's glob {@code **} followed by {@code /} does not match zero path segments.
     */
    private static void addAntPattern(FileSystem fs, List<PathMatcher> matchers, String pattern) {
        matchers.add(fs.getPathMatcher("glob:" + pattern));
        // If pattern starts with **/, also match without the **/ prefix (root-level)
        if (pattern.startsWith("**/")) {
            String rest = pattern.substring(3);
            if (!rest.isEmpty()) {
                matchers.add(fs.getPathMatcher("glob:" + rest));
            }
        }
    }

    private static boolean matchesAny(String relativePath, List<PathMatcher> matchers) {
        if (matchers.isEmpty()) {
            return true; // empty means match everything (for includes) — caller handles logic
        }
        Path relPath = Path.of(relativePath);
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(relPath)) {
                return true;
            }
        }
        return false;
    }
}
