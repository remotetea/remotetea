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

import java.io.*;
import java.net.*;

import org.acplt.oncrpc.*;

public class PseudoHttpServer {

    public PseudoHttpServer()
        throws IOException {
        ServerSocket sock = new ServerSocket(80);
        for ( ;; ) {
            try {
                Socket client = sock.accept();
                System.out.println("--REQUEST--");
                BufferedReader in = new BufferedReader(
                                        new InputStreamReader(client.getInputStream()));

                String line;
                for ( ;; ) {
                    line = in.readLine();
                    if ( line == null ) {
                        break;
                    }
                    System.out.println(line);
                }
                System.out.println("--/REQUEST--");

                in.close();
                client.close();
            } catch ( IOException e ) {
                System.out.println();
                System.out.println("----");
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("PseudoHttpServer");
        try {
            new PseudoHttpServer();
        } catch ( Exception e ) {
           e.printStackTrace(System.out);
        }
    }

}

// End of PseudoHttpServer.java