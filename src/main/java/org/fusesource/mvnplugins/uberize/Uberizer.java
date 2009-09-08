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

/**
 * @author Jason van Zyl
 * @author Hiram Chirino
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
}
