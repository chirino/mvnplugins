package org.fusesource.mvnplugins.notices.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

public class DependencyPom {
    
    private MavenProject project;
    private Model model;
    private String projectVersion;
    private File file;

    public DependencyPom(MavenProject project) {
        this.project = project;
        
        model = project.getOriginalModel();
        model.setPackaging("jar");
        model.setProfiles(null);
        model.setBuild(null);
        model.setArtifactId(model.getArtifactId() + "-dependencies");
        projectVersion = project.getVersion();

        Build build = new Build();
        model.setBuild(build);
    }
    
    public void addPlugin(Plugin plugin) {
        model.getBuild().addPlugin(plugin);        
    }

    public void generatePom(String repositories, String targetDir) throws IOException {
        model.setDependencies(loadDependenciesFromRepos(repositories));
        
        file = new File(targetDir, "dependency-pom.xml");
        if (file.exists()) {
            file.delete();
        }
        ModelWriter writer = new DefaultModelWriter();
        writer.write(file, null, model);
    }
    
    public File buildPom() throws MavenInvocationException {
        if (file != null) {
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(file);
            request.setBaseDirectory(file.getParentFile());
        
            request.setGoals(Collections.singletonList("package"));
        
            Invoker invoker = new DefaultInvoker();
            invoker.execute(request);
            
            return new File(project.getBasedir() + File.separator + "target" 
                    + File.separator + "target" + File.separator + model.getArtifactId() + "-" + projectVersion + ".jar");           
        }
        return null;
    }
    
    private List<Dependency> loadDependenciesFromRepos(String repositories) {
        if (repositories.contains(",")) {            
            List<Dependency> deps = new ArrayList<Dependency>();
            for (String repo : repositories.split(",")) {
                deps.addAll(loadDependenciesFromRepo(new File(repo)));
            }
            return deps;
        } else {
            return loadDependenciesFromRepo(new File(repositories));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Dependency> loadDependenciesFromRepo(File repo) {
        Iterator<File> jars = FileUtils.listFiles(repo, new String[] { "jar" }, true).iterator();
        List<Dependency> deps = new ArrayList<Dependency>();
        while (jars.hasNext()) {
            File jar = jars.next();
            Dependency dependencyFromJar = dependencyFromJar(jar, repo);
            if (dependencyFromJar != null) {
                deps.add(dependencyFromJar);
            }
        }
        return deps;
    }    
    
    private Model getPom(File jar) {
        try {
            ZipFile zip = new ZipFile(jar);
            FileInputStream fis = new FileInputStream(jar);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().startsWith("META-INF") && entry.getName().endsWith("pom.xml")) {
                    ModelReader reader = new DefaultModelReader();
                    InputStream inputStream = zip.getInputStream(entry);
                    Model pom = reader.read(inputStream, null);
                    inputStream.close();
                    return pom;
                }
            }
            zis.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Properties getPomProperties(File jar) {
        try {
            ZipFile zip = new ZipFile(jar);
            FileInputStream fis = new FileInputStream(jar);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().startsWith("META-INF") && entry.getName().endsWith("pom.properties")) {
                    Properties props = new Properties();
                    InputStream inputStream = zip.getInputStream(entry);
                    props.load(inputStream);
                    inputStream.close();
                    return props;
                }
            }
            zis.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Dependency dependencyFromJar(File jar, File repo) {
        Model pom = getPom(jar);
        Properties props = getPomProperties(jar);
        Dependency dep = new Dependency();
        if (props != null) {            
            dep.setArtifactId(props.getProperty("artifactId"));
            dep.setGroupId(props.getProperty("groupId"));
            dep.setVersion(props.getProperty("version"));
        } else if (pom != null) {            
            dep.setArtifactId(pom.getArtifactId());
            dep.setGroupId(pom.getGroupId());
            dep.setVersion(pom.getVersion());
        } else { 
            return null;
        }       
        return dep;
    }
}
