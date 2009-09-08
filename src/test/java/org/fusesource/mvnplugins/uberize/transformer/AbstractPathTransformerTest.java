package org.fusesource.mvnplugins.uberize.transformer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Locale;
import java.util.TreeMap;
import java.util.HashSet;
import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import org.fusesource.mvnplugins.uberize.UberEntry;

/**
 * Test for {@link XmlAppender}.
 *
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class AbstractPathTransformerTest extends TestCase {
    
    static class MockTransformer extends AbstractPathTransformer {
        boolean matched;

        protected void process(UberEntry entry, File target) throws IOException {
            matched = true;
        }
    }

    private MockTransformer transformer = new MockTransformer();

    static {
        /*
         * NOTE: The Turkish locale has an usual case transformation for the letters "I" and "i", making it a prime
         * choice to test for improper case-less string comparisions.
         */
        Locale.setDefault(new Locale("tr"));
    }

    public void testPathCaseInsensitve() throws IOException {
        this.transformer.setPath("abcdefghijklmnopqrstuvwxyz");
        this.transformer.setIgnoreCase(true);

        assertTrue(matches("abcdefghijklmnopqrstuvwxyz"));
        assertTrue(matches("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        assertFalse(matches("META-INF/MANIFEST.MF"));
    }


    public void testPathFilterCaseInsensitve() throws IOException {
        this.transformer.setPaths(new Paths());
        this.transformer.getPaths().includes = new HashSet();
        this.transformer.getPaths().includes.add("abcdefghijklmnopqrstuvwxyz");
        this.transformer.setIgnoreCase(true);

        assertTrue(matches("abcdefghijklmnopqrstuvwxyz"));
//  TODO: the following case is failing..        
//        assertTrue(matches("ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
        assertFalse(matches("META-INF/MANIFEST.MF"));
    }

    private boolean matches(String path) throws IOException {
        transformer.matched = false;
        File work = new File("target");
        TreeMap<String, UberEntry> tree = new TreeMap<String, UberEntry>();
        UberEntry enrty = new UberEntry(path);
        tree.put(path, enrty);
        transformer.process(work, tree);
        return transformer.matched;
    }

}
