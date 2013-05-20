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
package org.fusesource.mvnplugins.consolets;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Installs a console hook so that maven messages contain a timestamp.
 * <p/>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @phase validate
 * @goal install
 */
public class ConsoleTSMojo extends AbstractMojo {

    static PrintStream originalOut;
    static {
        originalOut = System.out;
    }

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Should time stamping be enabled.
     * <br/>
     *
     * @parameter expression="${console.timestamp}" default="false"
     */
    protected boolean enabled;

    /**
     * The time format used.
     * <br/>
     *
     * @parameter default-value="HH:mm:ss"
     */
    protected String format;

    class TimeStampOutputStream extends FilterOutputStream {
        SimpleDateFormat sdf = new SimpleDateFormat(format);


        TimeStampOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        AtomicBoolean prefixNextWrite = new AtomicBoolean(true);

        @Override
        public void write(int i) throws IOException {
            if( prefixNextWrite.compareAndSet(true, false) ) {
                super.write(("["+sdf.format(new Date())+"] ").getBytes());
            }
            super.write(i);
            if( i == '\n' ) {
                prefixNextWrite.set(true);
            }
        }
    }

    public void execute() throws MojoExecutionException {
        if( enabled ) {
            System.setOut(new PrintStream(new TimeStampOutputStream(originalOut)));
        } else {
            System.setOut(originalOut);
        }
    }


}