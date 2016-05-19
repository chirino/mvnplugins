/**
 * Copyright (C) 2011 Progress Software, Inc.
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
package org.fusesource.mvnplugins.notices;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.MavenInvocationException;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.fusesource.mvnplugins.notices.util.DependencyPom;

/**
 * A plugin for merging all legal notices for all jars in a repository.
 *
 * @goal merge-notices-in-repository
 * @phase process-resources
 */
public class MergeNoticesMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;
    
    /**
     * The local repository taken from Maven's runtime. Typically $HOME/.m2/repository.
     *
     * @parameter default-value="${localRepository}"
     * @readonly
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * The Maven Session Object
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;
    
    /**
     * @parameter
     * @required
     */
    private String repositories;

    /**
     * @parameter default-value="My Project"
     * @required
     */    
    private String projectName;
    
    /**
     * @parameter default-value=""
     */    
    private String preamble;

    /**
     * @parameter default-value="The Apache Software Foundation"
     */    
    private String organizationName;

    /**
     * @parameter default-value="http://www.apache.org/"
     */    
    private String organizationURL;

    /**
     * @parameter default-value="false"
     */    
    private boolean listDependencies;
    
    /**
     * @parameter default-value=""
     */    
    private String extraDependencies;   
    
    /**
     * @parameter default-value=""
     */    
    private String excludeDependencies;   
    
    /**
     * @parameter default-value="notice-supplements.xml"
     */    
    private String noticeSupplements;       

    /**
     * @parameter default-value=""
     */    
    private String defaultParent;  
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
        	long start = System.currentTimeMillis();
        	
            DependencyPom pom = new DependencyPom(project, localRepository, extraDependencies, defaultParent, excludeDependencies);
            pom.setSession(session);
            pom.addPlugin(createShadePlugin());
            if (listDependencies) {
                pom.addPlugin(createRemoteResourcesPlugin());
            }
            String targetDir = project.getBasedir() + File.separator + "target";

            pom.generatePom(repositories, targetDir);
            File jarFile = pom.buildPom();           

            if (jarFile.exists()) {
                extractFile(targetDir, jarFile, "META-INF/NOTICE");
                if (listDependencies) {
                    extractFile(targetDir, jarFile, "META-INF/DEPENDENCIES");
                }
                getLog().info("Notices generated in " + (System.currentTimeMillis() - start) + "ms.");
            } else {
                getLog().error(IOUtil.toString(new FileInputStream(targetDir + "/dependency-pom.xml.log")));
                throw new MojoExecutionException("Could not generate notices, please check target/dependency-pom.xml.log for details.");
            }            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }
    }

    private void extractFile(String destination, File jarFile, String fileToExtract) throws IOException, FileNotFoundException {
        try {
            BufferedOutputStream dest = null;
            FileInputStream fis = new FileInputStream(jarFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (fileToExtract.equals(entry.getName())) {
                    int count;
                    byte data[] = new byte[2048];
                    File metaInf = new File(destination + File.separator + "META-INF");
                    metaInf.mkdir();
                    FileOutputStream fos = new FileOutputStream(destination + File.separator + entry.getName());
                    dest = new BufferedOutputStream(fos, 2048);
                    while ((count = zis.read(data, 0, 2048)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                    break;
                }
            }                     
            zis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Plugin createShadePlugin() {
        Plugin shade = new Plugin();
        shade.setArtifactId("maven-shade-plugin");
        shade.setVersion("2.2");
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.addGoal("shade");
        pluginExecution.setPhase("package");

        Xpp3Dom configuration = new Xpp3Dom("configuration");

        Xpp3Dom artifactSet = new Xpp3Dom("artifactSet");
        configuration.addChild(artifactSet);
        Xpp3Dom artifactSetIncludes = new Xpp3Dom("includes");
        artifactSet.addChild(artifactSetIncludes);
        Xpp3Dom artifactSetInclude = new Xpp3Dom("include");
        artifactSetInclude.setValue("*:*:jar:*");
        artifactSetIncludes.addChild(artifactSetInclude);
        
        // filters element
        Xpp3Dom filters = new Xpp3Dom("filters");
        configuration.addChild(filters);
        Xpp3Dom filter = new Xpp3Dom("filter");
        filters.addChild(filter);
        Xpp3Dom artifact = new Xpp3Dom("artifact");
        artifact.setValue("*:*");
        filter.addChild(artifact);
        Xpp3Dom excludes = new Xpp3Dom("excludes");
        filter.addChild(excludes);
        Xpp3Dom exclude = new Xpp3Dom("exclude");
        exclude.setValue("org/**");
        excludes.addChild(exclude);
        Xpp3Dom includes = new Xpp3Dom("includes");
        filter.addChild(includes);
        Xpp3Dom include = new Xpp3Dom("include");
        include.setValue("META-INF/**");
        includes.addChild(include);

        // transformers element
        Xpp3Dom transformers = new Xpp3Dom("transformers");
        configuration.addChild(transformers);
        Xpp3Dom transformer = new Xpp3Dom("transformer");
        transformer.setAttribute("implementation", "org.apache.maven.plugins.shade.resource.ApacheNoticeResourceTransformer");

        Xpp3Dom projectNameElement = new Xpp3Dom("projectName");
        projectNameElement.setValue(projectName);
        transformer.addChild(projectNameElement);
        
        Xpp3Dom organizationNameElement = new Xpp3Dom("organizationName");
        organizationNameElement.setValue(organizationName);
        transformer.addChild(organizationNameElement);
        
        Xpp3Dom organizationURLElement = new Xpp3Dom("organizationURL");
        organizationURLElement.setValue(organizationURL);
        transformer.addChild(organizationURLElement);
        
        Xpp3Dom preamble1Element = new Xpp3Dom("preamble1");
        preamble1Element.setValue(preamble);
        transformer.addChild(preamble1Element);        
        
        transformers.addChild(transformer);

        pluginExecution.setConfiguration(configuration);
        shade.addExecution(pluginExecution);
        return shade;
    }
    
    private Plugin createRemoteResourcesPlugin() {
        Plugin plugin = new Plugin();
        
        plugin.setArtifactId("maven-remote-resources-plugin");
        plugin.setVersion("1.4");
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.addGoal("process");
        pluginExecution.setPhase("package");

        Xpp3Dom configuration = new Xpp3Dom("configuration");
        
        addNestedElement(configuration, "resourceBundles", "resourceBundle", "org.apache:apache-jar-resource-bundle:1.4");
        addNestedElement(configuration, "supplementalModels", "supplementalModel", noticeSupplements);
        addNestedElement(configuration, "properties", "projectName", projectName);
        
        // we already have all the dependencies in our pom so no need to grab transitives too
        addElement(configuration, "excludeTransitive", "true");
        
        pluginExecution.setConfiguration(configuration);
        plugin.addExecution(pluginExecution);
        return plugin;
    }

    private void addElement(Xpp3Dom parent, String name, String value) {
        Xpp3Dom artifactId = new Xpp3Dom(name);        
        artifactId.setValue(value);
        parent.addChild(artifactId);
    }
    
    private void addNestedElement(Xpp3Dom root, String parent, String child, String value) {
        Xpp3Dom parentNode = new Xpp3Dom(parent);        
        addElement(parentNode, child, value);
        root.addChild(parentNode);
    }
}
