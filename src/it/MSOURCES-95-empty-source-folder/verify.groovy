
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

String sourcesJarFileName = 'empty-source-folder-1.0-SNAPSHOT-sources.jar'
String testSourcesJarFileName = "empty-source-folder-1.0-SNAPSHOT-test-sources.jar"

assert buildLog.exists()

// Make sure the jars are created on the first build
assert buildLog.text =~ / Building jar: .*${sourcesJarFileName}/
assert buildLog.text =~ / Building jar: .*${testSourcesJarFileName}/

// Make sure the jars are not re-created on subsequent builds
assert buildLog.text =~ / Archive .*${sourcesJarFileName} is uptodate/
assert buildLog.text =~ / Archive .*${testSourcesJarFileName} is uptodate/
