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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import org.codehaus.plexus.util.IOUtil;
import org.fusesource.mvnplugins.uberize.UberEntry;
import org.fusesource.mvnplugins.uberize.Uberizer;

/**
 * Resources transformer that appends entries in META-INF/services resources into
 * a single resource. For example, if there are several META-INF/services/org.apache.maven.project.ProjectBuilder
 * resources spread across many JARs the individual entries will all be concatenated into a single
 * META-INF/services/org.apache.maven.project.ProjectBuilder resource packaged into the resultant JAR produced
 * by the shading process.
 *
 * @author jvanzyl
 */
public class ServicesAppender extends AbstractTransformer {
    private static final String SERVICES_PATH = "META-INF/services";

    private ByteArrayOutputStream data;

    protected boolean matches(String resource) {
        return resource.startsWith(SERVICES_PATH);
    }

    protected UberEntry process(Uberizer uberizer, UberEntry entry, File target) throws IOException {
        OutputStream out = new FileOutputStream(target);
        try {
            for (File source : entry.getSources()) {
                InputStream in = new FileInputStream(source);
                try {
                    IOUtil.copy( in, out );
                } finally {
                    IOUtil.close(in);
                }
            }
        } finally {
            IOUtil.close(out);
        }
        return new UberEntry(entry).addSource(target);
    }

}
