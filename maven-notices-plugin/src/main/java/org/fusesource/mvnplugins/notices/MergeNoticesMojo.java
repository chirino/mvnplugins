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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

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
     * @parameter
     * @required
     */
    private File repository;

    /**
     * @parameter default-value="My Project"
     * @required
     */    
    private String projectName;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Model model = project.getOriginalModel();
            model.setPackaging("jar");
            model.setProfiles(null);
            model.setBuild(null);
            model.setArtifactId(model.getArtifactId() + "-dependencies");

            String projectVersion = project.getVersion();
            
            model.setDependencies(loadDependenciesFromRepo());

            Build build = new Build();
            build.addPlugin(createShadePlugin());
            model.setBuild(build);

            // save the new pom to disk
            String targetDir = project.getBasedir() + File.separator + "target";
            File f = new File(targetDir, "dependency-pom.xml");
            if (f.exists()) {
                f.delete();
            }
            ModelWriter writer = new DefaultModelWriter();
            writer.write(f, null, model);

            // run a build for the new pom.xml
            buildProject(f);

            String jarFile = targetDir + File.separator + "target" + File.separator + model.getArtifactId() + "-" + projectVersion + ".jar";

            extractNotice(targetDir, jarFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unchecked")
    private List<Dependency> loadDependenciesFromRepo() {
        System.out.println(repository);
        Iterator<File> jars = FileUtils.listFiles(repository, new String[] { "jar" }, true).iterator();
        List<Dependency> deps = new ArrayList<Dependency>();
        while (jars.hasNext()) {
            File jar = jars.next();
            String version = jar.getParentFile().getName();

            String artifactId = jar.getParentFile().getParentFile().getName();

            String groupIdSep = StringUtils.replace(jar.getParentFile().getParentFile().getParentFile().getPath(), repository.getPath(), "");
            String groupId = groupIdSep.replace(File.separatorChar, '.');
            groupId = groupId.startsWith(".") ? groupId.substring(1) : groupId;
            groupId = groupId.endsWith(".") ? groupId.substring(0, groupId.length() - 1) : groupId;

            Dependency dep = new Dependency();
            dep.setArtifactId(artifactId);
            dep.setGroupId(groupId);
            dep.setVersion(version);
            deps.add(dep);
        }
        return deps;
    }

    private void extractNotice(String destination, String jarFile) throws IOException, FileNotFoundException {
        try {
            BufferedOutputStream dest = null;
            FileInputStream fis = new FileInputStream(jarFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("META-INF/NOTICE".equals(entry.getName())) {
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

    private void buildProject(File f) throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(f);
        request.setBaseDirectory(f.getParentFile());

        request.setGoals(Collections.singletonList("package"));

        Invoker invoker = new DefaultInvoker();
        invoker.execute(request);
    }

    private Plugin createShadePlugin() {
        Plugin shade = new Plugin();
        shade.setArtifactId("maven-shade-plugin");
        shade.setVersion("1.4");
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.addGoal("shade");
        pluginExecution.setPhase("package");

        Xpp3Dom configuration = new Xpp3Dom("configuration");

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
        transformers.addChild(transformer);

        pluginExecution.setConfiguration(configuration);
        shade.addExecution(pluginExecution);
        return shade;
    }
}
