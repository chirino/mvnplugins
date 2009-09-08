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

import org.fusesource.mvnplugins.uberize.DefaultUberizer;
import org.fusesource.mvnplugins.uberize.Transformer;
import org.fusesource.mvnplugins.uberize.UberEntry;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;

public class ApacheNoticeAgreggator implements Transformer {

    private static final String NOTICE_PATH = "META-INF/NOTICE";
    private static final String NOTICE_TXT_PATH = "META-INF/NOTICE.txt";

    /**
     * The file encoding of the <code>NOTICE</code> file.
     */
    String encoding;


    Map organizationEntries = new LinkedHashMap();
    String projectName;

    // If notices is not set.. a default one will be generated using the following
    // properties...
    Set<String> notices;

    //defaults overridable via config in pom
    String organizationName = "The Apache Software Foundation";
    String organizationURL = "http://www.apache.org/";
    String inceptionYear = "2006";
    String copyright;


    public void process(File workDir, TreeMap<String, UberEntry> uberEntries) throws IOException {

        Set<String> noticeSet = new LinkedHashSet<String>();

        // Do some setup...
        if (notices==null) {

            String year = new SimpleDateFormat("yyyy").format(new Date());
            if (!inceptionYear.equals(year)) {
                year = inceptionYear + "-" + year;
            }

            noticeSet.add(
                "// ------------------------------------------------------------------\n"
              + "// NOTICE file corresponding to the section 4d of The Apache License,\n"
              + "// Version 2.0, in this case for " + projectName + "\n" +
                "// ------------------------------------------------------------------\n");


            //fake second entry, we'll look for a real one later
            noticeSet.add(projectName + "\n" +
                    "Copyright " + year + " " + organizationName + "\n");
            noticeSet.add("This product includes software developed at" +
                    "\n" + organizationName + " (" + organizationURL + ").\n");
        } else {
            noticeSet.addAll(notices);
        }

        // Add all the license files..
        ArrayList<UberEntry> matches = new ArrayList<UberEntry>();
        for (UberEntry entry : new ArrayList<UberEntry>(uberEntries.values())) {
            if (matches(entry.getPath())) {
                for (File file : entry.getSources()) {
                    processFile(file, noticeSet);
                }
                matches.add(entry);
            }
        }

        // Create the new merged license file.
        File targetFile = DefaultUberizer.prepareFile(workDir, NOTICE_PATH);
        FileOutputStream jos = new FileOutputStream(targetFile);

        Writer pow;
        if (StringUtils.isNotEmpty(encoding)) {
            pow = new OutputStreamWriter(jos, encoding);
        } else {
            pow = new OutputStreamWriter(jos);
        }
        PrintWriter writer = new PrintWriter(pow);

        int count = 0;
        for (Iterator itr = noticeSet.iterator(); itr.hasNext();) {
            ++count;
            String line = (String) itr.next();
            if (line.equals(copyright) && count != 2) {
                continue;
            }

            if (count == 2 && copyright != null) {
                writer.print(copyright);
                writer.print('\n');
            } else {
                writer.print(line);
                writer.print('\n');
            }
            if (count == 3) {
                //do org stuff
                for (Iterator oit = organizationEntries.entrySet().iterator(); oit.hasNext();) {
                    Map.Entry entry = (Map.Entry) oit.next();
                    writer.print(entry.getKey().toString());
                    writer.print('\n');
                    Set entrySet = (Set) entry.getValue();
                    for (Iterator eit = entrySet.iterator(); eit.hasNext();) {
                        writer.print(eit.next().toString());
                    }
                    writer.print('\n');
                }
            }
        }
        writer.flush();

        // Update the entry tree
        UberEntry modEntry = new UberEntry(NOTICE_PATH, matches);
        modEntry.getSources().add(targetFile);
        uberEntries.put(modEntry.getPath(), modEntry);
    }

    static boolean matches(String resource) {
        return NOTICE_PATH.equalsIgnoreCase(resource)
                || NOTICE_TXT_PATH.equalsIgnoreCase(resource);
    }

    private void processFile(File file, Set<String> entries) throws IOException {
        BufferedReader reader;
        if (StringUtils.isNotEmpty(encoding)) {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
        } else {
            reader = new BufferedReader(new FileReader(file));
        }

        String line = reader.readLine();
        StringBuffer sb = new StringBuffer();
        Set currentOrg = null;
        int lineCount = 0;
        while (line != null) {
            String trimedLine = line.trim();

            if (!trimedLine.startsWith("//")) {
                if (trimedLine.length() > 0) {
                    if (trimedLine.startsWith("- ")) {
                        //transformer-bundle 1.3 mode
                        if (lineCount == 1
                                && sb.toString().indexOf("This product includes/uses software(s) developed by") != -1) {
                            currentOrg = (Set) organizationEntries.get(sb.toString().trim());
                            if (currentOrg == null) {
                                currentOrg = new TreeSet();
                                organizationEntries.put(sb.toString().trim(), currentOrg);
                            }
                            sb = new StringBuffer();
                        } else if (sb.length() > 0 && currentOrg != null) {
                            currentOrg.add(sb.toString());
                            sb = new StringBuffer();
                        }

                    }
                    sb.append(line).append("\n");
                    lineCount++;
                } else {
                    String ent = sb.toString();
                    if (ent.startsWith(projectName)
                            && ent.indexOf("Copyright ") != -1) {
                        copyright = ent;
                    }
                    if (currentOrg == null) {
                        entries.add(ent);
                    } else {
                        currentOrg.add(ent);
                    }
                    sb = new StringBuffer();
                    lineCount = 0;
                    currentOrg = null;
                }
            }

            line = reader.readLine();
        }
        if (sb.length() > 0) {
            if (currentOrg == null) {
                entries.add(sb.toString());
            } else {
                currentOrg.add(sb.toString());
            }
        }
    }

}
