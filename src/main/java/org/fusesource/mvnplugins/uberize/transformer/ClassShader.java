/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fusesource.mvnplugins.uberize.transformer;

import org.fusesource.mvnplugins.uberize.relocation.Relocator;
import org.fusesource.mvnplugins.uberize.relocation.SimpleRelocator;
import org.fusesource.mvnplugins.uberize.relocation.PackageRelocation;
import org.fusesource.mvnplugins.uberize.Transformer;
import org.fusesource.mvnplugins.uberize.UberEntry;
import org.fusesource.mvnplugins.uberize.DefaultUberizer;
import org.fusesource.mvnplugins.uberize.Uberizer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.Map.Entry;

/**
 * @author Jason van Zyl
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ClassShader implements Transformer {
    
    public PackageRelocation[] relocations;
    public Paths resources;

    private List<Relocator> getRelocators()
    {
        List<Relocator> relocators = new ArrayList<Relocator>();
        if ( relocations == null )
        {
            return relocators;
        }

        for ( int i = 0; i < relocations.length; i++ )
        {
            PackageRelocation r = relocations[i];

            relocators.add( new SimpleRelocator( r.getPattern(), r.getShadedPattern(), r.getExcludes() ) );
        }

        return relocators;
    }


    public void process(Uberizer uberizer, File workDir, TreeMap<String, UberEntry> nodes) throws IOException {

        final List<Relocator> relocators = getRelocators();

        // Perhaps there is no work for us to do.
        if( relocators.isEmpty() ) {
            return;
        }


        HashMap<String, String> relocatedClasses = new HashMap<String, String>();
        RelocatorRemapper remapper = new RelocatorRemapper(relocators);
        for (UberEntry node : new ArrayList<UberEntry>(nodes.values())) {
            String path = node.getPath();
            if ( path.endsWith( ".class" ) )
            {

                // Need to take the .class off for remapping evaluation
                final String classPath = path.substring(0, path.indexOf('.'));
                String remappedPath = remapper.map(classPath) + ".class";
                byte[] modifiedClass;

                File file = uberizer.pickOneSource(nodes, node);
                InputStream is = new FileInputStream( file );
                try {
                    ClassReader cr = new ClassReader( is );
                    ClassWriter cw = new ClassWriter( cr, 0 );
                    ClassVisitor cv = new RemappingClassAdapter( cw, remapper );
                    cr.accept( cv, ClassReader.EXPAND_FRAMES );
                    modifiedClass = cw.toByteArray();
                } finally {
                    IOUtil.close( is );
                }


                String className = classPath.replace('/','.');
                String mappedClassName = mapClassName(relocators, className);
                if( mappedClassName == className ) {
                    relocatedClasses.put(className, mappedClassName);
                }

                // Write the file out
                is = new ByteArrayInputStream(modifiedClass);
                File classFile = DefaultUberizer.writeFile(workDir, remappedPath, is);

                // Modify the node tree.
                nodes.remove(path);
                UberEntry update = new UberEntry(remappedPath, node).addSource(classFile);
                nodes.put(update.getPath(), update);
            }
        }

        // Should we update resources with the class name changes?
        if( resources!=null && !relocatedClasses.isEmpty()) {
            for (UberEntry node : new ArrayList<UberEntry>(nodes.values())) {
                String path = node.getPath();
                if ( resources.matches(path) && !path.endsWith(".class")) {
                    File file = uberizer.pickOneSource(nodes, node);
                    String content = FileUtils.fileRead(file);
                    for (Entry<String, String> entry : relocatedClasses.entrySet()) {
                        content.replaceAll(Pattern.quote(entry.getKey()), Pattern.quote(entry.getValue()));
                    }
                    File udpateFile = DefaultUberizer.prepareFile(workDir, node.getPath());
                    FileUtils.fileWrite(udpateFile.getPath(), content);

                    // Modify the node tree.
                    UberEntry update = new UberEntry(node).addSource(udpateFile);
                    nodes.put(node.getPath(), update);
                }
            }
        }
    }

    public String mapClassName(List<Relocator> relocators, String name)
    {
        String value = name;
        for ( Iterator i = relocators.iterator(); i.hasNext(); )
        {
            Relocator r = (Relocator) i.next();
            if ( r.canRelocateClass( name ) )
            {
                value = r.relocateClass( name );
                break;
            }
        }
        return value;
    }

    class RelocatorRemapper extends Remapper
    {
        List<Relocator> relocators;

        public RelocatorRemapper( List<Relocator> relocators )
        {
            this.relocators = relocators;
        }

        public boolean hasRelocators()
        {
            return !relocators.isEmpty();
        }

        public Object mapValue( Object object )
        {
            if ( object instanceof String )
            {
                String name = (String) object;
                String value = name;
                for ( Iterator i = relocators.iterator(); i.hasNext(); )
                {
                    Relocator r = (Relocator) i.next();

                    if ( r.canRelocateClass( name ) )
                    {
                        value = r.relocateClass( name );
                        break;
                    }
                    else if ( r.canRelocatePath( name ) )
                    {
                        value = r.relocatePath( name );
                        break;
                    }

                    if ( name.length() > 0 && name.charAt( 0 ) == '[' )
                    {
                        int count = 0;
                        while ( name.length() > 0 && name.charAt(0) == '[' )
                        {
                            name = name.substring( 1 );
                            ++count;
                        }

                        if ( name.length() > 0
                             && name.charAt( 0 ) == 'L'
                             && name.charAt( name.length() - 1 ) == ';' )
                        {
                            name = name.substring( 1, name.length() - 1 );

                            if ( r.canRelocatePath( name ) )
                            {
                                value = 'L' + r.relocatePath( name ) + ';';
                                while ( count > 0 )
                                {
                                    value = '[' + value;
                                    --count;
                                }
                                break;
                            }

                            if ( r.canRelocateClass( name ) )
                            {
                                value = 'L' + r.relocateClass( name ) + ';';
                                while (count > 0)
                                {
                                    value = '[' + value;
                                    --count;
                                }
                                break;
                            }

                        }
                    }
                }
                return value;
            }
            return super.mapValue( object );
        }

        public String map( String name )
        {
            String value = name;
            for ( Iterator i = relocators.iterator(); i.hasNext(); )
            {
                Relocator r = (Relocator) i.next();

                if ( r.canRelocatePath( name ) )
                {
                    value = r.relocatePath( name );
                    break;
                }
            }
            return value;
        }
    }

}
