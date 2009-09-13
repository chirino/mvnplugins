package org.fusesource.mvnplugins.uberize.transformer;

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

import org.codehaus.plexus.util.SelectorUtils;

import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;

/**
 * @author David Blevins
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class Paths {

    public Set includes;
    public Set excludes;

    public Paths() {
    }

    public Paths(Set includes, Set excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    public Set getIncludes() {
        return includes;
    }

    public Set getExcludes() {
        return excludes;
    }

    public boolean matchesIgnoreCase(String value) {
        return isIncluded(value, true) && !isExcluded(value, true);
    }

    public boolean matches(String value) {

        return isIncluded(value, false) && !isExcluded(value, false);
    }

    private boolean isIncluded(String value, boolean ignoreCase) {
        if (includes == null || includes.size() == 0) {
            return true;
        }

        return matchPaths(includes, value, ignoreCase);
    }

    private boolean isExcluded(String value, boolean ignoreCase) {
        if (excludes == null || excludes.size() == 0) {
            return false;
        }

        return matchPaths(excludes, value, ignoreCase);
    }

    private boolean matchPaths(Collection patterns, String value, boolean ignoreCase) {
        if( !ignoreCase ) {
            return matchPaths(patterns, value);
        }
        if (matchPaths(toLower(patterns), value.toLowerCase())) return true;
        if (matchPaths(toUpper(patterns), value.toUpperCase())) return true;
        return false;
    }

    private ArrayList toLower(Collection patterns) {
        ArrayList t = new ArrayList(patterns.size());
        for (Iterator iterator = patterns.iterator(); iterator.hasNext();) {
            String pattern = (String) iterator.next();
            t.add(pattern.toLowerCase());
        }
        return t;
    }
    
    private ArrayList toUpper(Collection patterns) {
        ArrayList t = new ArrayList(patterns.size());
        for (Iterator iterator = patterns.iterator(); iterator.hasNext();) {
            String pattern = (String) iterator.next();
            t.add(pattern.toUpperCase());
        }
        return t;
    }

    private boolean matchPaths(Collection p, String value) {
        for (Iterator iterator = p.iterator(); iterator.hasNext();) {
            String pattern = (String) iterator.next();
            if (SelectorUtils.matchPath(pattern, value)) {
                return true;
            }
        }
        return false;
    }

}