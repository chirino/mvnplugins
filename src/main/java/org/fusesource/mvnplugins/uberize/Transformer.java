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
import java.io.IOException;
import java.util.TreeMap;

/**
 * Transformer implementations are used to tranform the content and structure
 * of an uber jar before it is finalized.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface Transformer {

    /**
     * A transformer will modify the specified uber entries to apply transformation.  It can
     * add/remove entries from the map or replace existing uber entries.
     *
     * A transformer should not modify exisiting uber entries.  It should instead create new
     * enry instance (linked to the old one) and replace the old entry in the map with the
     * new entry.
     *
     * @param uberizer the Uberizer instance requesting the transformation.
     * @param workDir a work directory that the transformer can store transformed files in
     * @param uberEntries a map of all the jar entries that will be included in the uber jar
     * @throws IOException
     */
    public void process(Uberizer uberizer, File workDir, TreeMap<String, UberEntry> uberEntries) throws IOException;

}
