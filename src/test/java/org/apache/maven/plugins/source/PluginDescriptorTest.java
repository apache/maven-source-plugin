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

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the parameters and defaults the generated plugin descriptor declares for every source goal. The
 * {@code help} goal prints exactly these names and defaults, and hides the parameters that are not editable.
 */
class PluginDescriptorTest {

    private static final Map<String, String> GOAL_CLASSIFIERS = new LinkedHashMap<>();

    static {
        GOAL_CLASSIFIERS.put("jar", "sources");
        GOAL_CLASSIFIERS.put("jar-no-fork", "sources");
        GOAL_CLASSIFIERS.put("test-jar", "test-sources");
        GOAL_CLASSIFIERS.put("test-jar-no-fork", "test-sources");
        GOAL_CLASSIFIERS.put("generated-test-jar", "test-sources");
    }

    private static Document descriptor;

    @BeforeAll
    static void loadDescriptor() throws Exception {
        try (InputStream in = PluginDescriptorTest.class.getResourceAsStream("/META-INF/maven/plugin.xml")) {
            assertNotNull(in, "plugin.xml is generated before the tests run");
            descriptor =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        }
    }

    @Test
    void testUserParametersAndDefaults() {
        GOAL_CLASSIFIERS.forEach((goal, classifier) -> {
            Element mojo = mojo(goal);

            assertEquals("${project.build.finalName}", defaultValue(mojo, "finalName"), goal);
            assertEquals("${project.build.directory}", defaultValue(mojo, "outputDirectory"), goal);
            assertEquals(classifier, defaultValue(mojo, "classifier"), goal);
            assertEquals("true", defaultValue(mojo, "attach"), goal);
            assertEquals("false", defaultValue(mojo, "excludeResources"), goal);
            assertEquals("false", defaultValue(mojo, "forceCreation"), goal);
            assertEquals("false", defaultValue(mojo, "includePom"), goal);
            assertEquals("false", defaultValue(mojo, "skipSource"), goal);
            assertEquals("true", defaultValue(mojo, "useDefaultExcludes"), goal);
            assertEquals("false", defaultValue(mojo, "useDefaultManifestFile"), goal);
            assertEquals(
                    "${project.build.outputDirectory}/META-INF/MANIFEST.MF",
                    defaultValue(mojo, "defaultManifestFile"),
                    goal);

            for (String name : new String[] {"archive", "excludes", "includes"}) {
                assertEquals("true", parameterField(mojo, name, "editable"), goal + " " + name);
            }
        });
    }

    @Test
    void testInternalParametersAreNotConfigurable() {
        for (String goal : GOAL_CLASSIFIERS.keySet()) {
            Element mojo = mojo(goal);
            for (String name : new String[] {"session", "reactorProjects", "project"}) {
                assertEquals("false", parameterField(mojo, name, "editable"), goal + " " + name);
            }
        }
    }

    private static Element mojo(String goal) {
        NodeList mojos = descriptor.getElementsByTagName("mojo");
        for (int i = 0; i < mojos.getLength(); i++) {
            Element mojo = (Element) mojos.item(i);
            if (goal.equals(text(child(mojo, "goal")))) {
                return mojo;
            }
        }
        throw new AssertionError("No goal " + goal);
    }

    private static String defaultValue(Element mojo, String name) {
        Element field = child(child(mojo, "configuration"), name);
        assertTrue(field.hasAttribute("default-value"), "No default for " + name);
        return field.getAttribute("default-value");
    }

    private static String parameterField(Element mojo, String name, String field) {
        NodeList parameters = child(mojo, "parameters").getElementsByTagName("parameter");
        for (int i = 0; i < parameters.getLength(); i++) {
            Element parameter = (Element) parameters.item(i);
            if (name.equals(text(child(parameter, "name")))) {
                return text(child(parameter, field));
            }
        }
        throw new AssertionError("No parameter " + name);
    }

    private static Element child(Element parent, String name) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && name.equals(n.getNodeName())) {
                return (Element) n;
            }
        }
        throw new AssertionError("No element " + name + " in " + parent.getNodeName());
    }

    private static String text(Element e) {
        return e.getTextContent().trim();
    }
}
