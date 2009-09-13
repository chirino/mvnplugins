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
package org.fusesource.mvnplugins.uberize;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An UberEntry represents a file path in an uber jar.  It
 * will keep track of overlapping source files until
 * a transformation can apply a merge strategy to them.
 * When a transformation is applied, a new UberEntry will
 * replace the previous one but it will maintain a reference
 * to it so that the transformation history of the file path
 * can be inspected.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class UberEntry {

    private final String path;
    private final List<File> sources = new ArrayList<File>();
    private final List<UberEntry> previous;

    /**
     * Creates an UberEntry at located at the specified path.
     * @param path
     */
    public UberEntry(String path) {
        this(path, (List<UberEntry>)null);
    }

    /**
     * Creates an UberEntry that is an update of a previous
     * UberEntry.  The path of the new UberEntry will match
     * the previous one.
     *
     * @param previous
     */
    public UberEntry(UberEntry previous) {
        this(previous.getPath(), previous);
    }

    /**
     * Creates na UberEntry at located at the specified path, which
     * is an updated of a previous UberEntry.
     *
     * @param path
     */
    public UberEntry(String path, UberEntry previous) {
        this.path = path;
        this.previous = toList(previous);
    }

    static private List<UberEntry> toList(UberEntry entry) {
        if( entry == null ) {
            return null;
        }
        ArrayList rc = new ArrayList(1);
        rc.add(entry);
        return rc;
    }

    public UberEntry(String path, List<UberEntry> previous) {
        this.path = path;
        this.previous = previous;
    }

    /**
     * A list which can be used to track all the overlapping source files associated with the path entry.
     *
     * @return
     */
    public List<File> getSources() {
        return sources;
    }

    /**
     *
     * @return the source file associated with entry if there is only one, otherwise returns null.
     */
    public File getSource() {
        if( sources.size()!=1 ) {
            return null;
        }
        return sources.get(0);
    }

    /**
     * The path of the entry.
     *
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * @return The previous version of the UberEntry or null if this is the original version.
     */
    public UberEntry getPrevious() {
        if( previous==null || previous.isEmpty() ) {
            return null;
        }
        return previous.get(0);
    }

    /**
     * If a transformer agregates mutliple UberEntry paths into a single path
     * then the prvious version of thise node will be a list of UberEntrys
     * @return
     */
    public List<UberEntry> getAllPrevious() {
        if( previous==null ) {
            return null;
        }
        return previous;
    }

    public UberEntry addSource(File file) {
        sources.add(file);
        return this;
    }
}
