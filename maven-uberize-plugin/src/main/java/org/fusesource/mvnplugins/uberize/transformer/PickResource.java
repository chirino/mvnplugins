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
import java.util.List;

import org.fusesource.mvnplugins.uberize.UberEntry;
import org.fusesource.mvnplugins.uberize.Transformer;
import org.fusesource.mvnplugins.uberize.Uberizer;

/**
 * A transformer that picks either the first or last resource
 * from all the available files.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class PickResource extends AbstractPathTransformer {

    public String type="first";

    protected UberEntry process(Uberizer uberizer, UberEntry entry, File target) throws IOException {
        final List<File> sources = entry.getSources();
        if( sources.isEmpty() ) {
            return entry;
        }
        if( "first".equals(type) ) {
            return new UberEntry(entry).addSource(sources.get(0));
        } else if( "last".equals(type) ) {
            return new UberEntry(entry).addSource(sources.get(sources.size()-1));
        } else {
            throw new IllegalArgumentException("Invalid configuration. Type must be set to 'first' or 'last'");
        }
    }


}