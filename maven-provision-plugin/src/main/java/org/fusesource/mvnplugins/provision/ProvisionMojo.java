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
package org.fusesource.mvnplugins.provision;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.List;

/**
 * A Maven Mojo to install a provision in the local repo if its not already there and then
 * provision an artifact into an output directory
 *
 * @author <a href="http://macstrac.blogspot.com">James Strachan</a>
 * @goal provision
 * @execute phase="install"
 */
public class ProvisionMojo extends AbstractMojo {

    /**
     * The directory where the output files will be located.
     *
     * @parameter expression="${outputDirectory}
     * @required
     */
    private File outputDirectory;


    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;


    /**
     * @component
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * @component
     * @readonly
     */
    private ArtifactResolver artifactResolver;


    /**
     * @parameter expression="${localRepository}"
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Whether to run the "chmod" command on the remote site after the deploy.
     * Defaults to "true".
     *
     * @parameter expression="${maven.updatesite.chmod}" default-value="true"
     * @since 2.1
     */
    private boolean chmod;

    /**
     * The mode used by the "chmod" command. Only used if chmod = true.
     * Defaults to "g+w,a+rX".
     *
     * @parameter expression="${maven.updatesite.chmod.mode}" default-value="g+w,a+rX"
     * @since 2.1
     */
    private String chmodMode;

    /**
     * The Server ID used to deploy the site which should reference a &lt;server&gt; in your
     * ~/.m2/settings.xml file for username/pwd
     *
     * @parameter expression="${updatesite.remoteServerId}"
     */
    private String remoteServerId;

    /**
     * The Server Server URL to deploy the site to which uses the same URL format as the
     * distributionManagement / site / url expression in the pom.xml
     *
     * @parameter expression="${updatesite.remoteServerUrl}"
     */
    private String remoteServerUrl;

    /**
     * The directory used to put the update site in. Defaults to "update".
     * <p/>
     * If you use the htacess generation then this directory is used as part of the redirects
     *
     * @parameter default-value="update"
     */
    private String remoteDirectory;


    /**
     * The options used by the "chmod" command. Only used if chmod = true.
     * Defaults to "-Rf".
     *
     * @parameter expression="${maven.updatesite.chmod.options}" default-value="-Rf"
     * @since 2.1
     */
    private String chmodOptions;

    /**
     * The options used by the "mv" command to move the current update site out of the way
     * Defaults to "".
     *
     * @parameter expression="${maven.updatesite.mv.options}" default-value=""
     * @since 2.1
     */
    private String mvOptions;

    /**
     * The date format to use for old build directories
     *
     * @parameter expression="${maven.updatesite.oldBuild.dateFormat}" default-value="yyyy-MM-dd-HH-mm-ss-SSS"
     */
    private String oldBuildDateFormat = "yyyy-MM-dd-HH-mm-ss-SSS";


    /**
     * @component
     */
    private WagonManager wagonManager;

    /**
     * The current user system settings for use in Maven.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;


    public void execute() throws MojoExecutionException, MojoFailureException {
        if (outputDirectory.exists()) {
            if (outputDirectory.isFile()) {
                throw new MojoExecutionException("Output directory is a file: " + outputDirectory);
            }
        } else {
            outputDirectory.mkdirs();
            if (!outputDirectory.exists() || !outputDirectory.isDirectory()) {
                throw new MojoExecutionException("Failed to create output directory: " + outputDirectory);
            }
        }
        Artifact a = project.getArtifact();
        String groupId = a.getGroupId();
        String artifactId = a.getArtifactId();
        String version = a.getVersion();
        String packaging = a.getType();

        getLog().debug("Attempting to resolve: " + groupId + ":" + artifactId + ":" + version + ":" + packaging);

        Artifact toDownload = artifactFactory.createBuildArtifact(groupId, artifactId, version, packaging);

        List remoteArtifactRepositories = project.getRemoteArtifactRepositories();

        try {
            artifactResolver.resolve(toDownload, remoteArtifactRepositories, localRepository);

        } catch (AbstractArtifactResolutionException e) {
            throw new MojoExecutionException("Couldn't download artifact: " + e.getMessage(), e);
        }

        File file = toDownload.getFile();

        File destFile = new File(outputDirectory, file.getName());
        try {
            getLog().info("Copying "
                    + file.getName() + " to "
                    + destFile);
            FileUtils.copyFile(file, destFile);

        } catch (Exception e) {
            throw new MojoExecutionException("Error copying artifact from " + file + " to " + destFile, e);
        }
    }

}
