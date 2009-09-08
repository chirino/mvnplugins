package org.fusesource.mvnplugins.uberize;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.net.URLClassLoader;
import java.net.URL;

import junit.framework.TestCase;

import org.fusesource.mvnplugins.uberize.relocation.PackageRelocation;
import org.fusesource.mvnplugins.uberize.transformer.PlexusComponents;
import org.fusesource.mvnplugins.uberize.transformer.PackageShader;

/**
 * @author Jason van Zyl
 * @author Mauro Talevi
 */
public class DefaultUberizerTest extends TestCase {
    
    File base = new File("target");
    File workDir = new File(base, "uber");

    private static final String[] EXCLUDES = new String[]{
            "org/codehaus/plexus/util/xml/Xpp3Dom",
            "org/codehaus/plexus/util/xml/pull.*"};

    public void testGetParentDirs() {
        ArrayList<String> rc = new ArrayList<String>();
        DefaultUberizer.getParentDirs("a/b", rc);
        assertEquals(1, rc.size());
        assertEquals("a/", rc.get(0));
    }

    public void testShaderWithStaticInitializedClass() throws Exception {
        Uberizer uberizer = new DefaultUberizer();
        Set sources = new LinkedHashSet();
        sources.add(new File("src/test/jars/test-artifact-1.0-SNAPSHOT.jar"));

        List transformers = new ArrayList();
        PackageShader shader = new PackageShader();
        shader.relocations = new PackageRelocation[]{
            new PackageRelocation("org.fusesource.mvnplugins.uberize", null, null)
        };
        transformers.add(shader);

        List filters = new ArrayList();

        File uberJar = new File(base, "testShaderWithStaticInitializedClass.jar");
        uberizer.uberize(workDir, sources, uberJar, filters, transformers);

        URLClassLoader cl = new URLClassLoader(new URL[]{uberJar.toURI().toURL()});
        Class c = cl.loadClass("hidden.org.fusesource.mvnplugins.uberize.Lib");
        Object o = c.newInstance();
        assertEquals("foo.bar/baz", c.getDeclaredField("CONSTANT").get(o));
    }

    public void testShaderWithDefaultShadedPattern() throws Exception {
        shaderWithPattern(null, new File(base, "foo-default.jar"), EXCLUDES);
    }

    public void testShaderWithCustomShadedPattern() throws Exception {
        shaderWithPattern("org/shaded/plexus/util", new File("target/foo-custom.jar"), EXCLUDES);
    }

    public void testShaderWithoutExcludesShouldRemoveReferencesOfOriginalPattern() throws Exception {
        //FIXME:  shaded jar should not include references to org/codehaus/* (empty dirs) or org.codehaus.* META-INF files.
        shaderWithPattern("org/shaded/plexus/util", new File("target/foo-custom-without-excludes.jar"), new String[]{});
    }

    public void shaderWithPattern(String shadedPattern, File jar, String[] excludes) throws Exception {
        Uberizer s = new DefaultUberizer();

        Set set = new LinkedHashSet();
        set.add(new File("src/test/jars/test-project-1.0-SNAPSHOT.jar"));
        set.add(new File("src/test/jars/plexus-utils-1.4.1.jar"));


        PackageShader shader = new PackageShader();
        shader.relocations = new PackageRelocation[]{
                new PackageRelocation("org/codehaus/plexus/util", shadedPattern, Arrays.asList(excludes))
        };

        List transformers = new ArrayList();
        transformers.add(shader);
        transformers.add(new PlexusComponents());

        List filters = new ArrayList();

        s.uberize(workDir, set, jar, filters, transformers);
    }

}
