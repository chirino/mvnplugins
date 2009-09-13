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

import org.fusesource.mvnplugins.uberize.filter.Filter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;

/**
 * @author Jason van Zyl
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface Uberizer {

    String ROLE = Uberizer.class.getName();

    /**
     * Creates an uber jar from the source jars.  The work directory
     * can be used to stage data.
     *
     * @param workDir
     * @param sourceJars
     * @param uberJar
     * @param filters
     * @param transformers
     * @throws IOException
     */
    void uberize(File workDir, Set sourceJars, File uberJar,
                 List<Filter> filters, List<Transformer> transformers) throws IOException;

    /**
     * When a transformation can't aggregate multiple sources
     * for an entry.. this method asks the Uberizer to pick one source for it
     * to use.
     *
     * @param tree
     * @param entry
     * @return
     */
    File pickOneSource(TreeMap<String, UberEntry> tree, UberEntry entry);

    /**
     * Transformations which re-map classes should updated this map.  It's a map
     * of 'original class name' to 'new class name'.
     *
     * @return the classes that be been relocated.
     */
    public HashMap<String, String>  getClassRelocations();

}
