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
import java.util.Vector;
import org.acplt.oncrpc.*;

public class BroadcastClientTest implements OncRpcBroadcastListener {

    //
    // List of addresses of portmappers that replied to our call...
    //
    Vector portmappers = new Vector();

    //
    // Remember addresses of replies for later processing. Please note
    // that you should not do any lengthy things (like DNS name lookups)
    // in this event handler, as you will otherwise miss some incomming
    // replies because the OS will drop them.
    //
    public void replyReceived(OncRpcBroadcastEvent evt) {
        portmappers.add(evt.getReplyAddress());
        System.out.print(".");
    }

    public BroadcastClientTest()
        throws OncRpcException, IOException {

        //
        // Create a portmap client object, which can then be used to contact
        // the local ONC/RPC ServerTest test server.
        //
        OncRpcUdpClient client =
            new OncRpcUdpClient(InetAddress.getByName("255.255.255.255"),
                                100000, 2, 111);

        //
        // Ping all portmappers in this subnet...
        //
        System.out.print("pinging portmappers in subnet: ");
        client.setTimeout(5*1000);
        try {
            client.broadcastCall(0,
                                 XdrVoid.XDR_VOID, XdrVoid.XDR_VOID,
                                 this);
        } catch ( OncRpcException e ) {
            System.out.println("method call failed unexpectedly:");
            e.printStackTrace(System.out);
            System.exit(1);
        }
        System.out.println("done.");

        //
        // Print addresses of all portmappers found...
        //
        for ( int idx = 0; idx < portmappers.size(); ++idx ) {
            System.out.println("Found: " +
                               ((InetAddress) portmappers.elementAt(idx)).getHostName() +
                               " (" +
                               ((InetAddress) portmappers.elementAt(idx)).getHostAddress() +
                               ")");
        }

        //
        // Release resources bound by portmap client object as soon as possible
        // so might help the garbage wo/man. Yeah, this is now a political
        // correct comment.
        //
        client.close();
        client = null;
    }

    public static void main(String[] args) {
        System.out.println("BroadcastClientTest");
        try {
            new BroadcastClientTest();
        } catch ( Exception e ) {
           e.printStackTrace(System.out);
        }
    }

}

// End of BroadcastClientTest.java