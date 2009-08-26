/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mvnplugins.graph;

import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

/**
 * Generates a graph image of the aggregate dependencies all the projects in
 * current reactor (aka multi project) build using the graphviz
 * tool 'dot'.  You must have the 'dot' executable installed and in your path
 * before using this goal.
 * <p/>
 *
 * @author chirino
 * @goal reactor
 * @aggregator
 * @requiresDependencyResolution compile|test|runtime
 */
public class ReactorMojo extends ProjectMojo {

    /**
     * @readonly
     * @parameter default-value="${reactorProjects}"
     * @since 1.0
     */
    private List<MavenProject> reactorProjects;


    /**
     * The file the diagram will be written to.  Must use a file extension that the dot command supports or just the
     * '.dot' extension.
     * <br/>
     * @parameter default-value="${project.build.directory}/reactor-graph.png" expression="${graph.target}"
     */
    private File target;

    @Override
    protected void collectProjects(ArrayList<MavenProject> projects) {
        super.collectProjects(projects);
        if (reactorProjects != null) {
            projects.addAll(reactorProjects);
        }
    }

    @Override
    public File getTarget() {
        return target;
    }
}