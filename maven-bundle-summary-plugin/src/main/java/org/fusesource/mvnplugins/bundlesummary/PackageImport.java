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

import aQute.bnd.header.Attrs;

/**
 * Information about single <code>Import-Package</code> from a bundle's MANIFEST.MF
 */
public class PackageImport {

    private String packageName;
    private String version;
    private Attrs otherAttributes;

    public PackageImport(String packageName, String version) {
        this.packageName = packageName;
        this.version = version;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Attrs getOtherAttributes() {
        return otherAttributes;
    }

    public void setOtherAttributes(Attrs otherAttributes) {
        this.otherAttributes = otherAttributes;
    }

}
