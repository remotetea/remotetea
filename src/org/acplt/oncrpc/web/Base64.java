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
 * The abstract <code>Base64</code> class provides static methods to convert
 * back and forth between binary and base64-encoded data.
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public abstract class Base64 {

    /**
     * Converts binary data into base64 encoded data.
     *
     * @param binaryData Binary data to be encoded.
     * @param binaryOffset Offset into <code>binaryData</code> where to
     *   the data to be encoded begins.
     * @param length Length of data to encode.
     * @param encodedData Buffer receiving base64 encoded data.
     * @param encodedOffset Offset into <code>encodedData</code> where the
     *   store base64 encoded data.
     *
     * @return Length of encoded base64 data.
     */
    public static int encode(byte [] binaryData, int binaryOffset, int length,
                             byte [] encodedData, int encodedOffset) {
        //
        // Calculate length of encoded data including optional padding.
        //
        int encodedLength = ((length + 2) / 3) * 4;
        //
        // Now do the encoding, thus inflating every three bytes of binary
        // data to four ASCII characters.
        //
        int b1, b2, b3;
        int endPos = binaryOffset + length - 1 - 2;
        while ( binaryOffset <= endPos ) {
            b1 = binaryData[binaryOffset++];
            b2 = binaryData[binaryOffset++];
            b3 = binaryData[binaryOffset++];
            encodedData[encodedOffset++] =
                encodingBase64Alephbeth[(b1 >>> 2) & 0x3F];
            encodedData[encodedOffset++] =
                encodingBase64Alephbeth[((b1 << 4) & 0x30) | ((b2 >>> 4) & 0xF)];
            encodedData[encodedOffset++] =
                encodingBase64Alephbeth[((b2 << 2) & 0x3C) | ((b3 >>> 6) & 0x03)];
            encodedData[encodedOffset++] =
                encodingBase64Alephbeth[b3 & 0x3F];
        }
        //
        // If one or two bytes are left (because we work on blocks of three
        // bytes), convert them too and apply padding.
        //
        endPos += 2; // now points to the last encodable byte
        if ( binaryOffset <= endPos ) {
            b1 = binaryData[binaryOffset++];
            encodedData[encodedOffset++] =
                encodingBase64Alephbeth[(b1 >>> 2) & 0x3F];
            if ( binaryOffset <= endPos ) {
                b2 = binaryData[binaryOffset++];
                encodedData[encodedOffset++] =
                    encodingBase64Alephbeth[((b1 << 4) & 0x30) | ((b2 >>> 4) & 0xF)];
                encodedData[encodedOffset++] =
                    encodingBase64Alephbeth[(b2 << 2) & 0x3C];
                encodedData[encodedOffset] = '=';
            } else {
                encodedData[encodedOffset++] =
                    encodingBase64Alephbeth[(b1 << 4) & 0x30];
                encodedData[encodedOffset++] = '=';
                encodedData[encodedOffset] = '=';
            }
        }
        //
        // Finally return length of encoded data
        //
        return encodedLength;
    }

    /**
     * Converts base64 encoded data into binary data.
     *
     * @param encodedData Base64 encoded data.
     * @param encodedOffset Offset into <code>encodedData</code> where the
     *   base64 encoded data starts.
     * @param length Length of encoded data.
     * @param binaryData Decoded (binary) data.
     * @param binaryOffset Offset into <code>binaryData</code> where to
     *   store the decoded binary data.
     *
     * @return Length of decoded binary data.
     */
    public static int decode(byte [] encodedData, int encodedOffset, int length,
                             byte [] binaryData, int binaryOffset) {
        //
        // Determine the length of data to be decoded. Optional padding has
        // to be removed first (of course).
        //
        int endPos = encodedOffset + length - 1;
        while ( (endPos >= 0) && (encodedData[endPos] == '=') ) {
            --endPos;
        }
        // next line was: endPos - length / 4 + 1
        int binaryLength = endPos - encodedOffset - length / 4 + 1;
        //
        // Now do the four-to-three entities/letters/bytes/whatever
        // conversion. We chew on as many four-letter groups as we can,
        // converting them into three byte groups.
        //
        byte b1, b2, b3, b4;
        int stopPos = endPos - 3; // now points to the last letter in the
                                  // last four-letter group
        while ( encodedOffset <= stopPos ) {
            b1 = decodingBase64Alephbeth[encodedData[encodedOffset++]];
            b2 = decodingBase64Alephbeth[encodedData[encodedOffset++]];
            b3 = decodingBase64Alephbeth[encodedData[encodedOffset++]];
            b4 = decodingBase64Alephbeth[encodedData[encodedOffset++]];
            binaryData[binaryOffset++] = (byte)(((b1 << 2) & 0xFF)
                                                | ((b2 >>> 4) & 0x03));
            binaryData[binaryOffset++] = (byte)(((b2 << 4) & 0xFF)
                                                | ((b3 >>> 2) & 0x0F));
            binaryData[binaryOffset++] = (byte)(((b3 << 6) & 0xFF)
                                                | (b4 & 0x3F));
        }
        //
        // If one, two or three letters from the base64 encoded data are
        // left, convert them too.
        // Hack Note(tm): if the length of encoded data is not a multiple
        // of four, then padding must occur ('='). As the decoding alphabet
        // contains zeros everywhere with the exception of valid letters,
        // indexing into the mapping is just fine and reliefs us of the
        // pain to check everything and make thus makes the code better.
        //
        if ( encodedOffset <= endPos ) {
            b1 = decodingBase64Alephbeth[encodedData[encodedOffset++]];
            b2 = decodingBase64Alephbeth[encodedData[encodedOffset++]];
            binaryData[binaryOffset++] = (byte)(((b1 << 2) & 0xFF)
                                                | ((b2 >>> 4) & 0x03));
            if ( encodedOffset <= endPos ) {
                b3 = decodingBase64Alephbeth[encodedData[encodedOffset]];
                binaryData[binaryOffset++] = (byte)(((b2 << 4) & 0xFF)
                                                    | ((b3 >>> 2) & 0x0F));
            }
        }
        //
        // Okay. That's it for now. Just return the length of decoded data.
        //
        return binaryLength;
    }

    /**
     * Mapping from binary 0-63 to base64 alphabet according to RFC 2045.
     * (Yes, I do know that the Hebrew alphabet has only 22 letters.)
     */
    private static final byte [] encodingBase64Alephbeth = {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
      'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      '+', '/'
    };

    /**
     * Mapping from base64 alphabet to binary 0-63.
     */
    private static final byte [] decodingBase64Alephbeth;

    /**
     * The class initializer is responsible to set up the mapping from the
     * base64 alphabet (ASCII-based) to binary 0-63.
     */
    static {
        decodingBase64Alephbeth = new byte[256];
        for ( int i = 0; i < 64; ++i ) {
            decodingBase64Alephbeth[encodingBase64Alephbeth[i]] = (byte) i;
        }
    }

}

// End of Base64.java
