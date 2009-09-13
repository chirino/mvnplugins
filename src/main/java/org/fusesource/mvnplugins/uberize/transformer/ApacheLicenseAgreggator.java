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

import org.fusesource.mvnplugins.uberize.Transformer;
import org.fusesource.mvnplugins.uberize.UberEntry;
import org.fusesource.mvnplugins.uberize.Uberizer;

import java.io.IOException;
import java.io.File;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Map.Entry;

/**
 * Prevents duplicate copies of the license
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ApacheLicenseAgreggator implements Transformer {

    private static final String LICENSE_PATH = "META-INF/LICENSE";
    private static final String LICENSE_TXT_PATH = "META-INF/LICENSE.txt";


    public void process(Uberizer uberizer, File workDir, TreeMap<String, UberEntry> uberEntries) throws IOException {
        ArrayList<UberEntry> matches = new ArrayList<UberEntry>();
        for (Entry<String, UberEntry> entry : uberEntries.entrySet()) {
            String resource = entry.getKey();
            if (matches(resource)) {
                for (File file : entry.getValue().getSources()) {
                    //TODO: implement
                }
                matches.add(entry.getValue());
            }
        }
    }


    static boolean matches(String resource) {
        return LICENSE_PATH.equalsIgnoreCase(resource)
                || LICENSE_TXT_PATH.equalsIgnoreCase(resource);
    }

}
