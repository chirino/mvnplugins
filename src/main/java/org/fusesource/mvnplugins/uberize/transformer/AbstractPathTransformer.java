package org.fusesource.mvnplugins.uberize.transformer;

import org.fusesource.mvnplugins.uberize.UberEntry;
import org.fusesource.mvnplugins.uberize.DefaultUberizer;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.ArrayList;

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
 */
abstract public class AbstractPathTransformer extends AbstractTransformer
{
    private String path;
    private Paths paths;
    private boolean ignoreCase;

    public void process(File workDir, TreeMap<String, UberEntry> uberEntries) throws IOException {
        if( getPath() !=null && !isIgnoreCase() && getPaths() ==null ) {
            // In the simple single path, case senstive case, we can just directly lookup
            // the entry vs. matching against all the entries.
            UberEntry uberEntry = uberEntries.get(getPath());
            if( uberEntry!=null ) {
                _process(workDir, uberEntries, uberEntry);
            }
        } else {
            // process all the entries that match.
            for (UberEntry uberEntry : new ArrayList<UberEntry>(uberEntries.values())) {
                final boolean match = matches(uberEntry.getPath());
                if (match) {
                    _process(workDir, uberEntries, uberEntry);
                }
            }
        }
    }

    private void _process(File workDir, TreeMap<String, UberEntry> uberEntries, UberEntry uberEntry) throws IOException {
        File target = DefaultUberizer.prepareFile(workDir, uberEntry.getPath());
        process(uberEntry, target);
        if( target.exists() ) {
            final UberEntry modEntry = new UberEntry(uberEntry);
            modEntry.getSources().add(target);
            uberEntries.put(uberEntry.getPath(), modEntry);
        } else {
            uberEntries.remove(uberEntry.getPath());
        }
    }

    protected boolean matches(String entryPath) {
        if(isIgnoreCase()) {
            return (getPath() != null && getPath().equalsIgnoreCase(entryPath))
                    || (getPaths() != null && getPaths().matchesIgnoreCase(entryPath));
        }
        return ( getPath() !=null && getPath().equals(entryPath))
                 || (getPaths() !=null && getPaths().matches(entryPath) );
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Paths getPaths() {
        return paths;
    }

    public void setPaths(Paths paths) {
        this.paths = paths;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }
}