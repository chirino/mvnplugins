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

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.fusesource.mvnplugins.uberize.UberEntry;

/**
 * Test for {@link PlexusComponents}.
 * 
 * @author Brett Porter
 * @version $Id$
 */
public class PlexusComponentsTest
    extends TestCase
{
    File basedir = new File("target/test-data/"+getClass().getName());
    private PlexusComponents transformerPlexus;

    public void setUp()
    {
        this.transformerPlexus = new PlexusComponents();
    }

    public void testConfigurationMerging() throws IOException
    {
        TreeMap<String, UberEntry> entries = new TreeMap<String, UberEntry>();
        final String path = PlexusComponents.COMPONENTS_XML_PATH;
        UberEntry uberEntry = new UberEntry(path);
        uberEntry.getSources().add(resourceToFile("/components-1.xml"));
        uberEntry.getSources().add(resourceToFile("/components-2.xml"));
        entries.put(path, uberEntry);
        transformerPlexus.process(basedir, entries);
        assertEquals( IOUtil.toString( getClass().getResourceAsStream( "/components-expected.xml" ) ),
                      FileUtils.fileRead( entries.get(path).getSource()) );
    }

    private File resourceToFile(String resource) throws IOException {
        final InputStream is = getClass().getResourceAsStream(resource);
        try {
            File file = new File(basedir, resource);
            file.getParentFile().mkdirs();
            final FileOutputStream os = new FileOutputStream(file);
            try {
                IOUtil.copy(is, os);
            } finally {
                IOUtil.close(os);
            }
            return file;
        } finally {
            IOUtil.close(is);
        }
    }
}
