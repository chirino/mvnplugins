/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mvnplugins.graph;

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

import java.util.ArrayList;
import java.io.File;

/**
 * Generates a graph image of the dependencies of the project using the graphviz
 * tool 'dot'.  You must have the 'dot' executable installed and in your path
 * before using this goal.
 * <p/>
 *
 * @author chirino
 * @goal project
 * @requiresDependencyResolution compile|test|runtime
 */
public class ProjectMojo extends AbstractMojo {

    /**
     * @required
     * @readonly
     * @parameter expression="${project}"
     * @since 1.0
     */
    private MavenProject project;

    /**
     * @required
     * @readonly
     * @parameter expression="${localRepository}"
     * @since 1.0
     */
    private ArtifactRepository localRepository;

    /**
     * @required
     * @component
     * @since 1.0
     */
    private ArtifactResolver artifactResolver;

    /**
     * @required
     * @readonly
     * @component
     * @since 1.0
     */
    private ArtifactFactory artifactFactory;

    /**
     * @required
     * @readonly
     * @component
     * @since 1.0
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * @required
     * @readonly
     * @component
     */
    private ArtifactCollector artifactCollector;

    /**
     * @required
     * @readonly
     * @component
     * @since 1.0
     */
    private DependencyTreeBuilder treeBuilder;

    /**
     * The file the diagram will be written to.  Must use a file extension that the dot command supports or just the
     * '.dot' extension.
     * <br/>
     * @parameter default-value="${project.build.directory}/project-graph.png" expression="${graph.target}"
     */
    private File target;

    /**
     * If set to true, ommitted dependencies will not be drawn.  Dependencies are marked
     * as ommitted if it would result in a resolution conflict.
     * <br/>
     * @parameter default-value="true" expression="${hide-omitted}"
     */
    private boolean hideOmitted;

    /**
     * If set to true optional dependencies are not drawn.
     * <br/>
     * @parameter default-value="false" expression="${hide-optional}"
     */
    private boolean hideOptional;

    /**
     * If set to true pom dependencies are not drawn.
     * <br/>
     * @parameter default-value="true" expression="${hide-poms}"
     */
    private boolean hidePoms;

    /**
     * A comma seperated list of scopes.  Dependencies which
     * mach the specified scopes will not be drawn.
     * <br/>
     * For example: <code>runtime,test</code>
     * <br/>
     * @parameter expression="${hide-scope}"
     */
    private String hideScopes;

    /**
     * If set to true then depdencies not explicitly defined in the projects
     * pom will not be drawn.
     * <br/>
     * @parameter default-value="false" expression="${hide-transitive}"
     */
    private boolean hideTransitive;

    /**
     * The label for the graph.
     * <br/>
     * @parameter default-value="Depedency Graph for ${project.name}" expression="${graph.label}"
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

            if (hideScopes != null) {
                for (String scope : hideScopes.split(",")) {
                    visualizer.hideScopes.add(scope);
                }
            }

            ArrayList<MavenProject> projects = new ArrayList<MavenProject>();
            collectProjects(projects);

            for (MavenProject p : projects) {
                ArtifactFilter filter = null;
                DependencyNode node = treeBuilder.buildDependencyTree(p, localRepository, artifactFactory, artifactMetadataSource, filter, artifactCollector);
                visualizer.add(node);
            }

            getTarget().getParentFile().mkdirs();
            visualizer.export(getTarget());
            getLog().info("Dependency graph exported to: " + getTarget());
        } catch (DependencyTreeBuilderException e) {
            throw new MojoExecutionException("Could not build the depedency tree.", e);
        }
    }

    protected void collectProjects(ArrayList<MavenProject> projects) {
        projects.add(project);
    }

    public File getTarget() {
        return target;
    }

}