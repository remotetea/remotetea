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
import org.acplt.oncrpc.server.*;


public class ServerTest implements OncRpcDispatchable {

    public class ShutdownHookThread extends Thread {

        private ServerTest svr;

        public ShutdownHookThread(ServerTest svr) {
            this.svr = svr;
        }

        public void run() {
            System.out.println("Shutdown Hook Thread activated");
            svr.shutdown();
        }
    }

    //
    // Flag to signal shut down of the server.
    //
    public Object leaveTheStage = new Object();

    //
    // Shorthand credential use counter.
    //
    public int shorthandCredCounter = 0;

    //
    //
    //
    public ServerTest()
        throws OncRpcException, IOException {

        OncRpcUdpServerTransport udpTrans =
            new OncRpcUdpServerTransport(this, 55555, 0x49679, 1, 8192);
        final OncRpcTcpServerTransport tcpTrans =
            new OncRpcTcpServerTransport(this, 55555, 0x49679, 1, 8192);

        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(this));

        udpTrans.register();
        tcpTrans.register();

        System.out.println("Server started.");

        tcpTrans.listen();
        udpTrans.listen();

        //
        // Reality Check: open a connection to the TCP/IP server socket
        // waiting for incoming connection, thus testing proper shutdown
        // behaviour later...
        //
        OncRpcTcpClient client = new OncRpcTcpClient(
            InetAddress.getByName("127.0.0.1"), 0x49679, 1, 0);

        //
        // Now wait for the shutdown to become signalled... Note that
        // a simple call to wait() without the outer loop would not be
        // sufficient as we might get spurious wake-ups due to
        // interruptions.
        //
        for ( ;; ) {
            synchronized ( leaveTheStage ) {
                try {
                    leaveTheStage.wait();
                    break;
                } catch ( InterruptedException e ) {
                }
            }
        }
        System.out.println("Server shutting down...");
        //
        // Unregister TCP-based transport. Then close it. This will
        // also automatically bring down the threads handling individual
        // TCP transport connections.
        //
        tcpTrans.unregister();
        tcpTrans.close();
        //
        // Unregister UDP-based transport. Then close it.  This will
        // automatically bring down the thread which handles the
        // UDP transport.
        //
        udpTrans.unregister();
        udpTrans.close();

        System.out.println("Server shut down.");

    }

    //
    // Indicate that the server should be shut down. Sad enough that the
    // Java Environment can not cope with signals. I know that they're not
    // universially available -- but why shouldn't be there a class providing
    // this functionality and in case the runtime environment does not support
    // sending and receiving signals, it either throws an exception or gives
    // some indication otherwise. It wouldn't be bad and we would be sure
    // that the appropriate class is always available.
    //
    public void shutdown() {
        if ( leaveTheStage != null ) {
            System.out.println("Requesting server to shut down...");
            synchronized ( leaveTheStage ) {
                leaveTheStage.notify();
            }
        }
    }

    //
    // Handle incomming calls...
    //
    public void dispatchOncRpcCall(OncRpcCallInformation call,
                                   int program, int version, int procedure)
           throws OncRpcException, IOException {
        //
        // Spit out some diagnosis messages...
        //
        System.out.println("Incomming call for program "
                           + Integer.toHexString(program)
                           + "; version " + version
                           + "; procedure " + Integer.toHexString(procedure)
                           + "; auth type " + call.callMessage.auth.getAuthenticationType());
        //
        // Handle incomming credentials...
        //
        if ( call.callMessage.auth.getAuthenticationType()
               == OncRpcAuthType.ONCRPC_AUTH_UNIX ) {
            OncRpcServerAuthUnix auth = (OncRpcServerAuthUnix) call.callMessage.auth;
            if ( (auth.uid != 42)
                 && (auth.gid != 815) ) {
                throw(new OncRpcAuthenticationException(
                              OncRpcAuthStatus.ONCRPC_AUTH_BADCRED));
            }
            //
            // Suggest shorthand authentication...
            //
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(8);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(42);
            xdr.xdrEncodeInt(~42);
            xdr.endEncoding();
            //
            // ATTENTION: this will return the *whole* buffer created by the
            // constructor of XdrBufferEncodingStream(len) above. So make sure
            // that you really want to return the whole buffer!
            //
            auth.setShorthandVerifier(xdr.getXdrData());
        } else if ( call.callMessage.auth.getAuthenticationType()
                      == OncRpcAuthType.ONCRPC_AUTH_SHORT ) {
            //
            // Check shorthand credentials.
            //
            OncRpcServerAuthShort auth = (OncRpcServerAuthShort) call.callMessage.auth;
            XdrBufferDecodingStream xdr =
                new XdrBufferDecodingStream(auth.getShorthandCred());
            xdr.beginDecoding();
            int credv1 = xdr.xdrDecodeInt();
            int credv2 = xdr.xdrDecodeInt();
            xdr.endDecoding();
            if ( credv1 != ~credv2 ) {
                System.out.println("AUTH_SHORT rejected");
                throw(new OncRpcAuthenticationException(
                              OncRpcAuthStatus.ONCRPC_AUTH_REJECTEDCRED));
            }
            if ( (++shorthandCredCounter % 3) == 0 ) {
                System.out.println("AUTH_SHORT too old");
                throw(new OncRpcAuthenticationException(
                              OncRpcAuthStatus.ONCRPC_AUTH_REJECTEDCRED));
            }
            System.out.println("AUTH_SHORT accepted");
        }
        //
        // Now dispatch incomming calls...
        //
        switch ( procedure ) {
        case 0:
            //
            // The usual ONC/RPC PING (aka "NULL" procedure)
            //
            call.retrieveCall(XdrVoid.XDR_VOID);
            call.reply(XdrVoid.XDR_VOID);
            break;
        case 1: {
            //
            // echo string parameter
            //
            XdrString param = new XdrString();
            call.retrieveCall(param);
            System.out.println("Parameter: \"" + param.stringValue() + "\"");
            call.reply(param);
            break;
        }
        case 42:
            //
            // This is a special call to shut down the server...
            //
            if ( (program == 42) && (version == 42) ) {
                call.retrieveCall(XdrVoid.XDR_VOID);
                call.reply(XdrVoid.XDR_VOID);
                shutdown();
                break;
            }
        //
        // For all unknown calls, send back an error reply.
        //
        default:
            call.failProcedureUnavailable();
        }
    }


    public static void main(String[] args) {
        System.out.println("ServerTest");
        try {
            new ServerTest();
        } catch ( Exception e ) {
           e.printStackTrace(System.out);
        }
    }

}

// End of PortmapGetPortTest.java
