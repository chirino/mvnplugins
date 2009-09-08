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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.fusesource.mvnplugins.uberize.DefaultUberizer;
import org.fusesource.mvnplugins.uberize.Transformer;
import org.fusesource.mvnplugins.uberize.UberEntry;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A transformer that aggregates plexus <code>components.xml</code> files.
 */
public class PlexusComponents
        implements Transformer {
    private Map components = new LinkedHashMap();

    public static final String COMPONENTS_XML_PATH = "META-INF/plexus/components.xml";


    public void process(File workDir, TreeMap<String, UberEntry> uberEntries) throws IOException {
        UberEntry uberEntry = uberEntries.get(COMPONENTS_XML_PATH);

        // This transformer only needs to kick in when there is
        // more than 1 components.xml file that needs to get aggregated.
        if (uberEntry == null || uberEntry.getSources().size() == 1) {
            return;
        }

        // Load and merge the components from all the files...
        LinkedHashMap<String, Xpp3Dom> components = merge(uberEntry.getSources());

        // Write the merged components into a new XML file
        File aggregatedFile = DefaultUberizer.prepareFile(workDir, COMPONENTS_XML_PATH);
        Writer writer = WriterFactory.newXmlWriter(aggregatedFile);
        try {
            Xpp3Dom dom = new Xpp3Dom("component-set");
            Xpp3Dom componentDom = new Xpp3Dom("components");
            dom.addChild(componentDom);

            for (Xpp3Dom component : components.values()) {
                componentDom.addChild(component);
            }

            Xpp3DomWriter.write(writer, dom);
        }
        finally {
            IOUtil.close(writer);
        }

        // Update the entry tree
        UberEntry modEntry = new UberEntry(uberEntry);
        modEntry.getSources().add(aggregatedFile);
        uberEntries.put(modEntry.getPath(), modEntry);
    }

    private LinkedHashMap<String, Xpp3Dom> merge(List<File> files) throws IOException {
        LinkedHashMap<String, Xpp3Dom> components = new LinkedHashMap<String, Xpp3Dom>();
        for (File file : files) {

            Xpp3Dom dom;
            try {
                dom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(file));
            } catch (Exception e) {
                throw new IOException("Error parsing components.xml in " + file);
            }

            // Only try to merge in components if there are some elements in the component-set
            if (dom.getChild("components") == null) {
                continue;
            }

            for (Xpp3Dom component : dom.getChild("components").getChildren("component")) {

                String role = component.getChild("role").getValue();
                Xpp3Dom child = component.getChild("role-hint");
                String roleHint = child != null ? child.getValue() : "";
                String key = role + roleHint;

                Xpp3Dom previous = (Xpp3Dom) components.get(key);
                if (previous!=null) {
                    // TODO: use the tools in Plexus to merge these properly. For now, I just need an all-or-nothing
                    // configuration carry over
                    if (previous.getChild("configuration") != null) {
                        component.addChild(previous.getChild("configuration"));
                    }
                }
                components.put(key, component);
            }
        }
        return components;
    }

}
