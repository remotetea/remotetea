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
 * The <code>OncRpcServerAuthNone</code> class handles all protocol issues
 * of the ONC/RPC authentication <code>AUTH_NONE</code> on the server
 * side.
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public final class OncRpcServerAuthNone extends OncRpcServerAuth {

    /**
     * Returns the type (flavor) of {@link OncRpcAuthType authentication}
     * used.
     *
     * @return Authentication type used by this authentication object.
     */
    public final int getAuthenticationType() {
        return OncRpcAuthType.ONCRPC_AUTH_NONE;
    }

    /**
     * Decodes -- that is: deserializes -- an ONC/RPC authentication object
     * (credential & verifier) on the server side.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    public final void xdrDecodeCredVerf(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        //
        // As the authentication type has already been pulled off the XDR
        // stream, we only need to make sure that really no opaque data follows.
        //
        if ( xdr.xdrDecodeInt() != 0 ) {
            throw(new OncRpcAuthenticationException(
                OncRpcAuthStatus.ONCRPC_AUTH_BADCRED));
        }
        //
        // We also need to decode the verifier. This must be of type
        // AUTH_NONE too. For some obscure historical reasons, we have to
        // deal with credentials and verifiers, although they belong together,
        // according to Sun's specification.
        //
        if ( (xdr.xdrDecodeInt() != OncRpcAuthType.ONCRPC_AUTH_NONE) ||
             (xdr.xdrDecodeInt() != 0) ) {
            throw(new OncRpcAuthenticationException(
                OncRpcAuthStatus.ONCRPC_AUTH_BADVERF));
        }
    }

    /**
     * Encodes -- that is: serializes -- an ONC/RPC authentication object
     * (its verifier) on the server side.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    public final void xdrEncodeVerf(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        //
        // Encode an AUTH_NONE verifier with zero length.
        //
        xdr.xdrEncodeInt(OncRpcAuthType.ONCRPC_AUTH_NONE);
        xdr.xdrEncodeInt(0);
    }

    /**
     * Singleton to use when an authentication object for <code>AUTH_NONE</code>
     * is needed.
     */
    public static final OncRpcServerAuthNone AUTH_NONE = new OncRpcServerAuthNone();

}

// End of OncRpcServerAuthNone.java
