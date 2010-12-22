package org.fusesource.mvnplugins.updatesite;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.CommandExecutor;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.observers.Debug;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Deploys an Eclipse update site using <code>scp</code> or <code>file</code>
 * protocol to the site URL specified in the
 * <code>remoteServerId</code> and <code>remoteServerUrl</code> values in the
 * <code>&lt;configuration&gt;</code> of this plugin.
 * <p>
 * For <code>scp</code> protocol, the website files are packaged into zip archive,
 * then the archive is transfered to the remote host, next it is un-archived.
 * This method of deployment should normally be much faster
 * than making a file by file copy.  For <code>file</code> protocol, the files are copied
 * directly to the destination directory.
 * </p>
 *
 * @author <a href="mailto:michal@org.codehaus.org">Michal Maczka</a>
 * @phase("deploy")
 * @goal deploy
 */
public class UpdateSiteDeployMojo
        extends AbstractMojo implements Contextualizable {
    /**
     * Directory containing the generated site.
     *
     * @parameter alias="outputDirectory" expression="${project.build.directory}/site"
     * @required
     */
    private File inputDirectory;

    /**
     * Name of the generated .htaccess file to use
     *
     * @parameter alias="outputDirectory" expression="${project.build.directory}/updateSiteHtaccess"
     * @required
     */
    private String htaccessFileName;

    /**
     * Whether to generate a new update site for each build using a date/time postfix.
     * Defaults to "true".
     *
     * @parameter expression="${maven.updatesite.timestampDirectory}" default-value="true"
     */
    private boolean timestampDirectory;

    /**
     * Whether to generate a <code>.htaccess</code> file if using timestampDirectory mode
     *
     * @parameter expression="${maven.updatesite.generateHtaccess}" default-value="true"
     */
    private boolean generateHtaccess;

    /**
     * The name of the remote <code>.htaccess</code> file if using timestampDirectory mode.
     *
     * Defaults to ".htaccess".
     *
     * If your webdav provider won't let you upload files called ".htaccess" then you could
     * configure this property to be something like "tmp.htacces" then you could later on rename the file
     * using a cron job or something.
     *
     * @parameter expression="${maven.updatesite.remotehtAccessFile}" default-value=".htaccess"
     */
    private String remotehtAccessFile;

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
     *
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
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

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

    private PlexusContainer container;

    /**
     * {@inheritDoc}
     */
    public void execute()
            throws MojoExecutionException {
        if (!inputDirectory.exists()) {
            throw new MojoExecutionException("The site does not exist, please run site:site first");
        }
        String url = remoteServerUrl;
        String id = remoteServerId;

        if (id == null) {
            throw new MojoExecutionException("The remoteServerId is missing in the plugin configuration.");
        }
        if (url == null) {
            throw new MojoExecutionException("The remoteServerUrl is missing in the plugin configuration.");
        }
        getLog().debug("The site will be deployed to '" + url + "' with id '" + id + "'");

        Repository repository = new Repository(id, url);

        // TODO: work on moving this into the deployer like the other deploy methods

        Wagon wagon;

        try {
            wagon = wagonManager.getWagon(repository);
            configureWagon(wagon, repository.getId(), settings, container, getLog());
        } catch (UnsupportedProtocolException e) {
            throw new MojoExecutionException("Unsupported protocol: '" + repository.getProtocol() + "'", e);
        } catch (WagonConfigurationException e) {
            throw new MojoExecutionException("Unable to configure Wagon: '" + repository.getProtocol() + "'", e);
        }

        if (!wagon.supportsDirectoryCopy()) {
            throw new MojoExecutionException(
                    "Wagon protocol '" + repository.getProtocol() + "' doesn't support directory copying");
        }

        try {
            Debug debug = new Debug();

            wagon.addSessionListener(debug);

            wagon.addTransferListener(debug);

            ProxyInfo proxyInfo = getProxyInfo(repository, wagonManager);
            if (proxyInfo != null) {
                wagon.connect(repository, wagonManager.getAuthenticationInfo(id), proxyInfo);
            } else {
                wagon.connect(repository, wagonManager.getAuthenticationInfo(id));
            }

            SimpleDateFormat format = new SimpleDateFormat(oldBuildDateFormat);
            String postfix = "-" + format.format(new Date());

            if (wagon instanceof CommandExecutor) {
                CommandExecutor exec = (CommandExecutor) wagon;
                String repositoryBasedir = repository.getBasedir();

                // lets move the old directory first before we push...
                String newDir = repositoryBasedir + postfix;

                getLog().info("Moving the current update site to: " + newDir);
                exec.executeCommand("mv " + mvOptions + " " + repositoryBasedir + " " + newDir);

                wagon.putDirectory(inputDirectory, remoteDirectory);

                if (chmod) {
                    exec.executeCommand("chmod " + chmodOptions + " " + chmodMode + " " + repositoryBasedir);
                }
            } else {
                if (timestampDirectory) {
                    String updateSiteDirectory = remoteDirectory + postfix;

                    if (generateHtaccess) {
                        PrintWriter out = new PrintWriter(new FileWriter(htaccessFileName));
                        out.println("RewriteEngine on");
                        out.println();

                        /*
                        String[] paths = remoteDirectory.split("/");
                        int idx = paths.length - 1;
                        String dirName = paths[idx];
                        while ((dirName == "" || dirName == null) && --idx >= 0) {
                            dirName = paths[idx];
                        }
                        if (dirName == "" || dirName == null) {
                            getLog().warn("Could not deduce the last directory name ")
                            dirName = "update";
                        }
                        */
                        out.println("RewriteRule " + remoteDirectory + "/(.*) " + remoteDirectory + postfix + "/$1");
                        out.close();

                        getLog().info("Created .htaccess file " + htaccessFileName + " which will be uploaded to: " + remotehtAccessFile + " on completion");
                    }

                    wagon.putDirectory(inputDirectory, updateSiteDirectory);

                    if (generateHtaccess) {
                        getLog().info("Uploading .htaccess file " + htaccessFileName + " to: " + remotehtAccessFile);
                        File htAccessFile = new File(htaccessFileName);
                        wagon.put(htAccessFile, remotehtAccessFile);
                    }
                } else {
                    wagon.putDirectory(inputDirectory, remoteDirectory);
                }
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Error uploading site", e);
        } finally {
            try {
                wagon.disconnect();
            } catch (ConnectionException e) {
                getLog().error("Error disconnecting wagon - ignored", e);
            }
        }
    }

    /**
     * <p>
     * Get the <code>ProxyInfo</code> of the proxy associated with the <code>host</code>
     * and the <code>protocol</code> of the given <code>repository</code>.
     * </p>
     * <p>
     * Extract from <a href="http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html">
     * J2SE Doc : Networking Properties - nonProxyHosts</a> : "The value can be a list of hosts,
     * each separated by a |, and in addition a wildcard character (*) can be used for matching"
     * </p>
     * <p>
     * Defensively support for comma (",") and semi colon (";") in addition to pipe ("|") as separator.
     * </p>
     *
     * @param repository   the Repository to extract the ProxyInfo from.
     * @param wagonManager the WagonManager used to connect to the Repository.
     * @return a ProxyInfo object instantiated or <code>null</code> if no matching proxy is found
     */
    public static ProxyInfo getProxyInfo(Repository repository, WagonManager wagonManager) {
        ProxyInfo proxyInfo = wagonManager.getProxy(repository.getProtocol());

        if (proxyInfo == null) {
            return null;
        }

        String host = repository.getHost();
        String nonProxyHostsAsString = proxyInfo.getNonProxyHosts();
        String[] nonProxyHosts = StringUtils.split(nonProxyHostsAsString, ",;|");
        for (int i = 0; i < nonProxyHosts.length; i++) {
            String nonProxyHost = nonProxyHosts[i];
            if (StringUtils.contains(nonProxyHost, "*")) {
                // Handle wildcard at the end, beginning or middle of the nonProxyHost
                String nonProxyHostPrefix = StringUtils.substringBefore(nonProxyHost, "*");
                String nonProxyHostSuffix = StringUtils.substringAfter(nonProxyHost, "*");
                // prefix*
                if (StringUtils.isNotEmpty(nonProxyHostPrefix) && host.startsWith(nonProxyHostPrefix)
                        && StringUtils.isEmpty(nonProxyHostSuffix)) {
                    return null;
                }
                // *suffix
                if (StringUtils.isEmpty(nonProxyHostPrefix)
                        && StringUtils.isNotEmpty(nonProxyHostSuffix) && host.endsWith(nonProxyHostSuffix)) {
                    return null;
                }
                // prefix*suffix
                if (StringUtils.isNotEmpty(nonProxyHostPrefix) && host.startsWith(nonProxyHostPrefix)
                        && StringUtils.isNotEmpty(nonProxyHostSuffix) && host.endsWith(nonProxyHostSuffix)) {
                    return null;
                }
            } else if (host.equals(nonProxyHost)) {
                return null;
            }
        }
        return proxyInfo;
    }

    /**
     * Configure the Wagon with the information from serverConfigurationMap ( which comes from settings.xml )
     *
     * @param wagon
     * @param repositoryId
     * @param settings
     * @param container
     * @param log
     * @throws WagonConfigurationException
     * @todo Remove when {@link WagonManager#getWagon(Repository) is available}. It's available in Maven 2.0.5.
     */
    static void configureWagon(Wagon wagon, String repositoryId, Settings settings, PlexusContainer container,
                               Log log)
            throws WagonConfigurationException {
        // MSITE-25: Make sure that the server settings are inserted
        for (int i = 0; i < settings.getServers().size(); i++) {
            Server server = (Server) settings.getServers().get(i);
            String id = server.getId();
            if (id != null && id.equals(repositoryId)) {
                if (server.getConfiguration() != null) {
                    final PlexusConfiguration plexusConf =
                            new XmlPlexusConfiguration((Xpp3Dom) server.getConfiguration());

                    ComponentConfigurator componentConfigurator = null;
                    try {
                        componentConfigurator = (ComponentConfigurator) container.lookup(ComponentConfigurator.ROLE);
                        componentConfigurator.configureComponent(wagon, plexusConf, container.getContainerRealm());
                    } catch (final ComponentLookupException e) {
                        throw new WagonConfigurationException(repositoryId, "Unable to lookup wagon configurator."
                                + " Wagon configuration cannot be applied.", e);
                    } catch (ComponentConfigurationException e) {
                        throw new WagonConfigurationException(repositoryId, "Unable to apply wagon configuration.",
                                e);
                    } finally {
                        if (componentConfigurator != null) {
                            try {
                                container.release(componentConfigurator);
                            } catch (ComponentLifecycleException e) {
                                log.error("Problem releasing configurator - ignoring: " + e.getMessage());
                            }
                        }
                    }

                }

            }
        }
    }

    public void contextualize(Context context)
            throws ContextException {
        container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }

}
