/**
 * Copyright (C) 2009 Progress Software, Inc.
 * http://fusesource.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.mvnplugins.fab;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;

/**
 * Generates a fabric module descriptor and attaches it to the project
 * so that it gets deployed to a maven repo.
 * <p/>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @phase verify
 * @goal generate
 */
public class FabMojo extends AbstractMojo {

    public static final String FAB_MODULE_ID = "Id";
    public static final String FAB_MODULE_NAME = "Name";
    public static final String FAB_MODULE_DESCRIPTION = "Description";
    public static final String FAB_MODULE_SHA1 = "SHA1";

    /**
     * @required
     * @readonly
     * @parameter expression="${project}"
     * @since 1.0
     */
    protected MavenProject project;

    /**
     * @component
     * @required
     * @readonly
     */
    private MavenProjectHelper projectHelper;

    /**
     * The file name of the .
     * <br/>
     * @parameter default-value="${project.build.directory}/${project.artifactId}-${project.version}.${project.packaging}.fmd"
     */
    protected File target;

    /**
     * The classifier to use.
     * <br/>
     * @parameter
     */
    protected String classifier;

    /**
     * If set to false, then it disables the plugin.
     * <br/>
     * @parameter default-value="true"
     */
    protected boolean enabled;

    /**
     * Descriptor settings.
     * @parameter
     */
    protected Map<String, String> descriptor;

    public void execute() throws MojoExecutionException {
        if( !enabled )
            return;

        try {
            Properties p = new Properties();
            if( descriptor!=null ) {
                for (Map.Entry<String, String> entry: descriptor.entrySet()){
                    if( entry.getValue()!=null && entry.getValue().trim().length()!=0 ) {
                        p.put(entry.getKey(), entry.getValue().trim() );
                    }
                }
            }

            if( !p.containsKey(FAB_MODULE_ID) ) {
                String id = project.getGroupId()+":"+project.getArtifact()+":"+project.getVersion()+":"+project.getArtifact().getType()+
                        (classifier==null ? "" : ":"+classifier);
                p.setProperty(FAB_MODULE_ID, id);
            }
            if( !p.containsKey(FAB_MODULE_NAME) ) {
                p.setProperty(FAB_MODULE_NAME, project.getArtifactId());
            }
            if( !p.containsKey(FAB_MODULE_DESCRIPTION) ) {
                p.setProperty(FAB_MODULE_DESCRIPTION, project.getDescription());
            }

            if( !project.getVersion().contains("SNAPSHOT") && project.getArtifact().getFile().exists() ) {
                p.setProperty(FAB_MODULE_SHA1, checksum("SHA1", project.getArtifact().getFile()));
            }

            FileOutputStream os = new FileOutputStream(target);
            try {
                p.store(os, null);
            } finally {
                os.close();
            }

            getLog().info("Generated: "+target);

            projectHelper.attachArtifact( project, project.getArtifact().getType()+".fmd", classifier, target );

        } catch (Exception e) {
            throw new MojoExecutionException(String.format("Could not store the fabric module descriptor '%s'", target), e);
        }
    }

    protected String checksum(String algorithm, File file) throws MojoExecutionException {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            FileInputStream is=null;
            try {
                is = new FileInputStream(file);
                byte buffer[] = new byte[1024*4];
                int c;
                while( (c=is.read(buffer)) >= 0 ) {
                    md.update(buffer,0, c);
                }
                byte[] digest = md.digest();

                return toString(digest);

            } catch (IOException e) {
                throw new MojoExecutionException("Could read file: "+file);
            } finally {
                try {
                    is.close();
                } catch (Throwable e) {
                }
            }

        } catch (NoSuchAlgorithmException e) {
            throw new MojoExecutionException("Invalid checksum algorithm: "+algorithm, e);
        }
    }

    static String toString(byte[] digest) {
        StringBuilder rc = new StringBuilder(digest.length*2);
        for (int i = 0; i < digest.length; i++) {
            rc.append( hexTable[ ((digest[i]>>4) & 0x0F) ] ) ;
            rc.append( hexTable[ (digest[i] & 0x0F) ] ) ;
        }
        return rc.toString();
    }
    static char hexTable[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

}