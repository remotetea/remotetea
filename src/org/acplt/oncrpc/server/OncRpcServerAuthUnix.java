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
 * of the ONC/RPC authentication <code>AUTH_UNIX</code> on the server
 * side.
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public final class OncRpcServerAuthUnix extends OncRpcServerAuth {

    /**
     * Contains timestamp as supplied through credential.
     */
    public int stamp;

    /**
     * Contains the machine name of caller supplied through credential.
     */
    public String machinename;

    /**
     * Contains the user ID of caller supplied through credential.
     */
    public int uid;

    /**
     * Contains the group ID of caller supplied through credential.
     */
    public int gid;

    /**
     * Contains a set of group IDs the caller belongs to, as supplied
     * through credential.
     */
    public int [] gids;

    /**
     * Constructs an <code>OncRpcServerAuthUnix</code> object and pulls its
     * state off an XDR stream.
     *
     * @param xdr XDR stream to retrieve the object state from.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    public OncRpcServerAuthUnix(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecodeCredVerf(xdr);
    }

    /**
     * Returns the type (flavor) of {@link OncRpcAuthType authentication}
     * used.
     *
     * @return Authentication type used by this authentication object.
     */
    public final int getAuthenticationType() {
        return OncRpcAuthType.ONCRPC_AUTH_UNIX;
    }

    /**
     * Sets shorthand verifier to be sent back to the caller. The caller then
     * can use this shorthand verifier as the new credential with the next
     * ONC/RPC calls to speed up things up (hopefully).
     */
    public final void setShorthandVerifier(byte [] shorthandVerf) {
        this.shorthandVerf = shorthandVerf;
    }

    /**
     * Returns the shorthand verifier to be sent back to the caller.
     */
    public final byte [] getShorthandVerifier() {
        return shorthandVerf;
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
        // Reset some part of the object's state...
        //
        shorthandVerf = null;
        //
        // Now pull off the object state of the XDR stream...
        //
        int realLen = xdr.xdrDecodeInt();
        stamp = xdr.xdrDecodeInt();
        machinename = xdr.xdrDecodeString();
        uid = xdr.xdrDecodeInt();
        gid = xdr.xdrDecodeInt();
        gids = xdr.xdrDecodeIntVector();
        //
        // Make sure that the indicated length of the opaque data is kosher.
        // If not, throw an exception, as there is something strange going on!
        //
        int len =   4 // length of stamp
                  + ((machinename.length() + 7) & ~3) // len string incl. len
                  + 4 // length of uid
                  + 4 // length of gid
                  + (gids.length * 4) + 4 // length of vector of gids incl. len
                  ;
        if ( realLen != len ) {
            if ( realLen < len ) {
                throw(new OncRpcException(OncRpcException.RPC_BUFFERUNDERFLOW));
            } else {
                throw(new OncRpcException(OncRpcException.RPC_AUTHERROR));
            }
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
        if ( shorthandVerf != null ) {
            //
            // Encode AUTH_SHORT shorthand verifier (credential).
            //
            xdr.xdrEncodeInt(OncRpcAuthType.ONCRPC_AUTH_SHORT);
            xdr.xdrEncodeDynamicOpaque(shorthandVerf);
        } else {
            //
            // Encode an AUTH_NONE verifier with zero length, if no shorthand
            // verifier (credential) has been supplied by now.
            //
            xdr.xdrEncodeInt(OncRpcAuthType.ONCRPC_AUTH_NONE);
            xdr.xdrEncodeInt(0);
        }
    }

    /**
     * Contains the shorthand authentication verifier (credential) to return
     * to the caller to be used with the next ONC/RPC calls.
     */
    private byte [] shorthandVerf;

}

// End of OncRpcServerAuthUnix.java
