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

import java.io.IOException;

/**
 *
 */
public class demoServer extends demoServerStub {

    /**
     *
     */
    public demoServer()
           throws OncRpcException, IOException {
        super();
    }

    /**
     *
     */
    public void NULL_1() {
        // definetely nothing to do here...
    }

    /**
     *
     */
    public void NULL_2() {
        // definetely nothing to do here...
    }

    /**
     *
     */
    public String echo_1(String params) {
        return params;
    }

    /**
     *
     */
    public boolean checkfoo_1(int params) {
        return params == ENUMFOO.FOO;
    }

    /**
     *
     */
    public int foo_1() {
        return ENUMFOO.FOO;
    }

    /**
     *
     */
    public String concat_1(STRINGVECTOR params) {
        StringBuffer result = new StringBuffer();
        int size = params.value.length;
        for ( int idx = 0; idx < size; ++idx ) {
            result.append(params.value[idx].value);
        }
        return result.toString();
    }

    /**
     *
     */
    public LINKEDLIST ll_1(LINKEDLIST params) {
        LINKEDLIST newNode = new LINKEDLIST();
        newNode.foo = 42;
        newNode.next = params;
        return newNode;
    }

    /**
     *
     */
    public String cat_2(String arg1, String arg2) {
        return arg1 + arg2;
    }

    /**
     *
     */
    public String cat3_2(String one, String two, String three) {
        return one + two + three;
    }

    /**
     *
     */
    public String checkfoo_2(int foo) {
        return "You are foo" + foo + ".";
    }

    /**
     *
     */
    public LINKEDLIST llcat_2(LINKEDLIST l1, LINKEDLIST l2) {
        l2.next = l1;
        return l2;
    }

    /**
     *
     */
    public void test_2(String a, int b, int c, int d) {
    }

    /**
     *
     */
    public static void main(String [] args) {
        System.out.println("Starting demoServer...");
        try {
            demoServer server = new demoServer();
            server.run();
        } catch ( Exception e ) {
            System.out.println("demoServer oops:");
            e.printStackTrace(System.out);
        }
        System.out.println("demoServer stopped.");
    }

}

// End of demoServer.java
