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
 * Objects of class <code>OncRpcCallInformation</code> contain information
 * about individual ONC/RPC calls. They are given to ONC/RPC
 * {@link OncRpcDispatchable call dispatchers},
 * so they can send back the reply to the appropriate caller, etc. Use only
 * this call info objects to retrieve call parameters and send back replies
 * as in the future UDP/IP-based transports may become multi-threaded handling.
 * The call info object is responsible to control access to the underlaying
 * transport, so never mess with the transport directly.
 *
 * <p>Note that this class provides two different patterns for accessing
 * parameters sent by clients within the ONC/RPC call and sending back replies.
 *
 * <ol>
 * <li>The convenient high-level access:
 *   <ul>
 *   <li>Use {@link #retrieveCall(XdrAble)} to retrieve the parameters of
 *     the call and deserialize it into a paramter object.
 *   <li>Use {@link #reply(XdrAble)} to send back the reply by serializing
 *     a reply/result object. Or use the <code>failXXX</code> methods to send back
 *     an error indication instead.
 *   </ul>
 *
 * <li>The lower-level access, giving more control over how and when data
 *   is deserialized and serialized:
 *   <ul>
 *   <li>Use {@link #getXdrDecodingStream} to get a reference to the XDR
 *     stream from which you can deserialize the call's parameter.
 *   <li>When you are finished deserializing, call {@link #endDecoding}.
 *   <li>To send back the reply/result, call
 *     {@link #beginEncoding(OncRpcServerReplyMessage)}. Using the XDR stream returned
 *     by {@link #getXdrEncodingStream} serialize the reply/result. Finally finish
 *     the serializing step by calling {@link #endEncoding}.
 *   </ul>
 * </ol>
 *
 * @see OncRpcDispatchable
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public class OncRpcCallInformation {

    /**
     * Create an <code>OncRpcCallInformation</code> object and associate it
     * with a ONC/RPC server transport. Typically,
     * <code>OncRpcCallInformation</code> objects are created by transports
     * once before handling incoming calls using the same call info object.
     * To support multithreaded handling of calls in the future (for UDP/IP),
     * the transport is already divided from the call info.
     *
     * @param transport ONC/RPC server transport.
     */
    protected OncRpcCallInformation(OncRpcServerTransport transport) {
        this.transport = transport;
    }

    /**
     * Contains the call message header from ONC/RPC identifying this
     * particular call.
     */
    public OncRpcServerCallMessage callMessage = new OncRpcServerCallMessage();

    /**
     * Internet address of the peer from which we received an ONC/RPC call
     * or whom we intend to call.
     */
    public InetAddress peerAddress = null;

    /**
     * Port number of the peer from which we received an ONC/RPC call or
     * whom we intend to call.
     */
    public int peerPort = 0;

    /**
     * Associated transport from which we receive the ONC/RPC call parameters
     * and to which we serialize the ONC/RPC reply. Never mess with this
     * member or you might break all future extensions horribly -- but this
     * warning probably only stimulates you...
     */
    protected OncRpcServerTransport transport;

    /**
     * Retrieves the parameters sent within an ONC/RPC call message. It also
     * makes sure that the deserialization process is properly finished after
     * the call parameters have been retrieved.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully deserialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void retrieveCall(XdrAble call)
           throws OncRpcException, IOException {
        transport.retrieveCall(call);
    }

    /**
     * Returns XDR stream which can be used for deserializing the parameters
     * of this ONC/RPC call. This method belongs to the lower-level access
     * pattern when handling ONC/RPC calls.
     *
     * @return Reference to decoding XDR stream.
     */
    public XdrDecodingStream getXdrDecodingStream() {
        return transport.getXdrDecodingStream();
    }

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
    public void endDecoding()
           throws OncRpcException, IOException {
        transport.endDecoding();
    }

    /**
     * Begins the sending phase for ONC/RPC replies. After beginning sending
     * you can serialize the reply/result (but only if the call was accepted, see
     * {@link org.acplt.oncrpc.OncRpcReplyMessage} for details). The stream
     * to use for serialization can be obtained using
     * {@link #getXdrEncodingStream}.
     * This method belongs to the lower-level access pattern when handling
     * ONC/RPC calls.
     *
     * @param state ONC/RPC reply header indicating success or failure.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void beginEncoding(OncRpcServerReplyMessage state)
           throws OncRpcException, IOException {
        transport.beginEncoding(this, state);
    }

    /**
     * Begins the sending phase for accepted ONC/RPC replies. After beginning
     * sending you can serialize the result/reply. The stream
     * to use for serialization can be obtained using
     * {@link #getXdrEncodingStream}.
     * This method belongs to the lower-level access pattern when handling
     * ONC/RPC calls.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void beginEncoding()
           throws OncRpcException, IOException {
        transport.beginEncoding(
            this,
            new OncRpcServerReplyMessage(
                callMessage,
                OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED,
                OncRpcAcceptStatus.ONCRPC_SUCCESS,
                OncRpcReplyMessage.UNUSED_PARAMETER,
                OncRpcReplyMessage.UNUSED_PARAMETER,
                OncRpcReplyMessage.UNUSED_PARAMETER,
                OncRpcReplyMessage.UNUSED_PARAMETER));
    }

    /**
     * Returns XDR stream which can be used for eserializing the reply
     * to this ONC/RPC call. This method belongs to the lower-level access
     * pattern when handling ONC/RPC calls.
     *
     * @return Reference to enecoding XDR stream.
     */
    public XdrEncodingStream getXdrEncodingStream() {
        return transport.getXdrEncodingStream();
    }

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
    public void endEncoding()
           throws OncRpcException, IOException {
        transport.endEncoding();
    }

    /**
     * Send back an ONC/RPC reply to the caller who sent in this call. This is
     * a low-level function and typically should not be used by call
     * dispatchers. Instead use the other {@link #reply(XdrAble) reply method}
     * which just expects a serializable object to send back to the caller.
     *
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
     * @see OncRpcReplyMessage
     * @see OncRpcDispatchable
     */
    public void reply(OncRpcServerReplyMessage state, XdrAble reply)
           throws OncRpcException, IOException {
        transport.reply(this, state, reply);
    }

    /**
     * Send back an ONC/RPC reply to the caller who sent in this call. This
     * automatically sends an ONC/RPC reply header before the reply part,
     * indicating success within the header.
     *
     * @param reply Reply body the ONC/RPC reply message.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void reply(XdrAble reply)
           throws OncRpcException, IOException {
        reply(new OncRpcServerReplyMessage(callMessage,
                                           OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED,
                                           OncRpcAcceptStatus.ONCRPC_SUCCESS,
                                           OncRpcReplyMessage.UNUSED_PARAMETER,
                                           OncRpcReplyMessage.UNUSED_PARAMETER,
                                           OncRpcReplyMessage.UNUSED_PARAMETER,
                                           OncRpcReplyMessage.UNUSED_PARAMETER),
              reply);
    }

    /**
     * Send back an ONC/RPC failure indication about invalid arguments to the
     * caller who sent in this call.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void failArgumentGarbage()
           throws OncRpcException, IOException {
        reply(new OncRpcServerReplyMessage(
                      callMessage,
                      OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED,
                      OncRpcAcceptStatus.ONCRPC_GARBAGE_ARGS,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER),
              null);
    }

    /**
     * Send back an ONC/RPC failure indication about an unavailable procedure
     * call to the caller who sent in this call.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void failProcedureUnavailable()
           throws OncRpcException, IOException {
        reply(new OncRpcServerReplyMessage(callMessage,
                      OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED,
                      OncRpcAcceptStatus.ONCRPC_PROC_UNAVAIL,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER),
              null);
    }

    /**
     * Send back an ONC/RPC failure indication about an unavailable program
     * to the caller who sent in this call.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void failProgramUnavailable()
           throws OncRpcException, IOException {
        reply(new OncRpcServerReplyMessage(callMessage,
                      OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED,
                      OncRpcAcceptStatus.ONCRPC_PROG_UNAVAIL,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER),
              null);
    }

    /**
     * Send back an ONC/RPC failure indication about a program version mismatch
     * to the caller who sent in this call.
     *
     * @param lowVersion lowest supported program version.
     * @param highVersion highest supported program version.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void failProgramMismatch(int lowVersion, int highVersion)
           throws OncRpcException, IOException {
        reply(new OncRpcServerReplyMessage(callMessage,
                      OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED,
                      OncRpcAcceptStatus.ONCRPC_PROG_MISMATCH,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      lowVersion,
                      highVersion,
                      OncRpcReplyMessage.UNUSED_PARAMETER),
              null);
    }

    /**
     * Send back an ONC/RPC failure indication about a system error
     * to the caller who sent in this call.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void failSystemError()
           throws OncRpcException, IOException {
        reply(new OncRpcServerReplyMessage(callMessage,
                      OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED,
                      OncRpcAcceptStatus.ONCRPC_SYSTEM_ERR,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER),
              null);
    }

    /**
     * Send back an ONC/RPC failure indication about a ONC/RPC version mismatch
     * call to the caller who sent in this call.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void failOncRpcVersionMismatch()
           throws OncRpcException, IOException {
        reply(new OncRpcServerReplyMessage(callMessage,
                      OncRpcReplyStatus.ONCRPC_MSG_DENIED,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcRejectStatus.ONCRPC_RPC_MISMATCH,
                      OncRpcCallMessage.ONCRPC_VERSION,
                      OncRpcCallMessage.ONCRPC_VERSION,
                      OncRpcReplyMessage.UNUSED_PARAMETER),
              null);
    }

    /**
     * Send back an ONC/RPC failure indication about a failed authentication
     * to the caller who sent in this call.
     *
     * @param authStatus {@link OncRpcAuthStatus Reason} why authentication
     *   failed.
     *
     * @throws OncRpcException if an ONC/RPC exception occurs, like the data
     *   could not be successfully serialized.
     * @throws IOException if an I/O exception occurs, like transmission
     *   failures over the network, etc.
     */
    public void failAuthenticationFailed(int authStatus)
           throws OncRpcException, IOException {
        reply(new OncRpcServerReplyMessage(callMessage,
                      OncRpcReplyStatus.ONCRPC_MSG_DENIED,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcRejectStatus.ONCRPC_AUTH_ERROR,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      OncRpcReplyMessage.UNUSED_PARAMETER,
                      authStatus),
              null);
    }

}

// End of OncRpcCallInformation.java
