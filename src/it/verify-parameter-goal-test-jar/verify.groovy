
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

File buildLog = new File( basedir, 'build.log' )

assert buildLog.exists()

assert buildLog.text.contains('source:test-jar')
assert buildLog.text.contains('    archive')
assert buildLog.text.contains('    attach (Default: true)')
assert buildLog.text.contains('    classifier (Default: test-sources)')
assert buildLog.text.contains('    defaultManifestFile (Default:')
assert buildLog.text.contains('    ${project.build.outputDirectory}/META-INF/MANIFEST.MF)')
assert buildLog.text.contains('    excludeResources (Default: false)')
assert buildLog.text.contains('    excludes')
assert buildLog.text.contains('    finalName (Default: ${project.build.finalName})')
assert buildLog.text.contains('    forceCreation (Default: false)')
assert buildLog.text.contains('    includePom (Default: false)')
assert buildLog.text.contains('    includes')
assert buildLog.text.contains('    outputDirectory (Default: ${project.build.directory})')
assert buildLog.text.contains('    skipSource (Default: false)')
assert buildLog.text.contains('    useDefaultExcludes (Default: true)')
assert buildLog.text.contains('    useDefaultManifestFile (Default: false)')

// Make sure the session/reactorProjects will not be visible for users
assert !buildLog.text.contains('    session (Default: ${session})')
assert !buildLog.text.contains('    reactorProjects (Default: ${reactorProjects})')

