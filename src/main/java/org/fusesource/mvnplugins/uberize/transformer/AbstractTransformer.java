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
import java.util.ArrayList;
import java.util.Map.Entry;

import org.fusesource.mvnplugins.uberize.Transformer;
import org.fusesource.mvnplugins.uberize.UberEntry;
import org.fusesource.mvnplugins.uberize.DefaultUberizer;
import org.fusesource.mvnplugins.uberize.Uberizer;

/**
 * Handy base class for simple transformers.
 * 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
abstract public class AbstractTransformer implements Transformer
{
    public void process(Uberizer uberizer, File workDir, TreeMap<String, UberEntry> uberEntries) throws IOException {
        for (UberEntry entry : new ArrayList<UberEntry>(uberEntries.values())) {
            if( entry.getSources().isEmpty() ) {
                continue;
            }
            String entryPath = entry.getPath();
            if (matches(entryPath)) {
                File target = DefaultUberizer.prepareFile(workDir, entryPath);
                UberEntry modEntry = process(uberizer, entry, target);
                if( modEntry!=null ) {
                    uberEntries.put(entryPath, modEntry);
                } else {
                    uberEntries.remove(entryPath);
                }
            }
        }
    }

    abstract protected boolean matches(String entryPath);

    abstract protected UberEntry process(Uberizer uberizer, UberEntry entry, File target) throws IOException;

}