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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Language;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.api.Type;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.Mojo;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ProjectManager;
import org.apache.maven.shared.archiver.MavenArchiveConfiguration;
import org.apache.maven.shared.archiver.MavenArchiver;
import org.apache.maven.shared.archiver.MavenArchiverException;

/**
 * Base class for bundling sources into a jar archive.
 *
 * @since 2.0.3
 */
public abstract class AbstractSourceJarMojo implements Mojo {
    private static final String[] DEFAULT_INCLUDES = new String[] {"**/**"};

    private static final String[] DEFAULT_EXCLUDES = new String[] {};

    /**
     * List of files to include. Specified as fileset patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     *
     * @since 2.1
     */
    @Parameter
    protected String[] includes;

    /**
     * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents
     * is being packaged into the JAR.
     *
     * @since 2.1
     */
    @Parameter
    protected String[] excludes;

    /**
     * Exclude commonly excluded files such as SCM configuration. These are defined in the plexus
     * FileUtils.getDefaultExcludes()
     *
     * @since 2.1
     */
    @Parameter(property = "maven.source.useDefaultExcludes", defaultValue = "true")
    protected boolean useDefaultExcludes;

    /**
     * Whether to follow symbolic links below the source directories. When false, a symbolic link is archived as a
     * link, which points nowhere once the jar is unpacked somewhere else; when true, a link is archived as the file
     * or directory it points at, and a linked directory's contents are included.
     *
     * @since 3.5.0
     */
    @Parameter(property = "maven.source.followSymlinks", defaultValue = "false")
    private boolean followSymlinks;

    /**
     * The Maven Project Object
     */
    @Inject
    protected Project project;

    /**
     * The archive configuration to use. See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
     * Archiver Reference</a>. <br/>
     * <b>Note: Since 3.0.0 the resulting archives contain a maven descriptor. If you need to suppress the generation of
     * the maven descriptor you can simply achieve this by using the
     * <a href="http://maven.apache.org/shared/maven-archiver/index.html#archive">archiver configuration</a></b>.
     *
     * @since 2.1
     */
    @Parameter
    protected MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Path to the default MANIFEST file to use. It will be used if <code>useDefaultManifestFile</code> is set to
     * <code>true</code>.
     *
     * @since 2.1
     */
    // CHECKSTYLE_OFF: LineLength
    @Parameter(
            defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF",
            readonly = false,
            required = true)
    // CHECKSTYLE_ON: LineLength
    protected Path defaultManifestFile;

    /**
     * Set this to <code>true</code> to enable the use of the <code>defaultManifestFile</code>. <br/>
     *
     * @since 2.1
     */
    @Parameter(property = "maven.source.useDefaultManifestFile", defaultValue = "false")
    protected boolean useDefaultManifestFile;

    /**
     * Specifies whether to attach the artifact to the project
     */
    @Parameter(property = "maven.source.attach", defaultValue = "true")
    protected boolean attach;

    /**
     * Specifies whether to exclude resources from the sources-jar. This can be convenient if your project
     * includes large resources, such as images, and you don't want to include them in the sources-jar.
     *
     * @since 2.0.4
     */
    @Parameter(property = "maven.source.excludeResources", defaultValue = "false")
    protected boolean excludeResources;

    /**
     * Specifies whether to include the POM file in the sources-jar.
     *
     * @since 2.1
     */
    @Parameter(property = "maven.source.includePom", defaultValue = "false")
    protected boolean includePom;

    /**
     * The directory where the generated archive file will be put.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected Path outputDirectory;

    /**
     * The filename to be used for the generated archive file. For the source:jar goal, "-sources" is appended to this
     * filename. For the source:test-jar goal, "-test-sources" is appended.
     */
    @Parameter(defaultValue = "${project.build.finalName}")
    protected String finalName;

    /**
     * Contains the full list of projects in the reactor.
     */
    @Parameter(defaultValue = "${session.projects}", readonly = true)
    protected List<Project> reactorProjects;

    /**
     * Whether creating the archive should be forced. If set to true, the jar will always be created. If set to false,
     * the jar will only be created when the sources are newer than the jar.
     *
     * @since 2.1
     */
    @Parameter(property = "maven.source.forceCreation", defaultValue = "false")
    protected boolean forceCreation;

    /**
     * A flag used to disable the source procedure. This is primarily intended for usage from the command line to
     * occasionally adjust the build.
     *
     * @since 2.2
     */
    @Parameter(property = "maven.source.skip", defaultValue = "false")
    protected boolean skipSource;

    /**
     * The Maven session.
     */
    @Inject
    protected Session session;

    /**
     * Timestamp for reproducible output archive entries, either formatted as ISO 8601
     * <code>yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like
     * <a href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
     *
     * @since 3.2.0
     */
    @Parameter(defaultValue = "${project.build.outputTimestamp}")
    protected String outputTimestamp;

    @Inject
    protected Log log;

    /**
     * Used for attaching the source jar to the project.
     */
    protected ProjectManager projectManager;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public Log getLog() {
        return log;
    }

    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoException {
        if (skipSource) {
            getLog().info("Skipping source per configuration.");
            return;
        }

        projectManager = session.getService(ProjectManager.class);
        doExecute();
    }

    protected void doExecute() {
        packageSources(project);
    }

    // ----------------------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------------------

    /**
     * @return the wanted classifier, ie <code>sources</code> or <code>test-sources</code>
     */
    protected abstract String getClassifier();

    /**
     * @param p {@link Project} not null
     * @return the compile or test sources
     * @throws MojoException in case of an error.
     */
    protected abstract List<Path> getSources(Project p) throws MojoException;

    /**
     * @param p {@link Project} not null
     * @return the compile or test resource roots
     * @throws MojoException in case of an error.
     */
    protected abstract List<SourceRoot> getResources(Project p) throws MojoException;

    /**
     * @param p {@link Project}
     * @throws MojoException in case of an error.
     */
    protected void packageSources(Project p) throws MojoException {
        String type = p.getPackaging().type().id();
        if (!Type.POM.equals(type) && !Type.BOM.equals(type)) {
            packageSources(Collections.singletonList(p));
        }
    }

    /**
     * Files map uses entry-name as key (unique in a JAR) and absolute source path as value.
     * This allows the same source file to appear under multiple entry names (e.g. pom.xml
     * at the root and META-INF/maven/.../pom.xml).
     *
     * @param theProjects {@link Project}
     * @throws MojoException in case of an error.
     */
    protected void packageSources(List<Project> theProjects) throws MojoException {
        Artifact currentProjectArtifact = project.getMainArtifact().get();
        if (!currentProjectArtifact.getClassifier().isEmpty()) {
            getLog().warn("NOT adding sources to artifacts with classifier as Maven only supports one classifier "
                    + "per artifact. Current artifact [" + currentProjectArtifact.key() + "] has a ["
                    + currentProjectArtifact.getClassifier() + "] classifier.");

            return;
        }

        try {
            // Collect all files: entryName -> absolutePath
            Map<String, Path> allFiles = new LinkedHashMap<>();

            // Add maven-shared-archive-resources if present
            projectManager
                    .getEnabledSourceRoots(project, ProjectScope.MAIN, Language.RESOURCES)
                    .map(SourceRoot::directory)
                    .filter(directory -> directory.endsWith("maven-shared-archive-resources"))
                    .forEach(directory -> {
                        try {
                            allFiles.putAll(SourceFileCollector.collect(
                                    directory,
                                    null,
                                    getCombinedIncludes(null),
                                    getCombinedExcludes(null),
                                    useDefaultExcludes,
                                    followSymlinks));
                        } catch (IOException e) {
                            throw new MojoException("Error collecting maven-shared-archive-resources", e);
                        }
                    });

            for (Project pItem : theProjects) {
                Project subProject = getProject(pItem);

                String type = subProject.getPackaging().type().id();
                if (Type.POM.equals(type) || Type.BOM.equals(type)) {
                    continue;
                }

                collectProjectContent(subProject, allFiles);
            }

            // Add maven descriptor (pom.xml + pom.properties)
            if (archive.isAddMavenDescriptor()) {
                addMavenDescriptor(allFiles);
            }

            if (!allFiles.isEmpty() || forceCreation) {

                if (useDefaultManifestFile && Files.exists(defaultManifestFile) && archive.getManifestFile() == null) {
                    getLog().info("Adding existing MANIFEST to archive. Found under: " + defaultManifestFile);
                    archive.setManifestFile(defaultManifestFile);
                }

                Path outputFile = outputDirectory.resolve(finalName + "-" + getClassifier() + getExtension());

                // Build the manifest
                Manifest manifest = buildManifest();

                getLog().debug("create archive " + outputFile);
                SourceToolExecutor.createJar(
                        outputFile, allFiles, manifest, archive.isCompress(), outputTimestamp, getLog());

                if (attach) {
                    ProducedArtifact artifact = session.createProducedArtifact(
                            project.getGroupId(),
                            project.getArtifactId(),
                            project.getVersion(),
                            getClassifier(),
                            null,
                            getType());
                    boolean requiresAttach = true;
                    for (Artifact attachedArtifact : projectManager.getAttachedArtifacts(project)) {
                        if (Objects.equals(artifact.key(), attachedArtifact.key())) {
                            Path attachedFile = session.getService(ArtifactManager.class)
                                    .getPath(attachedArtifact)
                                    .orElse(null);
                            if (attachedFile != null && !outputFile.equals(attachedFile)) {
                                getLog().error("Artifact " + attachedArtifact.key()
                                        + " already attached to a file " + relative(attachedFile) + ": attach to "
                                        + relative(outputFile) + " should be done with another classifier");
                                throw new MojoException("Presumably you have configured maven-source-plugin "
                                        + "to execute twice in your build to different output files. "
                                        + "You have to configure a classifier for at least one of them.");
                            }
                            requiresAttach = false;
                            getLog().info("Artifact " + attachedArtifact.key() + " already attached to "
                                    + relative(outputFile) + ": ignoring same re-attach (same artifact, same file)");
                        }
                    }
                    if (requiresAttach) {
                        projectManager.attachArtifact(project, artifact, outputFile);
                    }
                } else {
                    getLog().info("NOT adding java-sources to attached artifacts list.");
                }
            } else {
                getLog().info("No sources in project. Archive not created.");
            }
        } catch (IOException e) {
            throw new MojoException("Error creating source archive: " + e.getMessage(), e);
        }
    }

    private String relative(Path to) {
        Path basedir = project.getBasedir().toAbsolutePath();
        return basedir.relativize(to.toAbsolutePath()).toString();
    }

    /**
     * Collects files from the project's sources and resources.
     */
    protected void collectProjectContent(Project project, Map<String, Path> files) throws MojoException {
        try {
            if (includePom) {
                Path pom = project.getPomPath();
                files.put(pom.getFileName().toString(), pom);
            }

            for (Path sourceDirectory : getSources(project)) {
                if (Files.exists(sourceDirectory)) {
                    files.putAll(SourceFileCollector.collect(
                            sourceDirectory,
                            null,
                            getCombinedIncludes(null),
                            getCombinedExcludes(null),
                            useDefaultExcludes,
                            followSymlinks));
                }
            }

            for (SourceRoot resource : getResources(project)) {
                Path sourceDirectory = resource.directory();
                Path absoluteSourceDirectory = project.getBasedir().resolve(sourceDirectory);

                if (!Files.exists(absoluteSourceDirectory)) {
                    continue;
                }

                List<String> resourceIncludes = resource.includes();
                String[] combinedIncludes = getCombinedIncludes(resourceIncludes);
                List<String> resourceExcludes = resource.excludes();
                String[] combinedExcludes = getCombinedExcludes(resourceExcludes);

                String targetPath = resource.targetPath().map(Path::toString).orElse(null);
                String prefix = null;
                if (targetPath != null) {
                    prefix = targetPath.trim();
                    if (!prefix.endsWith("/")) {
                        prefix += "/";
                    }
                }

                files.putAll(SourceFileCollector.collect(
                        absoluteSourceDirectory,
                        prefix,
                        combinedIncludes,
                        combinedExcludes,
                        useDefaultExcludes,
                        followSymlinks));
            }
        } catch (IOException e) {
            throw new MojoException("Error collecting source files.", e);
        }
    }

    /**
     * Adds Maven descriptor files (pom.xml + pom.properties) to the files map.
     */
    private void addMavenDescriptor(Map<String, Path> files) throws IOException {
        String groupId = project.getGroupId();
        String artifactId = project.getArtifactId();
        String version = project.getVersion();
        String prefix = "META-INF/maven/" + groupId + "/" + artifactId + "/";

        // Add pom.xml
        Path pomPath = project.getPomPath();
        files.put(prefix + "pom.xml", pomPath);

        // Generate pom.properties
        Path buildDir = outputDirectory;
        Files.createDirectories(buildDir);
        Path pomPropertiesDir = buildDir.resolve("maven-archiver");
        Files.createDirectories(pomPropertiesDir);
        Path pomPropertiesFile = pomPropertiesDir.resolve("pom.properties");

        Properties p = new Properties();
        p.setProperty("groupId", groupId);
        p.setProperty("artifactId", artifactId);
        p.setProperty("version", version);

        // Write properties in a reproducible format (no timestamp comment, sorted)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        p.store(baos, null);
        String output = baos.toString(StandardCharsets.ISO_8859_1)
                .lines()
                .filter(line -> !line.startsWith("#"))
                .sorted()
                .collect(Collectors.joining("\n", "", "\n"));
        Files.writeString(pomPropertiesFile, output, StandardCharsets.ISO_8859_1);

        files.put(prefix + "pom.properties", pomPropertiesFile);
    }

    /**
     * Builds a {@link Manifest} for the source JAR.
     */
    private Manifest buildManifest() throws MojoException {
        MavenArchiver mavenArchiver = new MavenArchiver();
        mavenArchiver.setCreatedBy("Maven Source Plugin", "org.apache.maven.plugins", "maven-source-plugin");
        try {
            return mavenArchiver.getManifest(session, project, archive);
        } catch (MavenArchiverException e) {
            throw new MojoException("Error generating manifest.", e);
        }
    }

    /**
     * @return The extension {@code .jar}
     */
    protected String getExtension() {
        return ".jar";
    }

    /**
     * @param p {@link Project}
     * @return the execution project
     */
    protected Project getProject(Project p) {
        return projectManager.getExecutionProject(p).orElse(p);
    }

    /**
     * @return The type {@code java-source}
     */
    protected String getType() {
        return "java-source";
    }

    /**
     * Combines the includes parameter and additional includes. Defaults to {@link #DEFAULT_INCLUDES} If the
     * additionalIncludes parameter is null, it is not added to the combined includes.
     *
     * @param additionalIncludes The includes specified in the pom resources section
     * @return The combined array of includes.
     */
    private String[] getCombinedIncludes(List<String> additionalIncludes) {
        List<String> combinedIncludes = new ArrayList<>();

        if (includes != null && includes.length > 0) {
            combinedIncludes.addAll(Arrays.asList(includes));
        }

        if (additionalIncludes != null && !additionalIncludes.isEmpty()) {
            combinedIncludes.addAll(additionalIncludes);
        }

        // If there are no other includes, use the default.
        if (combinedIncludes.isEmpty()) {
            combinedIncludes.addAll(Arrays.asList(DEFAULT_INCLUDES));
        }

        return combinedIncludes.toArray(new String[0]);
    }

    /**
     * Combines the user parameter {@link #excludes}, the default excludes, and the contents of
     * the parameter additionalExcludes.
     *
     * @param additionalExcludes additional excludes to add to the array
     * @return the combined list of excludes
     */
    private String[] getCombinedExcludes(List<String> additionalExcludes) {
        List<String> combinedExcludes = new ArrayList<>();

        if (excludes != null && excludes.length > 0) {
            combinedExcludes.addAll(Arrays.asList(excludes));
        }

        if (additionalExcludes != null && !additionalExcludes.isEmpty()) {
            combinedExcludes.addAll(additionalExcludes);
        }

        if (combinedExcludes.isEmpty()) {
            combinedExcludes.addAll(Arrays.asList(DEFAULT_EXCLUDES));
        }

        return combinedExcludes.toArray(new String[0]);
    }

    /**
     * @return The current project.
     */
    protected Project getProject() {
        return project;
    }

    /**
     * @param project {@link Project}
     */
    protected void setProject(Project project) {
        this.project = project;
    }
}
