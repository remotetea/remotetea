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
 * The <code>OncRpcReplyMessage</code> class represents an ONC/RPC reply
 * message as defined by ONC/RPC in RFC 1831. Such messages are sent back by
 * ONC/RPC to servers to clients and contain (in case of real success) the
 * result of a remote procedure call.
 *
 * <p>This class and all its derived classes can be encoded only. They are
 * not able to encode themselves, because they are used solely on the
 * server side of an ONC/RPC connection.
 *
 * <p>The decision to define only one single class for the accepted and
 * rejected replies was driven by the motivation not to use polymorphism
 * and thus have to upcast and downcast references all the time.
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public class OncRpcServerReplyMessage extends OncRpcReplyMessage {

    /**
     * Initializes a new <code>OncRpcReplyMessage</code> object and initializes
     * its complete state from the given parameters.
     *
     * <p>Note that depending on the reply, acceptance and rejectance status
     * some parameters are unused and can be specified as
     * <code>UNUSED_PARAMETER</code>.
     *
     * @param call The ONC/RPC call this reply message corresponds to.
     * @param replyStatus The reply status (see {@link OncRpcReplyStatus}).
     * @param acceptStatus The acceptance state (see {@link OncRpcAcceptStatus}).
     * @param rejectStatus The rejectance state (see {@link OncRpcRejectStatus}).
     * @param lowVersion lowest supported version.
     * @param highVersion highest supported version.
     * @param authStatus The autentication state (see {@link OncRpcAuthStatus}).
     */
    public OncRpcServerReplyMessage(OncRpcServerCallMessage call,
                                    int replyStatus,
                                    int acceptStatus, int rejectStatus,
                                    int lowVersion, int highVersion,
                                    int authStatus) {
        super(call, replyStatus, acceptStatus, rejectStatus,
              lowVersion, highVersion, authStatus);
        this.auth = call.auth;
    }

    /**
     * Encodes -- that is: serializes -- a ONC/RPC reply header object
     * into a XDR stream.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        xdr.xdrEncodeInt(messageId);
        xdr.xdrEncodeInt(messageType);
        xdr.xdrEncodeInt(replyStatus);
        switch ( replyStatus ) {
        case OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED:
            //
            // Encode the information returned for accepted message calls.
            //
            // First encode the authentification data. If someone has
            // nulled (nuked?) the authentication protocol handling object
            // from the call information object, then we can still fall back
            // to sending AUTH_NONE replies...
            //
            if ( auth != null ) {
                auth.xdrEncodeVerf(xdr);
            } else {
                xdr.xdrEncodeInt(OncRpcAuthType.ONCRPC_AUTH_NONE);
                xdr.xdrEncodeInt(0);
            }
            //
            // Even if the call was accepted by the server, it can still
            // indicate an error. Depending on the status of the accepted
            // call we have to send back an indication about the range of
            // versions we support of a particular program (server).
            //
            xdr.xdrEncodeInt(acceptStatus);
            switch ( acceptStatus ) {
            case OncRpcAcceptStatus.ONCRPC_PROG_MISMATCH:
                xdr.xdrEncodeInt(lowVersion);
                xdr.xdrEncodeInt(highVersion);
                break;
            default:
                //
                // Otherwise "open ended set of problem", like the author
                // of Sun's ONC/RPC source once wrote...
                //
                break;
            }
            break;

        case OncRpcReplyStatus.ONCRPC_MSG_DENIED:
            //
            // Encode the information returned for denied message calls.
            //
            xdr.xdrEncodeInt(rejectStatus);
            switch ( rejectStatus ) {
            case OncRpcRejectStatus.ONCRPC_RPC_MISMATCH:
                xdr.xdrEncodeInt(lowVersion);
                xdr.xdrEncodeInt(highVersion);
                break;
            case OncRpcRejectStatus.ONCRPC_AUTH_ERROR:
                xdr.xdrEncodeInt(authStatus);
                break;
            default:
            }
            break;
        }
    }

    /**
     * Contains the authentication protocol handling object.
     */
    OncRpcServerAuth auth;

}

// End of OncRpcServerReplyMessage.java
