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

package org.acplt.oncrpc.server;

import org.acplt.oncrpc.*;
import java.io.IOException;

/**
 * The abstract <code>OncRpcServerStub</code> class is the base class to
 * build ONC/RPC-program specific servers upon. This class is typically
 * only used by jrpcgen generated servers, which provide a particular
 * set of remote procedures as defined in a x-file.
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public abstract class OncRpcServerStub {

    /**
     * Array containing ONC/RPC server transport objects which describe what
     * transports an ONC/RPC server offers for handling ONC/RPC calls.
     */
    public OncRpcServerTransport [] transports;

    /**
     * Array containing program and version numbers tuples this server is
     * willing to handle.
     */
    public OncRpcServerTransportRegistrationInfo [] info;

    /**
     * Notification flag for signalling the server to stop processing
     * incomming remote procedure calls and to shut down.
     */
    protected Object shutdownSignal = new Object();

    /**
     * All inclusive convenience method: register server transports with
     * portmapper, then run the call dispatcher until the server is signalled
     * to shut down, and finally deregister the transports.
     *
     * @throws OncRpcException if the portmapper can not be contacted
     *   successfully.
     * @throws IOException if a severe network I/O error occurs in the
     *   server from which it can not recover (like severe exceptions thrown
     *   when waiting for now connections on a server socket).
     */
    public void run()
           throws OncRpcException, IOException {
        //
        // Ignore all problems during unregistration.
        //
        try {
            unregister(transports);
        } catch ( OncRpcException ore ) {
        }
        register(transports);
        run(transports);
        try {
            unregister(transports);
        } finally {
            close(transports);
        }
    }

    /**
     * Register a set of server transports with the local portmapper.
     *
     * @param transports Array of server transport objects to register,
     *   which will later handle incomming remote procedure call requests.
     *
     * @throws OncRpcException if the portmapper could not be contacted
     *   successfully.
     */
    public void register(OncRpcServerTransport [] transports)
              throws OncRpcException {
        int size = transports.length;
        for ( int idx = 0; idx < size; ++idx ) {
            transports[idx].register();
        }
    }

    /**
     * Process incomming remote procedure call requests from all specified
     * transports. To end processing and to shut the server down signal
     * the {@link #shutdownSignal} object. Note that the thread on which
     * <code>run()</code> is called will ignore any interruptions and
     * will silently swallow them.
     *
     * @param transports Array of server transport objects for which
     *   processing of remote procedure call requests should be done.
     */
    public void run(OncRpcServerTransport [] transports) {
        int size = transports.length;
        for ( int idx = 0; idx < size; ++idx ) {
            transports[idx].listen();
        }
        //
        // Loop and wait for the shutdown flag to become signalled. If the
        // server's main thread gets interrupted it will not shut itself
        // down. It can only be stopped by signalling the shutdownSignal
        // object.
        //
        for ( ;; ) {
            synchronized ( shutdownSignal ) {
                try {
                    shutdownSignal.wait();
                    break;
                } catch ( InterruptedException e ) {
                }
            }
        }
    }

    /**
     * Notify the RPC server to stop processing of remote procedure call
     * requests as soon as possible. Note that each transport has its own
     * thread, so processing will not stop before the transports have been
     * closed by calling the {@link #close} method of the server.
     */
    public void stopRpcProcessing() {
        if ( shutdownSignal != null ) {
            synchronized ( shutdownSignal ) {
                shutdownSignal.notify();
            }
        }
    }

    /**
     * Unregister a set of server transports from the local portmapper.
     *
     * @param transports Array of server transport objects to unregister.
     *
     * @throws OncRpcException with a reason of
     *   {@link OncRpcException#RPC_FAILED OncRpcException.RPC_FAILED} if
     *   the portmapper could not be contacted successfully. Note that
     *   it is not considered an error to remove a non-existing entry from
     *   the portmapper.
     */
    public void unregister(OncRpcServerTransport [] transports)
              throws OncRpcException {
        int size = transports.length;
        for ( int idx = 0; idx < size; ++idx ) {
            transports[idx].unregister();
        }
    }

    /**
     * Close all transports listed in a set of server transports. Only
     * by calling this method processing of remote procedure calls by
     * individual transports can be stopped. This is because every server
     * transport is handled by its own thread.
     *
     * @param transports Array of server transport objects to close.
     */
    public void close(OncRpcServerTransport [] transports) {
        int size = transports.length;
        for ( int idx = 0; idx < size; ++idx ) {
            transports[idx].close();
        }
    }

}
// End of OncRpcServerStub.java