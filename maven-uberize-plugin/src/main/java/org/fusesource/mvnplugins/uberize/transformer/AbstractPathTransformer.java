package org.fusesource.mvnplugins.uberize.transformer;

import org.fusesource.mvnplugins.uberize.UberEntry;
import org.fusesource.mvnplugins.uberize.DefaultUberizer;
import org.fusesource.mvnplugins.uberize.Uberizer;
import org.fusesource.mvnplugins.uberize.Transformer;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Map.Entry;

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

/**
 * Base class which allows applying the transformer to a user
 * configured path or path filter.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
abstract public class AbstractPathTransformer extends Resources implements Transformer
{

    public void process(Uberizer uberizer, File workDir, TreeMap<String, UberEntry> uberEntries) throws IOException {
        // process all the entries that match.
        // this can be probably optimized so we don't iterate through all the entries to find matches.
        for (UberEntry uberEntry : new ArrayList<UberEntry>(uberEntries.values())) {
            if( uberEntry.getSources().isEmpty() ) {
                continue;
            }
            final boolean match = matches(uberEntry.getPath());
            if (match) {
                File target = DefaultUberizer.prepareFile(workDir, uberEntry.getPath());
                UberEntry modEntry = process(uberizer, uberEntry, target);
                if( modEntry !=null ) {
                    uberEntries.put(uberEntry.getPath(), modEntry);
                } else {
                    uberEntries.remove(uberEntry.getPath());
                }
            }
        }
    }

    abstract protected UberEntry process(Uberizer uberizer, UberEntry entry, File target) throws IOException;

}