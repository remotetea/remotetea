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
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

/**
 * Instances of class <code>OncRpcTcpServerTransport</code> encapsulate
 * TCP/IP-based XDR streams of ONC/RPC servers. This server transport class
 * is responsible for accepting new ONC/RPC connections over TCP/IP.
 *
 * @see OncRpcServerTransport
 * @see OncRpcTcpConnectionServerTransport
 * @see OncRpcUdpServerTransport
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public class OncRpcTcpServerTransport extends OncRpcServerTransport {

    /**
     * Create a new instance of a <code>OncRpcTcpServerTransport</code> which
     * encapsulates TCP/IP-based XDR streams of an ONC/RPC server. This
     * particular server transport only waits for incoming connection requests
     * and then creates {@link OncRpcTcpConnectionServerTransport} server transports
     * to handle individual connections.
     * This constructor is a convenience constructor for those transports
     * handling only a single ONC/RPC program and version number.
     *
     * @param dispatcher Reference to interface of an object capable of
     *   dispatching (handling) ONC/RPC calls.
     * @param port Number of port where the server will wait for incoming
     *   calls.
     * @param program Number of ONC/RPC program handled by this server
     *   transport.
     * @param version Version number of ONC/RPC program handled.
     * @param bufferSize Size of buffer used when receiving and sending
     *   chunks of XDR fragments over TCP/IP. The fragments built up to
     *   form ONC/RPC call and reply messages.
     */
    public OncRpcTcpServerTransport(OncRpcDispatchable dispatcher,
                                    int port,
                                    int program, int version,
                                    int bufferSize)
           throws OncRpcException, IOException {
        this(dispatcher, port,
             new OncRpcServerTransportRegistrationInfo[] {
                new OncRpcServerTransportRegistrationInfo(program, version)
             },
             bufferSize);
    }

    /**
     * Create a new instance of a <code>OncRpcTcpServerTransport</code> which
     * encapsulates TCP/IP-based XDR streams of an ONC/RPC server. This
     * particular server transport only waits for incoming connection requests
     * and then creates {@link OncRpcTcpConnectionServerTransport} server transports
     * to handle individual connections.
     *
     * @param dispatcher Reference to interface of an object capable of
     *   dispatching (handling) ONC/RPC calls.
     * @param port Number of port where the server will wait for incoming
     *   calls.
     * @param info Array of program and version number tuples of the ONC/RPC
     *   programs and versions handled by this transport.
     * @param bufferSize Size of buffer used when receiving and sending
     *   chunks of XDR fragments over TCP/IP. The fragments built up to
     *   form ONC/RPC call and reply messages.
     */
    public OncRpcTcpServerTransport(OncRpcDispatchable dispatcher,
                                    int port,
                                    OncRpcServerTransportRegistrationInfo [] info,
                                    int bufferSize)
           throws OncRpcException, IOException {
        this(dispatcher, null, port, info, bufferSize);
    }

    /**
     * Create a new instance of a <code>OncRpcTcpServerTransport</code> which
     * encapsulates TCP/IP-based XDR streams of an ONC/RPC server. This
     * particular server transport only waits for incoming connection requests
     * and then creates {@link OncRpcTcpConnectionServerTransport} server transports
     * to handle individual connections.
     *
     * @param dispatcher Reference to interface of an object capable of
     *   dispatching (handling) ONC/RPC calls.
     * @param bindAddr The local Internet Address the server will bind to.
     * @param port Number of port where the server will wait for incoming
     *   calls.
     * @param info Array of program and version number tuples of the ONC/RPC
     *   programs and versions handled by this transport.
     * @param bufferSize Size of buffer used when receiving and sending
     *   chunks of XDR fragments over TCP/IP. The fragments built up to
     *   form ONC/RPC call and reply messages.
     */
    public OncRpcTcpServerTransport(OncRpcDispatchable dispatcher,
                                    InetAddress bindAddr,
                                    int port,
                                    OncRpcServerTransportRegistrationInfo [] info,
                                    int bufferSize)
           throws OncRpcException, IOException {
        super(dispatcher, port, info);
        //
        // Make sure the buffer is large enough and resize system buffers
        // accordingly, if possible.
        //
        if ( bufferSize < 1024 ) {
            bufferSize = 1024;
        }
        this.bufferSize = bufferSize;
        socket = new ServerSocket(port, 0, bindAddr);
        if ( port == 0 ) {
            this.port = socket.getLocalPort();
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
     * <p>Calling this method on a <code>OncRpcTcpServerTransport</code>
     * results in the listening TCP network socket immediately being closed.
     * In addition, all server transports handling the individual TCP/IP
     * connections will also be closed. The handler threads will therefore
     * either terminate directly or when they try to sent back replies.
     */
    public void close() {
        if ( socket != null ) {
            //
            // Since there is a non-zero chance of getting race conditions,
            // we now first set the socket instance member to null, before
            // we close the corresponding socket. This avoids null-pointer
            // exceptions in the method which waits for connections: it is
            // possible that that method is awakened because the socket has
            // been closed before we could set the socket instance member to
            // null. Many thanks to Michael Smith for tracking down this one.
            //
            ServerSocket deadSocket = socket;
            socket = null;
            try {
                deadSocket.close();
            } catch ( IOException e ) {
            }
        }
        //
        // Now close all per-connection transports currently open...
        //
        synchronized ( openTransports ) {
            while ( openTransports.size() > 0 ) {
                OncRpcTcpConnectionServerTransport transport =
                    (OncRpcTcpConnectionServerTransport)
                        openTransports.removeFirst();
                transport.close();
            }
        }
    }

    /**
     * Removes a TCP/IP server transport from the list of currently open
     * transports.
     *
     * @param transport Server transport to remove from the list of currently
     *   open transports for this listening transport.
     */
    protected void removeTransport(OncRpcTcpConnectionServerTransport transport) {
        synchronized ( openTransports ) {
            openTransports.remove((Object)transport);
        }
    }

    /**
     * Register the TCP/IP port where this server transport waits for incoming
     * requests with the ONC/RPC portmapper.
     *
     * @throws OncRpcException if the portmapper could not be contacted
     *   successfully.
     */
    public void register()
           throws OncRpcException {
        try {
            OncRpcPortmapClient portmapper =
                new OncRpcPortmapClient(InetAddress.getByName("127.0.0.1"));
            int size = info.length;
            for ( int idx = 0; idx < size; ++idx ) {
                portmapper.setPort(info[idx].program, info[idx].version,
                                   OncRpcProtocols.ONCRPC_TCP, port);
            }
        } catch ( IOException e ) {
            throw(new OncRpcException(OncRpcException.RPC_FAILED));
        }
    }

    /**
     * Do not call.
     *
     * @throws Error because this method must not be called for a listening
     * server transport.
     */
    public void retrieveCall(XdrAble call)
           throws OncRpcException, IOException {
        throw(new Error("OncRpcTcpServerTransport.retrieveCall() is abstract "
                       +"and can not be called."));
    }

   /**
     * Do not call.
     *
     * @throws Error because this method must not be called for a listening
     * server transport.
     */
    protected XdrDecodingStream getXdrDecodingStream() {
        throw(new Error("OncRpcTcpServerTransport.getXdrDecodingStream() is abstract "
                       +"and can not be called."));
    }

    /**
     * Do not call.
     *
     * @throws Error because this method must not be called for a listening
     * server transport.
     */
    protected void endDecoding()
              throws OncRpcException, IOException {
        throw(new Error("OncRpcTcpServerTransport.endDecoding() is abstract "
                       +"and can not be called."));
    }

    /**
     * Do not call.
     *
     * @throws Error because this method must not be called for a listening
     * server transport.
     */
    protected XdrEncodingStream getXdrEncodingStream() {
        throw(new Error("OncRpcTcpServerTransport.getXdrEncodingStream() is abstract "
                       +"and can not be called."));
    }

    /**
     * Do not call.
     *
     * @throws Error because this method must not be called for a listening
     * server transport.
     */
    protected void beginEncoding(OncRpcCallInformation callInfo,
                                 OncRpcServerReplyMessage state)
              throws OncRpcException, IOException {
        throw(new Error("OncRpcTcpServerTransport.beginEncoding() is abstract "
                       +"and can not be called."));
    }

    /**
     * Do not call.
     *
     * @throws Error because this method must not be called for a listening
     * server transport.
     */
    protected void endEncoding()
              throws OncRpcException, IOException {
        throw(new Error("OncRpcTcpServerTransport.endEncoding() is abstract "
                       +"and can not be called."));
    }

    /**
     * Do not call.
     *
     * @throws Error because this method must not be called for a listening
     * server transport.
     */
    protected void reply(OncRpcCallInformation callInfo,
                         OncRpcServerReplyMessage state, XdrAble reply)
           throws OncRpcException, IOException {
        throw(new Error("OncRpcTcpServerTransport.reply() is abstract "
                       +"and can not be called."));
    }

    /**
     * Creates a new thread and uses this thread to listen to incoming
     * ONC/RPC requests, then dispatches them and finally sends back the
     * appropriate reply messages. Control in the calling thread immediately
     * returns after the handler thread has been created.
     *
     * <p>For every incomming TCP/IP connection a handler thread is created
     * to handle ONC/RPC calls on this particular connection.
     */
    public void listen() {
        //
        // Create a new (daemon) thread which will handle incoming connection
        // requests.
        //
        Thread listenThread = new Thread("TCP server transport listener thread") {
            public void run() {
                for ( ;; ) {
                    try {
                        //
                        // Now wait for (new) connection requests to come in.
                        //
                        ServerSocket myServerSocket = socket;
                        if ( myServerSocket == null ) {
                            break;
                        }
                        Socket newSocket = myServerSocket.accept();
                        OncRpcTcpConnectionServerTransport transport =
                            new OncRpcTcpConnectionServerTransport(
                                dispatcher,
                                newSocket,
                                info,
                                bufferSize,
                                OncRpcTcpServerTransport.this,
                                transmissionTimeout);
                        synchronized ( openTransports ) {
                            openTransports.add((Object)transport);
                        }
                        //
                        // Let the newly created transport object handle this
                        // connection. Note that it will create its own
                        // thread for handling.
                        //
                        transport.listen();
                    } catch ( OncRpcException e ) {
                    } catch ( IOException e ) {
                        //
                        // We are just ignoring most of the IOExceptions as
                        // they might be thrown, for instance, if a client
                        // attempts a connection and resets it before it is
                        // pulled off by accept(). If the socket has been
                        // gone away after an IOException this means that the
                        // transport has been closed, so we end this thread
                        // gracefully.
                        //
                        if ( socket == null ) {
                            break;
                        }
                    }
                }
            }
        };
        //
        // Now make the new handling thread a deamon and start it, so it
        // sits there waiting for incoming TCP/IP connection requests.
        //
        listenThread.setDaemon(true);
        listenThread.start();
    }

    /**
     * Set the timeout used during transmission of data. If the flow of data
     * when sending calls or receiving replies blocks longer than the given
     * timeout, an exception is thrown. The timeout must be > 0.
     *
     * @param milliseconds Transmission timeout in milliseconds.
     */
    public void setTransmissionTimeout(int milliseconds) {
         if ( milliseconds <= 0 ) {
            throw(new IllegalArgumentException("transmission timeout must be > 0"));
        }
       transmissionTimeout = milliseconds;
    }

    /**
     * Retrieve the current timeout used during transmission phases (call and
     * reply phases).
     *
     * @return Current transmission timeout.
     */
    public int getTransmissionTimeout()
    {
        return transmissionTimeout;
    }

    /**
     * TCP socket used for stream-based communication with ONC/RPC
     * clients.
     */
    private ServerSocket socket;

    /**
     * Size of send/receive buffers to use when encoding/decoding XDR data.
     */
    private int bufferSize;

    /**
     * Collection containing currently open transports.
     */
    private TransportList openTransports = new TransportList();

    /**
     * Timeout during the phase where data is received within calls, or data is
     * sent within replies.
     */
    protected int transmissionTimeout = 30000;


    /**
     * Minumum implementation of a double linked list which notices which
     * transports are currently open and have to be shut down when this
     * listening transport is shut down. The only reason why we have this
     * code here instead of using java.util.LinkedList is due to JDK&nbsp;1.1
     * compatibility.
     *
     * <p>Note that the methods are not synchronized as we leave this up
     * to the caller, who can thus optimize access during critical sections.
     */
    private class TransportList {

        /**
         * Create a new instance of a list of open transports.
         */
        public TransportList() {
            //
            // Link header node with itself, so it is its own successor
            // and predecessor. Using a header node excuses us from checking
            // for the special cases of first and last node (or both at
            // the same time).
            //
            head.next = head;
            head.prev = head;
        }

        /**
         * Add new transport to list of open transports. The new transport
         * is always added immediately after the head of the linked list.
         */
        public void add(Object o) {
            Node node = new Node(o);
            node.next = head.next;
            head.next = node;
            node.prev = head;
            node.next.prev = node;
            ++size;
        }

        /**
         * Remove given transport from list of open transports.
         */
        public boolean remove(Object o) {
            Node node = head.next;
            while ( node != head ) {
                if ( node.item == o ) {
                    node.prev.next = node.next;
                    node.next.prev = node.prev;
                    --size;
                    return true;
                }
                node = node.next;
            }
            return false;
        }

        /**
         * Removes and returns the first open transport from list.
         */
        public Object removeFirst() {
            //
            // Do not remove the header node.
            //
            if ( size == 0 ) {
                throw(new java.util.NoSuchElementException());
            }
            Node node = head.next;
            head.next = node.next;
            node.next.prev = head;
            --size;
            return node.item;
        }

        /**
         * Returns the number of (open) transports in this list.
         *
         * @return the number of (open) transports.
         */
        public int size() {
            return size;
        }

        /**
         * Head node for list of open transports which does not represent
         * an open transport but instead excuses us of dealing with all
         * the special cases of real nodes at the begin or end of the list.
         */
        private Node head = new Node(null);

        /**
         * Number of (real) open transports currently registered in this
         * list.
         */
        private int size = 0;


        /**
         * Node class referencing an individual open transport and holding
         * references to the previous and next open transports.
         */
        private class Node {

            /**
             * Create a new instance of a node object and let it reference
             * an open transport. The creator of this object is then
             * responsible for adding this node to the circular list itself.
             */
            public Node(Object item) {
                this.item = item;
            }

            /**
             * Next item node (in other words: next open transport)
             * in the list. This will never be <code>null</code> for the
             * first item, but instead reference the last item. Thus, the
             * list is circular.
             */
            Node next;

            /**
             * Previous item node (in other words: previous open transport)
             * in the list. This will never be <code>null</code> for the
             * last item, but instead reference the first item. Thus, the
             * list is circular.
             */
            Node prev;

            /**
             * The item/object placed at this position in the list. This
             * currently always references an open transport.
             */
            Object item;

        }

    }

}

// End of OncRpcTcpServerTransport.java
