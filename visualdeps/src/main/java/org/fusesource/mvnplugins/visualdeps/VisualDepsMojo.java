/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mvnplugins.visualdeps;

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.DependencyNode;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * The org.fusesource.mvnplugins:visualdeps:visualdeps goal generates
 * a maven dependency graph using the 'dot' command line tool. 
 *
 * @goal visualdeps
 * @aggregator
 * @requiresDependencyResolution compile|test
 * @author chirino
 */
public class VisualDepsMojo extends AbstractMojo  {

    /**
     * @required
     * @readonly
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * @readonly
     * @parameter default-value="${reactorProjects}"
     */
    private List<MavenProject> reactorProjects;

    /**
     * @required
     * @readonly
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * @component
     */
    private ArtifactResolver artifactResolver;

    /**
     * @readonly
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * @readonly
     * @component
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * @readonly
     * @component
     */
    private ArtifactCollector artifactCollector;

    /**
     * @required
     * @readonly
     * @component
     */
    private DependencyTreeBuilder treeBuilder;

    /**
     * @parameter default-value="${basedir}/${project.artifactId}.png" expression="${visualdeps.target}"
     */
    private File target;

    /**
     * @parameter default-value="true" expression="${visualdeps.hideOmitted}"
     */
    private boolean hideOmitted;

    /**
     * @parameter default-value="false" expression="${visualdeps.hideOptional}"
     */
    private boolean hideOptional;

    /**
     * @parameter default-value="true" expression="${visualdeps.hidePoms}"
     */
    private boolean hidePoms;

    /**
     * @parameter expression="${visualdeps.hideScopes}"
     */
    private String hideScopes;

    /**
     * @parameter default-value="false" expression="${visualdeps.hideTransitive}"
     */
    private boolean hideTransitive;

    /**
     * @parameter default-value="${project.name} Depedency Graph" expression="${visualdeps.label}"
     */
    private String label;

    public void execute() throws MojoExecutionException {
        try {
            DependencyVisualizer visualizer = new DependencyVisualizer();
            visualizer.hideOptional = hideOptional;
            visualizer.hidePoms = hidePoms;
            visualizer.hideOmitted = hideOmitted;
            visualizer.label = label;
            visualizer.hideTransitive = hideTransitive;

            if( hideScopes !=null ) {
                for( String scope : hideScopes.split(",") ) {
                    visualizer.hideScopes.add(scope);
                }
            }

            // visualizer.hideScopes
            ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
            projects.add(project);
            if( reactorProjects!=null ) {
                projects.addAll(reactorProjects);
            }
            
            for (MavenProject p : projects) {
                ArtifactFilter filter=null;
                DependencyNode node = treeBuilder.buildDependencyTree(p, localRepository, artifactFactory, artifactMetadataSource, filter, artifactCollector);
                visualizer.add(node);
            }

            target.getParentFile().mkdirs();
            visualizer.export(target);
            getLog().info("Dependency graph exported to: "+target);
        } catch (DependencyTreeBuilderException e) {
            throw new MojoExecutionException("Could not build the depedency tree.", e);
        }
	}

}