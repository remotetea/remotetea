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

public class PortmapGetPortTest {

    public PortmapGetPortTest()
        throws OncRpcException, IOException {

        //
        // Create a portmap client object, which can then be used to contact
        // a local or remote ONC/RPC portmap process. In this test we contact
        // the local portmapper.
        //
        OncRpcPortmapClient portmap =
            new OncRpcPortmapClient(InetAddress.getByName("localhost"));
        //portmap.setRetransmissionMode(OncRpcUdpRetransmissionMode.FIXED);
        //portmap.setRetransmissionTimeout(3*1000);

        //
        // Ping the portmapper...
        //
        System.out.print("pinging portmapper: ");
        try {
            portmap.ping();
        } catch ( OncRpcException e ) {
            System.out.println("method call failed unexpectedly:");
            e.printStackTrace(System.out);
            System.exit(1);
        }
        System.out.println("portmapper is alive.");

        //
        // Ask for a non-existent ONC/RPC server.
        //
        int port;

        System.out.print("getPort() for non-existing program: ");
        try {
            port = portmap.getPort(1, 1, OncRpcProtocols.ONCRPC_UDP);
            System.out.println("method call failed (program found).");
        } catch ( OncRpcException e ) {
            if ( e.getReason() != OncRpcException.RPC_PROGNOTREGISTERED ) {
                System.out.println("method call failed unexpectedly:");
                e.printStackTrace(System.out);
                System.exit(10);
            }
            System.out.println("succeeded (RPC_PROGNOTREGISTERED).");
        }

        //
        // Register dummy ONC/RPC server.
        //
        System.out.print("setPort() dummy server identification: ");
        try {
            portmap.setPort(1, 42, OncRpcProtocols.ONCRPC_UDP, 65535);
        } catch ( OncRpcException e ) {
            System.out.println("method call failed unexpectedly:");
            e.printStackTrace(System.out);
            System.exit(12);
        }
        System.out.println("succeeded.");

        //
        // Now dump the current list of registered servers.
        //
        OncRpcServerIdent [] list = null;
        int i;
        boolean found = false;

        System.out.print("listServers(): ");
        try {
            list = portmap.listServers();
        } catch ( OncRpcException e ) {
            System.out.println("method call failed unexpectedly:");
            e.printStackTrace(System.out);
            System.exit(20);
        }
        System.out.println("succeeded.");
        for ( i = 0; i < list.length; ++i ) {
            if ( (list[i].program == 1) && (list[i].version == 42)
                 && (list[i].protocol == OncRpcProtocols.ONCRPC_UDP)
                 && (list[i].port == 65535) ) {
                found = true;
            }
            System.out.println("  " + list[i].program + " "
                                    + list[i].version + " "
                                    + list[i].protocol + " "
                                    + list[i].port);
        }
        if ( !found ) {
            System.out.println("registered dummy server ident not found.");
            System.exit(22);
        }

        //
        // Deregister dummy ONC/RPC server.
        //
        System.out.print("unsetPort() dummy server identification: ");
        try {
            portmap.unsetPort(1, 42);
        } catch ( OncRpcException e ) {
            System.out.println("method call failed unexpectedly:");
            e.printStackTrace(System.out);
            System.exit(12);
        }
        System.out.println("succeeded.");

        //
        // Now dump again the current list of registered servers.
        //
        found = false;
        list = null;

        System.out.print("listServers(): ");
        try {
            list = portmap.listServers();
        } catch ( OncRpcException e ) {
            System.out.println("method call failed unexpectedly:");
            e.printStackTrace(System.out);
            System.exit(20);
        }
        System.out.println("succeeded.");
        for ( i = 0; i < list.length; ++i ) {
            if ( (list[i].program == 1) && (list[i].version == 42)
                 && (list[i].protocol == OncRpcProtocols.ONCRPC_UDP)
                 && (list[i].port == 65535) ) {
                found = true;
                break;
            }
        }
        if ( found ) {
            System.out.println("registered dummy server ident still found after deregistering.");
            System.exit(22);
        }

        //
        // Release resources bound by portmap client object as soon as possible
        // so might help the garbage wo/man. Yeah, this is now a political
        // correct comment.
        //
        portmap.close();
        portmap = null;
    }

    public static void main(String[] args) {
        System.out.println("PortmapGetPortTest");
        try {
            new PortmapGetPortTest();
        } catch ( Exception e ) {
           e.printStackTrace(System.out);
        }
    }

}

// End of PortmapGetPortTest.java
