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

public class HttpTunnelPortmapTest {

    public HttpTunnelPortmapTest()
        throws OncRpcException, IOException {

        //
        // Create a portmap client object, which can then be used to contact
        // a local or remote ONC/RPC portmap process. In this test we contact
        // the local portmapper.
        //
        OncRpcClient portmap =
            new OncRpcHttpClient(
                "localhost", // name of host where the tunnel gateway resides
                "/teatunnel", // path to gateway
                "localhost", // name of host where ONC/RPC server is located
                OncRpcPortmapClient.PMAP_PROGRAM,
                OncRpcPortmapClient.PMAP_VERSION,
                OncRpcPortmapClient.PMAP_PORT,
                OncRpcProtocols.ONCRPC_TCP // transport protocol to use by
                                           // gateway to contact ONC/RPC server.
                );

        //
        // Ping the portmapper...
        //
        System.out.print("pinging portmapper: ");
        try {
            portmap.call(0, XdrVoid.XDR_VOID, XdrVoid.XDR_VOID);
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
            port = getPort(portmap, 1, 1, OncRpcProtocols.ONCRPC_UDP);
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
            setPort(portmap, 1, 42, OncRpcProtocols.ONCRPC_UDP, 65535);
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
            list = listServers(portmap);
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
            unsetPort(portmap, 1, 42);
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
            list = listServers(portmap);
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
        System.out.println("HttpTunnelPortmapTest");
        try {
            new HttpTunnelPortmapTest();
        } catch ( Exception e ) {
           e.printStackTrace(System.out);
        }
    }

    //
    // Directly stolen from org.acplt.oncrpc.OncRpcPortmapClient...
    //
    public int getPort(OncRpcClient client, int program, int version, int protocol)
        throws OncRpcException {
        //
        // Fill in the request parameters. Note that params.port is
        // not used. BTW - it is automatically initialized as 0 by the
        // constructor of the OncRpcServerParams class.
        //
        OncRpcServerIdent params =
            new OncRpcServerIdent(program, version, protocol, 0);
        OncRpcGetPortResult result = new OncRpcGetPortResult();
        //
        // Try to contact the portmap process. If something goes "boing"
        // at this stage, then rethrow the exception as a generic portmap
        // failure exception. Otherwise, if the port number returned is
        // zero, then no appropriate server was found. In this case,
        // throw an exception, that the program requested could not be
        // found.
        //
        try {
            client.call(OncRpcPortmapServices.PMAP_GETPORT, params, result);
        } catch ( OncRpcException e ) {
            throw(new OncRpcException(OncRpcException.RPC_PMAPFAILURE));
        }
        //
        // In case the program is not registered, throw an exception too.
        //
        if ( result.port == 0 ) {
            throw(new OncRpcProgramNotRegisteredException());
        }
        return result.port;
    }

    public boolean setPort(OncRpcClient client, int program, int version, int protocol, int port)
        throws OncRpcException {
        //
        // Fill in the request parameters.
        //
        OncRpcServerIdent params =
            new OncRpcServerIdent(program, version, protocol, port);
        XdrBoolean result = new XdrBoolean(false);
        //
        // Try to contact the portmap process. If something goes "boing"
        // at this stage, then rethrow the exception as a generic portmap
        // failure exception.
        //
        try {
            client.call(OncRpcPortmapServices.PMAP_SET, params, result);
        } catch ( OncRpcException e ) {
            throw(new OncRpcException(OncRpcException.RPC_PMAPFAILURE));
        }
        return result.booleanValue();
    }

    public boolean unsetPort(OncRpcClient client, int program, int version)
        throws OncRpcException {
        //
        // Fill in the request parameters.
        //
        OncRpcServerIdent params =
            new OncRpcServerIdent(program, version, 0, 0);
        XdrBoolean result = new XdrBoolean(false);
        //
        // Try to contact the portmap process. If something goes "boing"
        // at this stage, then rethrow the exception as a generic portmap
        // failure exception.
        //
        try {
            client.call(OncRpcPortmapServices.PMAP_UNSET, params, result);
        } catch ( OncRpcException e ) {
            throw(new OncRpcException(OncRpcException.RPC_PMAPFAILURE));
        }
        return result.booleanValue();
    }

    public OncRpcServerIdent [] listServers(OncRpcClient client)
           throws OncRpcException {
        //
        // Fill in the request parameters.
        //
        OncRpcDumpResult result = new OncRpcDumpResult();
        //
        // Try to contact the portmap process. If something goes "boing"
        // at this stage, then rethrow the exception as a generic portmap
        // failure exception.
        //
        try {
            client.call(OncRpcPortmapServices.PMAP_DUMP, XdrVoid.XDR_VOID, result);
        } catch ( OncRpcException e ) {
            throw(new OncRpcException(OncRpcException.RPC_PMAPFAILURE));
        }
        //
        // Copy the server ident object references from the Vector
        // into the vector (array).
        //
        OncRpcServerIdent [] info =
            new OncRpcServerIdent[result.servers.size()];
        result.servers.copyInto(info);
        return info;
    }

}

// End of HttpTunnelPortmapTest.java
