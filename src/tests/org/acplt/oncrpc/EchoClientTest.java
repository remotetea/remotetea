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

public class EchoClientTest {

    public EchoClientTest()
        throws OncRpcException, IOException {

        //
        // Create a portmap client object, which can then be used to contact
        // the local ONC/RPC ServerTest test server.
        //
        OncRpcClient client =
            new OncRpcTcpClient(InetAddress.getByName("localhost"),
                                0x49679, 1, 0);

        //
        // Ping the test server...
        //
        System.out.print("pinging test server: ");
        try {
            client.call(0, XdrVoid.XDR_VOID, XdrVoid.XDR_VOID);
        } catch ( OncRpcException e ) {
            System.out.println("method call failed unexpectedly:");
            e.printStackTrace(System.out);
            System.exit(1);
        }
        System.out.println("test server is alive.");

        //
        // Now check the echo RPC call...
        //
        String [] messages =
            { "Open Source", "is not yet", "another buzzword." };
        checkEcho(client, messages);

        //
        // Now test AUTH_UNIX authentication. First start with an
        // invalid credential...
        //
        OncRpcClientAuth auth = new OncRpcClientAuthUnix(
            "marvin", 0, 0, new int[0]);
        client.setAuth(auth);
        System.out.print("checking AUTH_UNIX with invalid credential: ");
        try {
            client.call(0, XdrVoid.XDR_VOID, XdrVoid.XDR_VOID);
        } catch ( OncRpcAuthenticationException ae ) {
            if ( ae.getAuthStatus() != OncRpcAuthStatus.ONCRPC_AUTH_BADCRED ) {
                System.out.println("received wrong authentication exception with status of "
                                   + ae.getAuthStatus());
                ae.printStackTrace(System.out);
                System.exit(1);
            }
        } catch ( OncRpcException e ) {
            System.out.println("method call failed unexpectedly:");
            e.printStackTrace(System.out);
            System.exit(1);
        }
        System.out.println("passed.");

        auth = new OncRpcClientAuthUnix(
            "marvin", 42, 815, new int[0]);
        client.setAuth(auth);
        messages = new String[]
            { "AUTH_UNIX", "is like", "*NO* authentication",
              "--", "it", "uses", "*NO CRYPTOGRAPHY*", "for securing",
              "ONC/RPC messages" };
        checkEcho(client, messages);

        client.setAuth(null);

        //
        // Release resources bound by ONC/RPC client object as soon as possible
        // so might help the garbage wo/man. Yeah, this is now a political
        // correct comment.
        //
        client.close();
        client = null;
    }

    public void checkEcho(OncRpcClient client, String [] messages) {
        for ( int idx = 0; idx < messages.length; ++idx ) {
            XdrString params = new XdrString(messages[idx]);
            XdrString result = new XdrString();
            System.out.print("checking echo: ");
            try {
                client.call(1, params, result);
            } catch ( OncRpcException e ) {
                System.out.println("method call failed unexpectedly:");
                e.printStackTrace(System.out);
                System.exit(1);
            }
            if ( !params.stringValue().equals(result.stringValue()) ) {
                System.out.println("answer does not match call:");
                System.out.println("  expected: \"" + params.stringValue() + "\"");
                System.out.println("  but got:  \"" + result.stringValue() + "\"");
                System.exit(1);
            }
            System.out.println(result.stringValue());
        }
    }

    public static void main(String[] args) {
        System.out.println("EchoClientTest");
        try {
            new EchoClientTest();
        } catch ( Exception e ) {
           e.printStackTrace(System.out);
        }
    }

}

// End of EchoClientTest.java