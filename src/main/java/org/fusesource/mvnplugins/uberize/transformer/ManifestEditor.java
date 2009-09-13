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
package org.fusesource.mvnplugins.uberize.transformer;

import org.fusesource.mvnplugins.uberize.DefaultUberizer;
import org.fusesource.mvnplugins.uberize.Transformer;
import org.fusesource.mvnplugins.uberize.UberEntry;
import org.fusesource.mvnplugins.uberize.Uberizer;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Allows you to change the first MANIFEST.MF that is found in the 
 * set of JARs being processed.
 *
 * @author Jason van Zyl
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @since 1.2
 */
public class ManifestEditor implements Transformer {
    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    // Configuration
    private String mainClass;
    private Map manifestEntries;

    public void process(Uberizer uberizer, File workDir, TreeMap<String, UberEntry> uberEntries) throws IOException {

        Manifest manifest;
        final UberEntry uberEntry = uberEntries.get(MANIFEST_PATH);
        if (uberEntry != null && uberEntry.getSources().size() > 0) {
            // We just want to take the first manifest we come across as that's our project's manifest.
            FileInputStream is = new FileInputStream(uberEntry.getSources().get(0));
            try {
                manifest = new Manifest(is);
            } finally {
                IOUtil.close(is);
            }
        } else {
            manifest = new Manifest();
        }

        Attributes attributes = manifest.getMainAttributes();
        if (mainClass != null) {
            attributes.put(Attributes.Name.MAIN_CLASS, mainClass);
        }

        if (manifestEntries != null) {
            for (Iterator i = manifestEntries.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                attributes.put(new Attributes.Name(key), manifestEntries.get(key));
            }
        }

        File targetFile = DefaultUberizer.prepareFile(workDir, MANIFEST_PATH);
        FileOutputStream os = new FileOutputStream(targetFile);
        try {
            manifest.write(os);
        } finally {
            IOUtil.close(os);
        }
        UberEntry modEntry = new UberEntry(MANIFEST_PATH, uberEntry).addSource(targetFile);
        uberEntries.put(MANIFEST_PATH, modEntry);
        
    }

}
