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

import java.io.IOException;
import java.io.File;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.fusesource.mvnplugins.uberize.Transformer;
import org.fusesource.mvnplugins.uberize.UberEntry;
import org.fusesource.mvnplugins.uberize.DefaultUberizer;

/**
 * Handy base class for simple transformers.
 */
abstract public class AbstractTransformer implements Transformer
{
    public void process(File workDir, TreeMap<String, UberEntry> uberEntries) throws IOException {
        for (Entry<String, UberEntry> entry : new TreeMap<String, UberEntry>(uberEntries).entrySet()) {
            String entryPath = entry.getKey();
            if (matches(entryPath)) {
                File target = DefaultUberizer.prepareFile(workDir, entryPath);
                process(entry.getValue(), target);
                if( target.exists() ) {
                    final UberEntry modEntry = new UberEntry(entry.getValue());
                    modEntry.getSources().add(target);
                    uberEntries.put(entryPath, modEntry);
                } else {
                    uberEntries.remove(entryPath);
                }
            }
        }
    }

    abstract protected boolean matches(String entryPath);

    abstract protected void process(UberEntry entry, File target) throws IOException;

}