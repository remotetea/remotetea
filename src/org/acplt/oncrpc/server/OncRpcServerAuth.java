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
 * The <code>OncRpcServerAuth</code> class is the base class and factory
 * for handling all protocol issues of ONC/RPC authentication on the server
 * side.
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public abstract class OncRpcServerAuth {

    /**
     * Returns the type (flavor) of {@link OncRpcAuthType authentication}
     * used.
     *
     * @return Authentication type used by this authentication object.
     */
    public abstract int getAuthenticationType();

    /**
     * Restores (deserializes) an authentication object from an XDR stream.
     *
     * @param xdr XDR stream from which the authentication object is
     *   restored.
     * @param recycle old authtentication object which is intended to be
     *   reused in case it is of the same authentication type as the new
     *   one just arriving from the XDR stream.
     *
     * @return Authentication information encapsulated in an object, whose class
     *   is derived from <code>OncRpcServerAuth</code>.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    public static final OncRpcServerAuth xdrNew(XdrDecodingStream xdr,
                                                OncRpcServerAuth recycle)
           throws OncRpcException, IOException {
        OncRpcServerAuth auth;
        //
        // In case we got an old authentication object and we are just about
        // to receive an authentication with the same type, we reuse the old
        // object.
        //
        int authType = xdr.xdrDecodeInt();
        if ( (recycle != null)
             && (recycle.getAuthenticationType() == authType) ) {
            //
            // Simply recycle authentication object and pull its new state
            // of the XDR stream.
            //
            auth = recycle;
            auth.xdrDecodeCredVerf(xdr);
        } else {
            //
            // Create a new authentication object and pull its state off
            // the XDR stream.
            //
            switch ( authType ) {
            case OncRpcAuthType.ONCRPC_AUTH_NONE:
                auth = OncRpcServerAuthNone.AUTH_NONE;
                auth.xdrDecodeCredVerf(xdr);
                break;
            case OncRpcAuthType.ONCRPC_AUTH_SHORT:
                auth = new OncRpcServerAuthShort(xdr);
                break;
            case OncRpcAuthType.ONCRPC_AUTH_UNIX:
                auth = new OncRpcServerAuthUnix(xdr);
                break;
            default:
                //
                // In case of an unknown or unsupported type, throw an exception.
                // Note: using AUTH_REJECTEDCRED is in sync with the way Sun's
                // ONC/RPC implementation does it. But don't ask me why they do
                // it this way...!
                //
                throw(new OncRpcAuthenticationException(
                              OncRpcAuthStatus.ONCRPC_AUTH_REJECTEDCRED));
            }
        }
        return auth;
    }

    /**
     * Decodes -- that is: deserializes -- an ONC/RPC authentication object
     * (credential & verifier) on the server side.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    public abstract void xdrDecodeCredVerf(XdrDecodingStream xdr)
           throws OncRpcException, IOException;

    /**
     * Encodes -- that is: serializes -- an ONC/RPC authentication object
     * (its verifier) on the server side.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    public abstract void xdrEncodeVerf(XdrEncodingStream xdr)
           throws OncRpcException, IOException;

}

// End of OncRpcServerAuth.java
