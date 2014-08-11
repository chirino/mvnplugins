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

import java.util.LinkedList;
import java.util.List;

/**
 * Essential information from bundle MANIFEST.MF file
 */
public class BundleMetadata implements Comparable<BundleMetadata> {

    private String symbolicName;
    private String version;
    private List<PackageImport> imports = new LinkedList<PackageImport>();
    private List<PackageExport> exports = new LinkedList<PackageExport>();
    private List<String> privatePackages = new LinkedList<String>();
    private List<Capability> requiredCapabilities = new LinkedList<Capability>();
    private List<Capability> providedCapabilities = new LinkedList<Capability>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BundleMetadata that = (BundleMetadata) o;

        return symbolicName.equals(that.symbolicName) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        int result = symbolicName.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    public int compareTo(BundleMetadata o) {
        int r1 = this.symbolicName.compareTo(o.symbolicName);
        return r1 == 0 ? this.version.compareTo(o.version) : r1;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<PackageImport> getImports() {
        return imports;
    }

    public List<PackageExport> getExports() {
        return exports;
    }

    public List<String> getPrivatePackages() {
        return privatePackages;
    }

    public List<Capability> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public List<Capability> getProvidedCapabilities() {
        return providedCapabilities;
    }

}
