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

package tests.org.acplt.oncrpc.jrpcgen;

import org.acplt.oncrpc.*;

import java.net.InetAddress;

import java.io.IOException;

/**
 *
 */
public class demoClientExample {

    /**
     *
     */
    public static void main(String [] args) {
        demoClient client = null;
        try {
            client = new demoClient(InetAddress.getByName("127.0.0.1"),
                                    OncRpcProtocols.ONCRPC_TCP);
        } catch ( Exception e ) {
            System.out.println("demoClientExample: oops when creating RPC client:");
            e.printStackTrace(System.out);
        }
        client.getClient().setTimeout(300*1000);

        System.out.print("About to ping: ");
        try {
            client.NULL_1();
            System.out.println("ok");
        } catch ( Exception e ) {
            e.printStackTrace(System.out);
            return;
        }

        System.out.print("About to echo: ");
        try {
            String text = "Hello, Remote Tea!";
            String echo = client.echo_1(text);
            if ( !echo.equals(text) ) {
                System.out.println(" oops. Got \"" + echo + "\" instead of \""
                                   + text + "\"");
            }
            System.out.println("ok. Echo: \"" + echo + "\"");
        } catch ( Exception e ) {
            e.printStackTrace(System.out);
            return;
        }

        System.out.print("About to concatenating: ");
        try {
            STRINGVECTOR strings = new STRINGVECTOR();
            strings.value = new STRING[3];
            strings.value[0] = new STRING("Hello, ");
            strings.value[1] = new STRING("Remote ");
            strings.value[2] = new STRING("Tea!");
            String echo = client.concat_1(strings);
            System.out.println("ok. Echo: \"" + echo + "\"");
        } catch ( Exception e ) {
            e.printStackTrace(System.out);
            return;
        }

        System.out.print("About to concatenating exactly three strings: ");
        try {
            String echo = client.cat3_2("(arg1:Hello )",
                                        "(arg2:Remote )",
                                        "(arg3:Tea!)");
            System.out.println("ok. Echo: \"" + echo + "\"");
        } catch ( Exception e ) {
            e.printStackTrace(System.out);
            return;
        }

        System.out.print("About to check for foo: ");
        try {
            if ( client.checkfoo_1(ENUMFOO.BAR) ) {
                System.out.println("oops: but a bar is not a foo!");
                return;
            }
            System.out.print("not bar: ");
            if ( !client.checkfoo_1(ENUMFOO.FOO) ) {
                System.out.println("oops: a foo should be a foo!");
                return;
            }
            System.out.println("but a foo. ok.");
        } catch ( Exception e ) {
            e.printStackTrace(System.out);
            return;
        }

        System.out.print("About to get a foo: ");
        try {
            if ( client.foo_1() != ENUMFOO.FOO ) {
                System.out.println("oops: got a bar instead of a foo!");
                return;
            }
            System.out.println("ok.");
        } catch ( Exception e ) {
            e.printStackTrace(System.out);
            return;
        }

        System.out.print("About to get a numbered foo string: ");
        try {
            String echo = client.checkfoo_2(42);
            System.out.println("ok. Echo: \"" + echo + "\"");
        } catch ( Exception e ) {
            e.printStackTrace(System.out);
            return;
        }

        System.out.print("Linked List test: ");
        try {
            LINKEDLIST node1 = new LINKEDLIST(); node1.foo = 0;
            LINKEDLIST node2 = new LINKEDLIST(); node2.foo = 8; node1.next = node2;
            LINKEDLIST node3 = new LINKEDLIST(); node3.foo = 15; node2.next = node3;
            LINKEDLIST list = client.ll_1(node1);
            System.out.print("ok. Echo: ");
            while ( list != null ) {
                System.out.print(list.foo + ", ");
                list = list.next;
            }
            System.out.println();
        } catch ( Exception e ) {
            e.printStackTrace(System.out);
            return;
        }

        System.out.print("Linking Linked Lists test: ");
        try {
            LINKEDLIST node1 = new LINKEDLIST(); node1.foo = 0;
            LINKEDLIST node2 = new LINKEDLIST(); node2.foo = 8;
            LINKEDLIST node3 = new LINKEDLIST(); node3.foo = 15; node2.next = node3;
            LINKEDLIST list = client.llcat_2(node2, node1);
            System.out.print("ok. Echo: ");
            while ( list != null ) {
                System.out.print(list.foo + ", ");
                list = list.next;
            }
            System.out.println();
        } catch ( Exception e ) {
            e.printStackTrace(System.out);
            return;
        }

        try {
            client.close();
        } catch ( Exception e ) {
            System.out.println("demoClientExample: oops when closing client:");
            e.printStackTrace(System.out);
        }
        client = null;
        System.out.println("All tests passed.");
    }

}

// End of demoClientExample.java

