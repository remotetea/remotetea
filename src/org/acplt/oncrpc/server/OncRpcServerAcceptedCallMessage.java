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
 * The <code>OncRpcServerAcceptedCallMessage</code> class represents (on the
 * sender's side) an accepted ONC/RPC call. In ONC/RPC babble, an "accepted"
 * call does not mean that it carries a result from the remote procedure
 * call, but rather that the call was accepted at the basic ONC/RPC level
 * and no authentification failure or else occured.
 *
 * <p>This ONC/RPC reply header class is only a convenience for server
 * implementors.
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public class OncRpcServerAcceptedCallMessage extends OncRpcServerReplyMessage {

    /**
     * Constructs an <code>OncRpcServerAcceptedCallMessage</code> object which
     * represents an accepted call, which was also successfully executed,
     * so the reply will contain information from the remote procedure call.
     *
     * @param call The call message header, which is used to construct the
     *   matching reply message header from.
     */
    public OncRpcServerAcceptedCallMessage(OncRpcServerCallMessage call) {
        super(call,
              OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED,
              OncRpcAcceptStatus.ONCRPC_SUCCESS,
              OncRpcReplyMessage.UNUSED_PARAMETER,
              OncRpcReplyMessage.UNUSED_PARAMETER,
              OncRpcReplyMessage.UNUSED_PARAMETER,
              OncRpcAuthStatus.ONCRPC_AUTH_OK);
    }

    /**
     * Constructs an <code>OncRpcAcceptedCallMessage</code> object which
     * represents an accepted call, which was not necessarily successfully
     * carried out. The parameter <code>acceptStatus</code> will then
     * indicate the exact outcome of the ONC/RPC call.
     *
     * @param call The call message header, which is used to construct the
     *   matching reply message header from.
     * @param acceptStatus The accept status of the call. This can be any
     * one of the constants defined in the {@link OncRpcAcceptStatus}
     * interface.
     */
    public OncRpcServerAcceptedCallMessage(OncRpcServerCallMessage call,
                                           int acceptStatus) {
        super(call,
              OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED,
              acceptStatus,
              OncRpcReplyMessage.UNUSED_PARAMETER,
              OncRpcReplyMessage.UNUSED_PARAMETER,
              OncRpcReplyMessage.UNUSED_PARAMETER,
              OncRpcAuthStatus.ONCRPC_AUTH_OK);
    }

    /**
     * Constructs an <code>OncRpcAcceptedCallMessage</code> object for an
     * accepted call with an unsupported version. The reply will contain
     * information about the lowest and highest supported version.
     *
     * @param call The call message header, which is used to construct the
     *   matching reply message header from.
     * @param low Lowest program version supported by this ONC/RPC server.
     * @param high Highest program version supported by this ONC/RPC server.
     */
    public OncRpcServerAcceptedCallMessage(OncRpcServerCallMessage call,
                                           int low, int high) {
        super(call,
              OncRpcReplyStatus.ONCRPC_MSG_ACCEPTED,
              OncRpcAcceptStatus.ONCRPC_PROG_MISMATCH,
              OncRpcReplyMessage.UNUSED_PARAMETER,
              low,
              high,
              OncRpcAuthStatus.ONCRPC_AUTH_OK);
    }

}

// End of OncRpcServerAcceptedCallMessage.java
