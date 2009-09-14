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

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

import org.fusesource.mvnplugins.uberize.UberEntry;
import org.fusesource.mvnplugins.uberize.Transformer;
import org.fusesource.mvnplugins.uberize.Uberizer;

/**
 * A resource processor that allows the removal of an arbitrary
 * jar entries from the uber jar.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class RemoveResource extends AbstractPathTransformer {

    protected UberEntry process(Uberizer uberizer, UberEntry entry, File target) throws IOException {
        // Returning null signals that we want the original entry removed.
        return null;
    }

}