/*
 * $Header$
 *
 * Copyright (c) 1999, 2000, 2001
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

/**
 * The class <code>OncRpcServerTransportRegistrationInfo</code> holds
 * information about (possibly multiple) registration of server transports
 * for individual program and version numbers.
 *
 * @version $Revision$ $State$ $Locker$
 * @author Harald Albrecht
 */

public class OncRpcServerTransportRegistrationInfo {

    /**
     * @param program Number of ONC/RPC program handled by a server
     *   transport.
     * @param version Version number of ONC/RPC program handled.
     */
    public OncRpcServerTransportRegistrationInfo(int program, int version) {
        this.program = program;
        this.version = version;
    }

    /**
     * Number of ONC/RPC program handled.
     */
    public int program;

    /**
     * Version number of ONC/RPC program handled.
     */
    public int version;

}

// End of OncRpcServerTransportRegistrationInfo.java
