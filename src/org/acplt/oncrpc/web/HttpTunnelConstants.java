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

package org.acplt.oncrpc.web;

/**
 * A collection of constants generally useful when working with HTTP
 * tunnels for the ONC/RPC protocol.
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public interface HttpTunnelConstants {

    /**
     * Amount of octets (binary data) which can be encoded in a single
     * plain ASCII line. This amount must always be a <b>multiple of
     * three</b>. This is demanded by the base64 encoding scheme, which
     * encodes every three octets using four plain ASCII characters.
     */
    public final static int BYTES_PER_LINE = 48;

    /**
     * Amount of plain ASCII characters per line for representing the encoded
     * octets. This amount is derived from the <code>BYTES_PER_LINE</code>
     * setting.
     */
    public final static int ENCODED_BYTES_PER_LINE =
        (BYTES_PER_LINE / 3) * 4;

    /**
     * Amount of plain ASCII characters per line for representing the encoded
     * octets. This amount is derived from the <code>BYTES_PER_LINE</code>
     * setting and also accounts for the line termination (CRLF).
     */
    public final static int ENCODED_BYTES_PER_LINE_CRLF =
        ENCODED_BYTES_PER_LINE + 2;

    /**
     * Amount of lines that should be processed at once using a buffer.
     */
    public final static int LINES_PER_BLOCK = 100;

    /**
     * Protocol identifier of ONC/RPC HTTP tunnel.
     */
    public final static String TUNNEL_PROTO_ID = "TEA/1.0";

}

// End of HttpTunnelConstants.java

