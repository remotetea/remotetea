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

package tests.org.acplt.oncrpc;

import org.acplt.oncrpc.web.Base64;

public class Base64Test {

    public boolean bytecmp(byte [] b1, byte [] b2, int len) {
        for ( int i = 0; i < len; ++i ) {
            if ( b1[i] != b2[i] ) {
                return false;
            }
        }
        return true;
    }

    public void check(String test,
                      byte [] source, int lenSource, int lenEncoded) {
        byte [] encoded = new byte[((source.length + 2) / 3 * 4)];
        byte [] decoded = new byte[source.length];

        System.out.print(test + ": ");
        int len = Base64.encode(source, 0, lenSource, encoded, 0);
        if ( len != lenEncoded ) {
            System.out.println("**failed**. Expected encoded length = "
                               + lenEncoded + ", got length = " + len);
            return;
        }
        len = Base64.decode(encoded, 0, len, decoded, 0);
        if ( len != lenSource ) {
            System.out.println("**failed**. Decoded length mismatch, expected "
                               + lenSource + ", got " + len);
            return;
        }
        System.out.println("passed.");
    }

    public Base64Test() {

        byte [] source = "The Foxboro jumps over the lazy I/A".getBytes();

        check("test-1", source, 1, 4);
        check("test-2", source, 2, 4);
        check("test-3", source, 3, 4);
        check("test-4", source, 4, 8);
        check("test-5", source, source.length, ((source.length + 2) / 3) * 4);
    }

    public static void main(String[] args) {
        System.out.println("Base64Test");
        try {
            new Base64Test();
        } catch ( Exception e ) {
           e.printStackTrace(System.out);
        }
    }

}

// End of Base64Test.java