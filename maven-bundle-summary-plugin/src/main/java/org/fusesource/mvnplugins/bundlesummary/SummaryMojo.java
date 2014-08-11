/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.fusesource.mvnplugins.bundlesummary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

/**
 * A plugin for generating report on OSGi imports/exports
 *
 * @goal summary
 * @phase verify
 */
@Mojo(name = "summary", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class SummaryMojo extends AbstractMojo {

    /**
     * The Maven Session Object
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    @Parameter(property = "session", readonly = true, required = true)
    protected MavenSession session;

    /**
     * @parameter default-value="${project.build.directory}/bundle-summary.xml"
     */
    @Parameter(defaultValue = "${project.build.directory}/bundle-summary.xml")
    protected File xmlReport;

    /**
     * @parameter default-value="${project.build.directory}/bundle-report.xslt"
     */
    @Parameter(defaultValue = "${project.build.directory}/bundle-report.xslt")
    protected File stylesheet;

    public void execute() throws MojoExecutionException, MojoFailureException {
        // symbolicName:version -> metadata
        Map<String, BundleMetadata> bundles = new TreeMap<String, BundleMetadata>();
        // imported package without version -> map(version -> List<BundleMetadata>)
        Map<String, Map<String, Set<BundleMetadata>>> imports = new TreeMap<String, Map<String, Set<BundleMetadata>>>();
        // exported package without version -> map(version -> List<BundleMetadata>)
        Map<String, Map<String, Set<BundleMetadata>>> exports = new TreeMap<String, Map<String, Set<BundleMetadata>>>();

        getLog().info("Gathering metadata of reactor's bundles (" + session.getProjects().size() + " artifacts)");
        for (MavenProject project : session.getProjects()) {
            Artifact artifact = project.getArtifact();
            if (artifact == null || !"bundle".equals(artifact.getType())) {
                continue;
            }
            if (getLog().isDebugEnabled()) {
                getLog().debug("Verifying bundle " + artifact.getArtifactId());
            }
            File bundle = artifact.getFile();
            if (bundle == null) {
                getLog().info("No file for artifact " + artifact.getArtifactId());
                continue;
            }
            JarFile jar = null;
            try {
                jar = new JarFile(bundle);
                Manifest m = jar.getManifest();
                if (m == null || m.getMainAttributes() == null) {
                    continue;
                }
                Attributes mainAttributes = m.getMainAttributes();
                String symbolicName = mainAttributes.getValue("Bundle-SymbolicName");
                if (symbolicName.contains(";")) {
                    // to remove ";blueprint.graceperiod:=false"
                    symbolicName = symbolicName.substring(0, symbolicName.indexOf(';'));
                }
                String version = mainAttributes.getValue("Bundle-Version");
                String exportsInfo = mainAttributes.getValue("Export-Package");
                String importsInfo = mainAttributes.getValue("Import-Package");
                String reqs = mainAttributes.getValue("Require-Capability");
                String provides = mainAttributes.getValue("Provide-Capability");

                String key = String.format("%s:%s", symbolicName, version);
                BundleMetadata metadata = new BundleMetadata();
                bundles.put(key, metadata);

                metadata.setSymbolicName(symbolicName);
                metadata.setVersion(version);

                // Import-Package
                Parameters importParameters = OSGiHeader.parseHeader(importsInfo);
                for (String packageName: importParameters.keySet()) {
                    String packageVersion = importParameters.get(packageName).getVersion();
                    if (packageVersion == null) {
                        packageVersion = "";
                    }
                    PackageImport packageImport = new PackageImport(packageName, packageVersion);
                    packageImport.setOtherAttributes(importParameters.get(packageName));
                    metadata.getImports().add(packageImport);
                    if (!imports.containsKey(packageName)) {
                        imports.put(packageName, new TreeMap<String, Set<BundleMetadata>>());
                    }
                    if (!imports.get(packageName).containsKey(packageVersion)) {
                        imports.get(packageName).put(packageVersion, new TreeSet<BundleMetadata>());
                    }
                    imports.get(packageName).get(packageVersion).add(metadata);
                }

                // Export-Package
                Parameters exportParameters = OSGiHeader.parseHeader(exportsInfo);
                for (String packageName: exportParameters.keySet()) {
                    String packageVersion = exportParameters.get(packageName).getVersion();
                    if (packageVersion == null) {
                        packageVersion = "";
                    }
                    PackageExport packageExport = new PackageExport(packageName, packageVersion);
                    packageExport.setOtherAttributes(exportParameters.get(packageName));
                    metadata.getExports().add(packageExport);
                    if (!exports.containsKey(packageName)) {
                        exports.put(packageName, new TreeMap<String, Set<BundleMetadata>>());
                    }
                    if (!exports.get(packageName).containsKey(packageVersion)) {
                        exports.get(packageName).put(packageVersion, new TreeSet<BundleMetadata>());
                    }
                    exports.get(packageName).get(packageVersion).add(metadata);
                }

                // private packages - all found packages, which are not exported
                for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                    JarEntry entry = e.nextElement();
                    if (entry.getName() != null && entry.getName().endsWith(".class")) {
                        String packageName = entry.getName().substring(0, entry.getName().lastIndexOf("/"));
                        packageName = packageName.replaceAll("/", ".");
                        if (!exports.containsKey(packageName)) {
                            metadata.getPrivatePackages().add(packageName);
                        }
                    }
                }

                // Provide-Capability/Require-Capability
                Parameters reqsParameters = OSGiHeader.parseHeader(reqs);
                for (String capabilityName: reqsParameters.keySet()) {
                    Capability capability = new Capability(capabilityName);
                    capability.setOtherAttributes(exportParameters.get(capabilityName));
                    metadata.getRequiredCapabilities().add(capability);
                    // TODO: report on matching reqs/provides to resolve conflicts
                }
                Parameters providesParameters = OSGiHeader.parseHeader(provides);
                for (String capabilityName: providesParameters.keySet()) {
                    Capability capability = new Capability(capabilityName);
                    capability.setOtherAttributes(exportParameters.get(capabilityName));
                    metadata.getProvidedCapabilities().add(capability);
                    // TODO: report on matching reqs/provides to resolve conflicts
                }
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            } finally {
                if (jar != null) {
                    try {
                        jar.close();
                    } catch (IOException e) {
                        throw new MojoExecutionException(e.getMessage(), e);
                    }
                }
            }
        }

        // report & verification time
        getLog().info("Verifying metadata of reactor's bundles");

        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(this.xmlReport), "UTF-8");
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<?xml-stylesheet type=\"text/xsl\" href=\"bundle-report.xslt\"?>\n");
            writer.write("<bundle-report xmlns=\"http://fabric8.io/bundle-report\">\n");

            writer.write("    <bundles>\n");
            for (String id : bundles.keySet()) {
                BundleMetadata bm = bundles.get(id);
                writer.write("        <bundle symbolic-name=\"" + bm.getSymbolicName() + "\" version=\"" + bm.getVersion() + "\">\n");
                writer.write("            <imports>\n");
                for (PackageImport pi : bm.getImports()) {
                    writer.write("                <import package=\"" + pi.getPackageName() + "\" version=\"" + pi.getVersion() + "\" />\n");
                }
                writer.write("            </imports>\n");
                writer.write("            <exports>\n");
                for (PackageExport pe : bm.getExports()) {
                    writer.write("                <export package=\"" + pe.getPackageName() + "\" version=\"" + pe.getVersion() + "\" />\n");
                }
                writer.write("            </exports>\n");
                writer.write("            <privates>\n");
                for (String p : bm.getPrivatePackages()) {
                    writer.write("                <private package=\"" + p + "\" />\n");
                }
                writer.write("            </privates>\n");
                writer.write("            <provided-capabilities>\n");
                for (Capability c : bm.getProvidedCapabilities()) {
                    writer.write("                <capability name=\"" + c.getName() + "\" />\n");
                }
                writer.write("            </provided-capabilities>\n");
                writer.write("            <required-capabilities>\n");
                for (Capability c : bm.getRequiredCapabilities()) {
                    writer.write("                <capability name=\"" + c.getName() + "\" />\n");
                }
                writer.write("            </required-capabilities>\n");
                writer.write("        </bundle>\n");
            }
            writer.write("    </bundles>\n");

            writer.write("    <imports>\n");
            // for imports we should have no imports with different versions/version ranges
            importInformation(imports, writer);
            writer.write("    </imports>\n");

            writer.write("    <exports>\n");
            // for exports we should have only one bundle which exports any symbolicName:version to avoid split-packages
            exportInformation(exports, writer);
            writer.write("    </exports>\n");

            // are there any conflicts left?
            if (imports.size() > 0) {
                writer.write("    <import-conflicts>\n");
                importInformation(imports, writer);
                writer.write("    </import-conflicts>\n");
            }
            if (exports.size() > 0) {
                writer.write("    <export-conflicts>\n");
                exportInformation(exports, writer);
                writer.write("    </export-conflicts>\n");
            }
            writer.write("</bundle-report>\n");

            FileOutputStream fos = new FileOutputStream(this.stylesheet);
            IOUtil.copy(getClass().getResourceAsStream("/bundle-report.xslt"), fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }
    }

    private void importInformation(Map<String, Map<String, Set<BundleMetadata>>> imports, Writer writer) throws IOException {
        for (Iterator<String> iterator = imports.keySet().iterator(); iterator.hasNext(); ) {
            String packageName = iterator.next();
            Map<String, Set<BundleMetadata>> versions = imports.get(packageName);
            writer.write("        <import package=\"" + packageName + "\">\n");
            for (String version : versions.keySet()) {
                writer.write("            <version version=\"" + version + "\">\n");
                for (BundleMetadata bm : versions.get(version)) {
                    writer.write("                <by-bundle symbolic-name=\"" + bm.getSymbolicName() + "\" version=\"" + bm.getVersion() + "\" />\n");
                }
                writer.write("            </version>\n");
            }
            writer.write("        </import>\n");
            if (versions.size() == 1) {
                // if there's only one version of imported package, we're safe
                iterator.remove();
            }
        }
    }

    private void exportInformation(Map<String, Map<String, Set<BundleMetadata>>> exports, Writer writer) throws IOException {
        for (Iterator<String> iterator = exports.keySet().iterator(); iterator.hasNext(); ) {
            String packageName = iterator.next();
            Map<String, Set<BundleMetadata>> versions = exports.get(packageName);
            writer.write("        <export package=\"" + packageName + "\">\n");
            for (String version : versions.keySet()) {
                writer.write("            <version version=\"" + version + "\">\n");
                for (BundleMetadata bm : versions.get(version)) {
                    writer.write("                <by-bundle symbolic-name=\"" + bm.getSymbolicName() + "\" version=\"" + bm.getVersion() + "\" />\n");
                }
                writer.write("            </version>\n");
            }
            writer.write("        </export>\n");
            if (exports.get(packageName).size() == 1) {
                // package exported with only one version ...
                String singleVersion = exports.get(packageName).keySet().iterator().next();
                if (exports.get(packageName).get(singleVersion).size() == 1) {
                    // ... and package:version exported by only one bundle
                    iterator.remove();
                }
            }
        }
    }

}
