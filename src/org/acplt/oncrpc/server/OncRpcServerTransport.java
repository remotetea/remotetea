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
import java.net.InetAddress;

/**
 * Instances of class <code>OncRpcServerTransport</code> encapsulate XDR
 * streams of ONC/RPC servers. Using server transports, ONC/RPC calls are
 * received and the corresponding replies are later sent back after
 * handling.
 *
 * <p>Note that the server-specific dispatcher handling requests
 * (done through {@link OncRpcDispatchable} will only
 * directly deal with {@link OncRpcCallInformation} objects. These
 * call information objects reference OncRpcServerTransport object, but
 * the server programmer typically will never touch them, as the call
 * information object already contains all necessary information about
 * a call, so replies can be sent back (and this is definetely a sentence
 * containing too many words).
 *
 * @see OncRpcCallInformation
 * @see OncRpcDispatchable
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public abstract class OncRpcServerTransport {

    /**
     * Create a new instance of a <code>OncRpcServerTransport</code> which
     * encapsulates XDR streams of an ONC/RPC server. Using a server transport,
     * ONC/RPC calls are received and the corresponding replies are sent back.
     *
     * <p>We do not create any XDR streams here, as it is the responsibility
     * of derived classes to create appropriate XDR stream objects for the
     * respective kind of transport mechanism used (like TCP/IP and UDP/IP).
     *
     * @param dispatcher Reference to interface of an object capable of
     *   dispatching (handling) ONC/RPC calls.
     * @param port Number of port where the server will wait for incoming
     *   calls.
     * @param info Array of program and version number tuples of the ONC/RPC
     *   programs and versions handled by this transport.
     */
    protected OncRpcServerTransport(OncRpcDispatchable dispatcher, int port,
                                    OncRpcServerTransportRegistrationInfo [] info) {
        this.dispatcher = dispatcher;
        this.port = port;
        this.info = info;
    }

    /**
     * Register the port where this server transport waits for incoming
     * requests with the ONC/RPC portmapper.
     *
     * <p>The contract of this method is, that derived classes implement
     * the appropriate communication with the portmapper, so the transport
     * is registered only for the protocol supported by a particular kind
     * of server transport.
     *
     * @throws OncRpcException if the portmapper could not be contacted
     *   successfully.
     */
    public abstract void register()
           throws OncRpcException;

    /**
     * Unregisters the port where this server transport waits for incoming
     * requests from the ONC/RPC portmapper.
     *
     * <p>Note that due to the way Sun decided to implement its ONC/RPC
     * portmapper process, deregistering one server transports causes all
     * entries for the same program and version to be removed, regardless
     * of the protocol (UDP/IP or TCP/IP) used. Sigh.
     *
     * @throws OncRpcException with a reason of
     *   {@link OncRpcException#RPC_FAILED OncRpcException.RPC_FAILED} if
     *   the portmapper could not be contacted successfully. Note that
     *   it is not considered an error to remove a non-existing entry from
     *   the portmapper.
     */
    public void unregister()
           throws OncRpcException {
        try {
            OncRpcPortmapClient portmapper =
                new OncRpcPortmapClient(InetAddress.getByName("127.0.0.1"));
            int size = info.length;
            for ( int idx = 0; idx < size; ++idx ) {
                portmapper.unsetPort(info[idx].program, info[idx].version);
            }
        } catch ( IOException e ) {
            throw(new OncRpcException(OncRpcException.RPC_FAILED));
        }
    }

    /**
     * Close the server transport and free any resources associated with it.
     *
     * <p>Note that the server transport is <b>not deregistered</b>. You'll
     * have to do it manually if you need to do so. The reason for this
     * behaviour is, that the portmapper removes all entries regardless of
     * the protocol (TCP/IP or UDP/IP) for a given ONC/RPC program number
     * and version.
     *
     * <p>Derived classes can choose between different behaviour for
     * shuting down the associated transport handler threads:
     * <ul>
     * <li>Close the transport immediately and let the threads stumble on the
     *   closed network connection.
     * <li>Wait for handler threads to complete their current ONC/RPC request
     *   (with timeout), then close connections and kill the threads.
     * </ul>
     */
    public abstract void close();

    /**
     * Creates a new thread and uses this thread to listen to incoming
     * ONC/RPC requests, then dispatches them and finally sends back the
     * appropriate reply messages.
     *
     * <p>Note that you have to supply an implementation for this abstract
     * method in derived classes. Your implementation needs to create a new
     * thread to wait for incoming requests. The method has to return
     * immediately for the calling thread.
     */
    public abstract void listen();

    /**
     * Returns port number of socket this server transport listens on for
     * incoming ONC/RPC calls.
     *
     * @result Port number of socket listening for incoming calls.
     */
    public int getPort() {
        return port;
    }

    /**
     * Retrieves the parameters sent within an ONC/RPC call message. It also
     * makes sure that the deserialization process is properly finished after
     * the call parameters have been retrieved. Under the hood this method
     * therefore calls {@link XdrDecodingStream#endDecoding} to free any
     * pending resources from the decoding stage.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully deserialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    protected abstract void retrieveCall(XdrAble call)
           throws OncRpcException, IOException;

    /**
     * Returns XDR stream which can be used for deserializing the parameters
     * of this ONC/RPC call. This method belongs to the lower-level access
     * pattern when handling ONC/RPC calls.
     *
     * @result Reference to decoding XDR stream.
     */
    protected abstract XdrDecodingStream getXdrDecodingStream();

    /**
     * Finishes call parameter deserialization. Afterwards the XDR stream
     * returned by {@link #getXdrDecodingStream} must not be used any more.
     * This method belongs to the lower-level access pattern when handling
     * ONC/RPC calls.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully deserialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    protected abstract void endDecoding()
              throws OncRpcException, IOException;

    /**
     * Returns XDR stream which can be used for eserializing the reply
     * to this ONC/RPC call. This method belongs to the lower-level access
     * pattern when handling ONC/RPC calls.
     *
     * @result Reference to enecoding XDR stream.
     */
    protected abstract XdrEncodingStream getXdrEncodingStream();

    /**
     * Begins the sending phase for ONC/RPC replies.
     * This method belongs to the lower-level access pattern when handling
     * ONC/RPC calls.
     *
     * @param callInfo Information about ONC/RPC call for which we are about
     *   to send back the reply.
     * @param state ONC/RPC reply header indicating success or failure.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     */
    protected abstract void beginEncoding(OncRpcCallInformation callInfo,
                                          OncRpcServerReplyMessage state)
              throws OncRpcException, IOException;

    /**
     * Finishes encoding the reply to this ONC/RPC call. Afterwards you must
     * not use the XDR stream returned by {@link #getXdrEncodingStream} any
     * longer.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    protected abstract void endEncoding()
              throws OncRpcException, IOException;

    /**
     * Send back an ONC/RPC reply to the original caller. This is rather a
     * low-level method, typically not used by applications. Dispatcher handling
     * ONC/RPC calls have to use the
     * {@link OncRpcCallInformation#reply(XdrAble)} method instead on the
     * call object supplied to the handler.
     *
     * <p>An appropriate implementation has to be provided in derived classes
     * as it is dependent on the type of transport (whether UDP/IP or TCP/IP)
     * used.
     *
     * @param callInfo information about the original call, which are necessary
     *   to send back the reply to the appropriate caller.
     * @param state ONC/RPC reply message header indicating success or failure
     *   and containing associated state information.
     * @param reply If not <code>null</code>, then this parameter references
     *   the reply to be serialized after the reply message header.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     *
     * @see OncRpcCallInformation
     * @see OncRpcDispatchable
     */
    protected abstract void reply(OncRpcCallInformation callInfo,
                                  OncRpcServerReplyMessage state, XdrAble reply)
           throws OncRpcException, IOException;

    /**
     * Reference to interface of an object capable of handling/dispatching
     * ONC/RPC requests.
     */
    protected OncRpcDispatchable dispatcher;

    /**
     * Port number where we're listening for incoming ONC/RPC requests.
     */
    protected int port;

    /**
     * Program and version number tuples handled by this server transport.
     */
    protected OncRpcServerTransportRegistrationInfo [] info;

}

// End of OncRpcServerTransport.java

