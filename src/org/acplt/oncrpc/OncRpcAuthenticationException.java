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

package org.acplt.oncrpc;

/**
 * The class <code>OncRpcAuthenticationException</code> indicates an
 * authentication exception.
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public class OncRpcAuthenticationException extends OncRpcException {

    /**
     * Initializes an <code>OncRpcAuthenticationException</code>
     * with a detail of {@link OncRpcException#RPC_AUTHERROR} and
     * the specified {@link OncRpcAuthStatus authentication status} detail.
     *
     * @param authStatus The authentication status, which can be any one of
     *   the {@link OncRpcAuthStatus OncRpcAuthStatus constants}.
     */
    public OncRpcAuthenticationException(int authStatus) {
        super(RPC_AUTHERROR);

        authStatusDetail = authStatus;
    }

    /**
     * Returns the authentication status detail of this ONC/RPC exception
     * object.
     *
     * @return  The authentication status of this <code>OncRpcException</code>.
     */
    public int getAuthStatus() {
        return authStatusDetail;
    }

    /**
     * Specific authentication status detail (reason why this authentication
     * exception was thrown).
     *
     * @serial
     */
    private int authStatusDetail;

}

// End of OncRpcAuthenticationException.java

