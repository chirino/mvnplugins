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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.jar.JarFile;
import java.net.URLClassLoader;
import java.net.URL;

import junit.framework.TestCase;

import org.fusesource.mvnplugins.uberize.relocation.PackageRelocation;
import org.fusesource.mvnplugins.uberize.transformer.PlexusComponents;
import org.fusesource.mvnplugins.uberize.transformer.ClassShader;
import org.fusesource.mvnplugins.uberize.transformer.Resources;
import org.codehaus.plexus.util.*;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.logging.Logger;

/**
 * @author Jason van Zyl
 * @author Mauro Talevi
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
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
        Uberizer uberizer = createUberizer();
        Set sources = new LinkedHashSet();
        sources.add(new File("src/test/jars/test-artifact-1.0-SNAPSHOT.jar"));

        List transformers = new ArrayList();
        ClassShader shader = new ClassShader();
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
        Uberizer s = createUberizer();

        Set set = new LinkedHashSet();
        set.add(new File("src/test/jars/test-project-1.0-SNAPSHOT.jar"));
        set.add(new File("src/test/jars/plexus-utils-1.4.1.jar"));


        ClassShader shader = new ClassShader();
        shader.relocations = new PackageRelocation[]{
            new PackageRelocation("org/codehaus/plexus/util", shadedPattern, Arrays.asList(excludes))
        };
        
        List transformers = new ArrayList();
        transformers.add(new PlexusComponents());
        transformers.add(shader);

        List filters = new ArrayList();
        s.uberize(workDir, set, jar, filters, transformers);
    }

    public void testShaderWithResourceTransformation() throws Exception {
        Uberizer uberizer = createUberizer();

        Set sources = new LinkedHashSet();
        sources.add(new File("src/test/jars/test-project-1.0-SNAPSHOT.jar"));

        List transformers = new ArrayList();
        ClassShader shader = new ClassShader();
        shader.relocations = new PackageRelocation[]{
            new PackageRelocation("org/component", "org/uber/component", null)
        };

        shader.resources = new Resources();
        shader.resources.includes = new HashSet();
        shader.resources.includes.add("META-INF/plexus/components.xml");

        transformers.add(new PlexusComponents());
        transformers.add(shader);

        List filters = new ArrayList();
        File uberJar = new File(base, "testShaderWithResourceTransformation.jar");
        uberizer.uberize(workDir, sources, uberJar, filters, transformers);


        JarFile jar = new JarFile(uberJar);
        InputStream is = jar.getInputStream(jar.getEntry("META-INF/plexus/components.xml"));
        String contnent = IOUtil.toString(is);

        assertFalse( contnent.contains("<role>org.component.PizzaComponent</role>") );
        assertFalse( contnent.contains("<implementation>org.component.DefaultPizzaComponent</implementation>") );

        assertTrue( contnent.contains("<role>org.uber.component.PizzaComponent</role>") );
        assertTrue( contnent.contains("<implementation>org.uber.component.DefaultPizzaComponent</implementation>") );
    }

    private DefaultUberizer createUberizer() {
        final DefaultUberizer rc = new DefaultUberizer();
        rc.enableLogging(new ConsoleLogger(Logger.LEVEL_INFO, "uberizer"));
        return rc;
    }

}
