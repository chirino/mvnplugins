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
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * @author Jason van Zyl
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @plexus.component
 */
public class DefaultUberizer extends AbstractLogEnabled implements Uberizer {
    private final HashMap<String, String> classRelocations = new HashMap<String, String>();
    private final HashMap<File, File> extractionMapping = new HashMap<File, File>();
    private Transformer currentTransformer;


    public void uberize(File targetDir, Set sourceJars, File uberJar, List<Filter> filters, List<Transformer> transformers)
            throws IOException {
        classRelocations.clear();
        extractionMapping.clear();
        currentTransformer=null;

        targetDir = targetDir.getCanonicalFile();
        targetDir.mkdirs();
        FileUtils.cleanDirectory(targetDir);
        final String jarMappingTxt = new File(targetDir, "jar-mapping.txt").getPath();
        final String transformMappingTxt = new File(targetDir, "transformation-mapping.txt").getPath();

        // A 'jar entry path' => UberEntry map, points to all the data
        // that is in the source sourceJars.
        TreeMap<String, UberEntry> tree = new TreeMap<String, UberEntry>();


        // Extract each jar to a seperate directory and build up the tree
        // tree to point to all the extracted files.
        getLogger().info("Extracting jars...");
        for (Iterator i = sourceJars.iterator(); i.hasNext();) {
            File jar = (File) i.next();

            int counter = 1;
            String id = jar.getName();
            File workDir = new File(targetDir, id);
            while (workDir.exists()) {
                id = jar.getName() + "." + counter++;
                workDir = new File(targetDir, id);
            }
            extractionMapping.put(workDir, jar);
            FileUtils.fileAppend(jarMappingTxt, id + "=" + jar.getPath() + "\n");

            List jarFilters = getFilters(jar, filters);
            JarFile jarFile = new JarFile(jar);
            try {
                for (Enumeration j = jarFile.entries(); j.hasMoreElements();) {
                    JarEntry entry = (JarEntry) j.nextElement();
                    String name = entry.getName();

                    // Skip over stuff we are filtering out
                    if (entry.isDirectory() || isFiltered(jarFilters, name)) {
                        continue;
                    }

                    // Extract the file..
                    InputStream is = jarFile.getInputStream(entry);
                    File extracted = writeFile(workDir, name, is);
                    getNode(tree, name).getSources().add(extracted);
                }
            } finally {
                jarFile.close();
            }
        }

        // The transformers can now inspect the tree modify it's organization
        // to aid in trouble shooting, the transformer should not modify the extracted
        // files.  It should instead generate new files in the provided work directory.
        int transformerCounter = 0;
        for (Transformer transformer : transformers) {
            getLogger().info("Applying transformer: "+transformer.getClass().getName());
            currentTransformer = transformer;
            final String id = "transformer-" + (transformerCounter++);
            File xformWorkDir = new File(targetDir, id);
            FileUtils.fileAppend(transformMappingTxt, id + "=" + transformer.getClass().getName() + "\n");
            transformer.process(this, xformWorkDir, tree);
        }

        // Cleanup any remaining overlapping entries. First source wins.
        boolean ok = true;
        for (UberEntry entry : new ArrayList<UberEntry>(tree.values())) {
            if( entry.getSources().isEmpty() ) {
                // We can dump empty entries..
                tree.remove(entry.getPath());
            } else {
                pickOneSource(tree, entry);
            }
        }

        // Generate the uber jar using the transformed tree
        getLogger().info("Uber jarring...");
        uberJar.getParentFile().mkdirs();
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(uberJar));
        HashSet<String> uberDirectories = new HashSet<String>();
        try {
            for (Entry<String, UberEntry> entry : tree.entrySet()) {
                final String path = entry.getKey();

                // Make sure the parent dirs are created in the jar
                ArrayList<String> dirs = new ArrayList<String>();
                getParentDirs(path, dirs);
                for (String dir : dirs) {
                    if (uberDirectories.add(dir)) {
                        jos.putNextEntry(new JarEntry(dir));
                    }
                }

                // Write the jar enry from the node's file
                jos.putNextEntry(new JarEntry(path));
                File file = entry.getValue().getSources().get(0);
                FileInputStream is = new FileInputStream(file);
                try {
                    IOUtil.copy(is, jos);
                } finally {
                    IOUtil.close(is);
                }

            }
        } finally {
            IOUtil.close(jos);
        }

    }

    public File pickOneSource(TreeMap<String, UberEntry> tree, UberEntry entry) throws IOException {
        if( entry.getSources().isEmpty() ) {
            return null;
        }

        if (entry.getSources().size() > 1) {
            LinkedList<File> ignores = new LinkedList<File>(entry.getSources());
            File pick = ignores.removeFirst();

            if( isFileTypeThatCanIgnoreDuplicates( entry.getPath() ) ) {
                // We can remove it from the list if it's a duplicate of the first...
                // If all the sources are duplicates, then we avoid logging a warning to the user.
                for (Iterator<File> i = ignores.iterator(); i.hasNext();) {
                    File file =  i.next();
                    if( FileUtils.contentEquals(pick, file) ) {
                        i.remove();
                    }
                }
            }

            if( !ignores.isEmpty() ) {
                String msgPrefix = "Overlapping sources for jar entry: ";
                if( currentTransformer!=null ) {
                    msgPrefix = currentTransformer.getClass().getSimpleName()+": "+msgPrefix;
                }
                getLogger().warn("  "+msgPrefix+entry.getPath());
                getLogger().warn("    Picking source: " + originalJar(pick));
                for (File dup : ignores) {
                    getLogger().warn("    Ignoring source: " + originalJar(dup));
                }
            }

            entry = new UberEntry(entry).addSource(pick);
            tree.put(entry.getPath(), entry);
        }
        return entry.getSources().get(0);
    }

    /**
     * Try to find the original jar for a source file.
     * If it's a tranformed source.. then this may not be possible.
     * 
     * @param source
     * @return
     * @throws IOException
     */
    private File originalJar(File source) throws IOException {
        String sourcePath = source.getCanonicalPath();
        for (File file : extractionMapping.keySet()) {
            if( sourcePath.startsWith(file.getCanonicalPath()+File.separator) ) {
                return extractionMapping.get(file);
            }
        }
        return source;
    }

    private boolean isFileTypeThatCanIgnoreDuplicates(String path) {
        return path.endsWith(".class")
                | path.endsWith(".jpg")
                | path.endsWith(".gif")
                | path.endsWith(".png")
                | path.endsWith(".tiff")
                | path.endsWith(".jar")
                | path.endsWith(".zip")
                | path.endsWith(".tgz")
                | path.endsWith(".tar")
                | path.endsWith(".so")
                | path.endsWith(".dll")
                | path.endsWith(".dylib")
                | path.endsWith(".lib")
                | path.endsWith(".exe")
                | path.endsWith(".sh")
                | path.endsWith(".bat")
                ;
    }

    public HashMap<String, String>  getClassRelocations() {
        return this.classRelocations;
    }

    static void getParentDirs(String path, ArrayList<String> dirs) {
        if (path.length() < 2) {
            return;
        }
        int p = path.lastIndexOf("/", path.length() - 2);
        if (p > 0) {
            String dir = path.substring(0, p + 1);
            dirs.add(dir);
            getParentDirs(dir, dirs);
        }
    }

    private UberEntry getNode(TreeMap<String, UberEntry> nodes, String path) {
        UberEntry node = nodes.get(path);
        if (node == null) {
            node = new UberEntry(path);
            nodes.put(path, node);
        }
        return node;
    }

    static public File writeFile(File basedir, String path, InputStream is) throws IOException {
        File file = prepareFile(basedir, path);
        try {
            FileOutputStream os = new FileOutputStream(file);
            try {
                IOUtil.copy(is, os);
            } finally {
                IOUtil.close(os);
            }
        } finally {
            IOUtil.close(is);
        }
        return file;
    }

    public static File prepareFile(File basedir, String path) throws IOException {
        File file = FileUtils.resolveFile(basedir, path);
        // Lets do a sanity check that the file resolved to be a sub dir.
        if (!file.getCanonicalPath().startsWith(basedir.getCanonicalPath())) {
            throw new IOException("Bad output file name resolution: " + path);
        }
        file.getParentFile().mkdirs();
        return file;
    }

    private List<Filter> getFilters(File jar, List<Filter> filters) {
        List<Filter> list = new ArrayList<Filter>();

        for (int i = 0; i < filters.size(); i++) {
            Filter filter = (Filter) filters.get(i);

            if (filter.canFilter(jar)) {
                list.add(filter);
            }

        }

        return list;
    }

    private boolean isFiltered(List filters, String name) {
        for (int i = 0; i < filters.size(); i++) {
            Filter filter = (Filter) filters.get(i);

            if (filter.isFiltered(name)) {
                return true;
            }
        }

        return false;
    }

}
