/*
 * $Header$
 *
 * Copyright (c) 2001
 * Daune Consult SPRL
 * Rue du monument, 1
 * 6530 Thirimont (Belgium)
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.acplt.oncrpc.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.acplt.oncrpc.apps.jrpcgen.jrpcgen;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Ant task to run the ONC/RPC '.x' file compiler provided in the Remote Tea
 * library: jrpcgen.
 *
 * <p>The Remote Tea library is a complete open source implementation of
 * the ONC/RPC standard, developped by the Chair of Process Control
 * Engineering of University of Aachen, Germany.
 * <p>Remote Tea can be found at
 * <a href="http://www.plt.rwth-aachen.de/ks/english/remotetea.html">
 * http://www.plt.rwth-aachen.de/ks/english/remotetea.html</a>.
 *
 * <p>The task attributes are:
 * <ul>
 * <li>srcfile   : '.x' file to compile (mandatory)</li>
 * <li>destdir   : directory where generated files need to be placed
 *                 (mandatory). If a 'package' directive is used,
 *                 do <b>not</b> add the package directories to destDir
 *                 (it is done automatically by the task) </li>
 * <li>package   : package name to be used for generated files (optional)</li>
 * <li>createdir : indicates whether jrpcgen must create destdir if it does
 *                 not exist (optional). Defaults to no.</li>
 * <li>verbose   : indicates whether jrpcgen must be verbose (optional).
 *                 Defaults to no.</li>
 * <li>debug     : indicates whether jrpcgen must trace debug information
 *                 (optional). Defaults to no.</li>
 * <li>backup    : indicates whether jrpcgen must backup files (optional).
 *                 Defaults to no.</li>
 * </ul>
 *
 * @author <a href="mailto:daune.jf@daune-consult.com">Jean-Francois Daune</a>
 */
public class JrpcgenTask extends org.apache.tools.ant.Task {

    public void setSrcfile(java.io.File srcFile) {
        this.srcFile = srcFile;
    }

    public void setDestdir(java.io.File destDir) {
        this.destDir = destDir;
    }

    public void setPackage(java.lang.String packageName) {
        this.packageName = packageName;
    }

    public void setCreatedir(boolean createDir) {
        this.createDir = createDir;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setBackup(boolean backup) {
        this.backup = backup;
    }

    public void execute() throws BuildException {

        checkAttributes();

        try {
            System.out.println("Compiling " + srcFile.getCanonicalPath());
        }
        catch (IOException ex) {
        }

        if(packageName != null) {
            jrpcgen.packageName = packageName;

            try {
                // Add the package name to destination dir
                destDir = new File(
                    destDir.getCanonicalPath() +
                    File.separator +
                    packageName.replace('.', File.separatorChar));
            }
            catch (IOException ex) {
                throw new BuildException(ex); // should never occur
            }
        }

        if (createDir) {
            // Create the destination dir if it does not exist
            try {
                if (!destDir.exists()) {
                    boolean dirsCreated = destDir.mkdirs();
                    if (!dirsCreated) {
                        throw new BuildException("Could not create destination dir" );
                    }
                }
            }
            catch (SecurityException ex) {
                throw new BuildException(ex);
            }
        }

        if (debug)
            dumpState();

        jrpcgen.debug          = debug;
        jrpcgen.verbose        = verbose;
        jrpcgen.noBackups      = (!backup);
        jrpcgen.destinationDir = destDir;
        jrpcgen.xFile          = srcFile;

        try {
            jrpcgen.doParse();
        }
        catch (Throwable t) {
            throw new BuildException(t);
        }

    }

    private void checkAttributes() throws BuildException {
        if(srcFile == null)
            throw new BuildException("srcfile has not been set");

        if(destDir == null)
            throw new BuildException("destdir has not been set");

        try {
            if(!srcFile.isFile())
                throw new BuildException("problem reading srcdir");

            if(!destDir.isDirectory())
                throw new BuildException("problem accessing srcdir");
        }
        catch(SecurityException ex) {
            throw new BuildException(ex);
        }
    }

    private void dumpState() {
        System.out.println(srcFile);
        System.out.println(destDir);
        System.out.println(packageName);
        System.out.println(backup);
        System.out.println(debug);
        System.out.println(verbose);
        System.out.println(createDir);
    }

    private java.io.File srcFile;
    private java.io.File destDir;
    private String packageName;

    /**
     * Task attribute "debug".
     */
    private boolean debug     = false;
    private boolean verbose   = false;
    private boolean backup    = false;
    private boolean createDir = false;
}