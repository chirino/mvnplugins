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
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
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