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

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import org.codehaus.plexus.util.IOUtil;
import org.fusesource.mvnplugins.uberize.UberEntry;

/**
 * A resource processor that appends content for a resource, separated by
 * an end of line sequence.
 */
public class TextAgreggator extends AbstractPathTransformer
{
    public String eol = "\n";

    protected void process(UberEntry entry, File target) throws IOException {
        byte eolBytes[] = eol.getBytes("UTF-8");
        OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
        int counter=0;
        try {
            for (File source : entry.getSources()) {
                InputStream in = new BufferedInputStream(new FileInputStream(source));
                try {
                    boolean endsWithNewline=true;
                    int c;
                    while( (c=in.read())>=0 ) {
                        if( c=='\r' ) {
                            continue;
                        }
                        if( c=='\n' ) {
                            endsWithNewline = true;
                            out.write(eolBytes);
                        } else {
                            endsWithNewline = false;
                            out.write(c);
                        }
                    }
                    if( !endsWithNewline ) {
                        out.write(eolBytes);
                    }
                } finally {
                    IOUtil.close(in);
                }
            }
        } finally {
            IOUtil.close(out);
        }
    }

}
