/*
 * $Header$
 *
 * Copyright (c) 1999, 2000
 * Lehrstuhl fuer Prozessleittechnik (PLT), RWTH Aachen
 * D-52064 Aachen, Germany.
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

package tests.org.acplt.oncrpc;

import java.io.IOException;
import java.net.*;

import org.acplt.oncrpc.*;
import org.acplt.oncrpc.apps.jportmap.*;


public class EmbeddedPortmapTest {

    public OncRpcEmbeddedPortmap epm;

    public EmbeddedPortmapTest()
           throws IOException, OncRpcException, UnknownHostException {
        //
        // Diagnostic: Is a portmapper already running?
        //
        System.out.print("Checking for portmap service: ");
        boolean externalPortmap = OncRpcEmbeddedPortmap.isPortmapRunning();
        if ( externalPortmap ) {
            System.out.println("A portmap service is already running.");
        } else {
            System.out.println("No portmap service available.");
        }

        //
        // Create embedded portmap service and check whether is has sprung
        // into action.
        //
        System.out.print("Creating embedded portmap instance: ");
        try {
            epm = new OncRpcEmbeddedPortmap();
        } catch ( IOException e ) {
            System.out.println("ERROR: failed:");
            e.printStackTrace(System.out);
        } catch ( OncRpcException e ) {
            System.out.println("ERROR: failed:");
            e.printStackTrace(System.out);
        }
        if ( !epm.embeddedPortmapInUse() ) {
            System.out.print("embedded service not used: ");
        } else {
            System.out.print("embedded service started: ");
        }
        if ( epm.embeddedPortmapInUse() == externalPortmap ) {
            System.out.println("ERROR: no service available or both.");
            return;
        }
        System.out.println("Passed.");

        //
        // Now register dummy ONC/RPC program. Note that the embedded
        // portmap service must not automatically spin down when deregistering
        // the non-existing dummy program.
        //
        OncRpcPortmapClient pmap =
            new OncRpcPortmapClient(InetAddress.getByName("127.0.0.1"));

        System.out.print("Deregistering non-existing program: ");
        pmap.unsetPort(12345678, 42);
        System.out.println("Passed.");

        System.out.print("Registering dummy program: ");
        pmap.setPort(12345678, 42, OncRpcProtocols.ONCRPC_TCP, 42);
        System.out.println("Passed.");

        System.out.println("Press any key to continue...");
        byte [] b = new byte[1];
        System.in.read(b);

        System.out.print("Deregistering dummy program: ");
        pmap.unsetPort(12345678, 42);
        System.out.println("Passed.");

        System.out.println("Press any key to continue...");
        System.in.read(b);

        //
        // Check that an embedded portmap service spins down properly if it
        // was started within this test.
        //
        if ( OncRpcEmbeddedPortmap.isPortmapRunning() && !externalPortmap ) {
            System.out.println("ERROR: embedded portmap service still running.");
        }
    }


    public static void main(String[] args) {
        System.out.println("EmbeddedPortmapTest");
        try {
            new EmbeddedPortmapTest();
        } catch ( Exception e ) {
           e.printStackTrace(System.out);
        }
        System.out.println("Test finished.");
        System.out.println("Press any key to continue...");
        byte [] b = new byte[1];
        try { System.in.read(b); } catch ( IOException e ) { }
    }

}

// End of EmbeddedPortmapTest.java