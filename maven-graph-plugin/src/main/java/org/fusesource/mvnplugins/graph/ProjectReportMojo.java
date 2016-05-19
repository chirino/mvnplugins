/**
 *  Copyright (C) 2009, Progress Software Corporation and/or its
 * subsidiaries or affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.mvnplugins.graph;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 *
 * @goal project-report
 * @requiresDependencyResolution compile|test|runtime
 */
public class ProjectReportMojo extends ProjectMojo implements MavenReport {


    /**
     * Output folder where the main page of the report will be generated. Note that this parameter is only relevant if
     * the goal is run directly from the command line or from the default lifecycle. If the goal is run indirectly as
     * part of a site generation, the output directory configured in the Maven Site Plugin will be used instead.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * @readonly
     * @parameter default-value="${reactorProjects}"
     * @since 1.0
     */
    private List<MavenProject> reactorProjects;


    public boolean canGenerateReport() {
        return true;
    }

    public String getOutputName() {
        return "dependency-graph";
    }

    public String getName(Locale locale) {
        return "Dependency Graph";
//        return getBundle( locale ).getString( "report.graph.name" );
    }

    public String getDescription(Locale locale) {
        return "Visual graph of the maven dependencies";
//        return getBundle( locale ).getString( "report.graph.description" );
    }

    public String getCategoryName() {
        return CATEGORY_PROJECT_INFORMATION;
    }

    public void setReportOutputDirectory(File file) {
        outputDirectory = file;
    }

    public File getReportOutputDirectory() {
        return outputDirectory;
    }

    public boolean isExternalReport() {
        return false;
    }

    public void generate(Sink sink, Locale locale) throws MavenReportException {
        try {
            getLog().info(project.getModules().toString() );
            execute();
            
            sink.figure();
            sink.figureGraphics(getOutputName()+".png");
            sink.figure_();
            
        } catch (MojoExecutionException e) {
            throw new MavenReportException("Could not generate graph.", e);
        }
    }

    @Override
    protected void collectProjects(ArrayList<MavenProject> projects) {
        super.collectProjects(projects);
        if ( project.getModules().size() > 1 && reactorProjects != null) {
            projects.addAll(reactorProjects);
        }
    }
    
    @Override
    public File getTarget() {
        return new File(outputDirectory, getOutputName()+".png");
    }

}
