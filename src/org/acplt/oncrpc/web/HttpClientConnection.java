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

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ProtocolException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.acplt.oncrpc.OncRpcConstants;

/**
 * The class <code>HttpClientConnection</code> provides a simple HTTP/1.1
 * compliant connection from a client to an HTTP server. This class does not
 * provide a full-blown HTTP connection, but it is rather optimized for what
 * ONC/RPC clients need in order to tunnel ONC remote procedure calls through
 * ordinary HTTP connections, thus penetrating firewalls.
 *
 * <p>A <code>HttpClientConnection</code> is not that clever as you would
 * first expect. Rather you have to do some things for yourself, like
 * reconnecting dropped connections, and so on. While this sometimes result
 * in more labour on the caller's shoulders, this keeps resource wasting at
 * a minimum, and gives you full control over redirections and other mess --
 * you do want full control, right?.
 *
 * <p>For this reason, for instance, an <code>HttpClientConnection</code>
 * does not buffer the whole request before sending it to the server but
 * rather relies on the caller to supply the right content-length information.
 * This avoids unnecessary double buffering but instead creates the bas64
 * encoded content on-the-fly.
 *
 * <p>Of course, this client connection object does not touch the content,
 * it just suplies the pipe to swallow the data.
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 */
public class HttpClientConnection {

    /**
     * Default port where HTTP servers listen for incomming requests. This is
     * just a convenience definition to make the code more readable.
     */
    public final static int HTTP_DEFAULTPORT = 80;

    /**
     * Constructs a new <code>HttpClientConnection</code>. The port used on
     * the HTTP server side is the default HTTP port, 80.
     *
     * @param hostname name (DNS name or IP dotted address) of host running
     *   a HTTP server to which we want to connect to.
     */
    public HttpClientConnection(String hostname) {
        this(hostname, HTTP_DEFAULTPORT);
    }

    /**
     * Constructs a new <code>HttpClientConnection</code>.
     *
     * @param hostname name (DNS name or IP dotted address) of host running
     *   a HTTP server to which we should connect to.
     * @param port Port number where the HTTP server can be contacted.
     */
    public HttpClientConnection(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        //
        // The connection is closed, that is, dead.
        //
        mode = HTTP_DEAD;
    }

    /**
     * Closes the connection to the HTTP server and frees up some resources.
     * After calling <code>close</code> it is still possible to open a new
     * connection to the HTTP server once again.
     */
    public void close() {
        if ( socket != null ) {
            //
            // Close the input and output streams, then close the
            // socket. Catch all exceptions when closing each object
            // individually, so we can clean up all three despite
            // them throwing nasty exceptions at us...
            //
            try {
                in.close();
            } catch ( IOException e ) { }
            try {
                out.close();
            } catch ( IOException e ) { }
            try {
                socket.close();
            } catch ( IOException e ) { }
            socket = null;
            out = null;
            in = null;
        }
        mode = HTTP_DEAD;
    }

    /**
     * Starts a new HTTP "POST" request and sends all necessary HTTP header
     * fields. Next, the caller can send lots of content using the
     * {@link #writeContentBytes} method. Finally, to finish the request
     * he has to call the {@link #endPostRequest} method.
     *
     * @param path Path to server object which handles the POST request.
     *   For instance, this can be a CGI script (although it better should
     *   not be one, except in the case of FAST CGI).
     * @param mimeType MIME-classified type of content to be sent.
     * @param contentLength Length of content to be sent. If negative, the
     *   length is not known in advance. In this case, keeping the connection
     *   alive is not possible, so callers should avoid this situation,
     *   if possible.
     *
     * @exception IOException if an I/O exception occurs when sending the
     *   HTTP headers. In this case the connection is closed automatically
     *   and the caller <b>must not</b> send any content. However, the caller
     *   is free to give the request another try by calling
     *   <code>beginEncoding</code> again, thus opening a new HTTP connection.
     */
    public void beginPostRequest(String path,
                                 String mimeType, int contentLength)
           throws IOException {
        //
        // Make sure that the connection is open. This will also handle the
        // case that the HTTP server or proxy can not keep connections alive,
        // either because it can not per se or the client does not know the
        // requests's content length in advance (bad, bad, very bad!).
        //
        connect();
        //
        // Remember how many content bytes we need to see before finishing
        // the request and then enter "sending" mode.
        //
        remainingContentLength = contentLength;
        mode = HTTP_SENDING;
        //
        // Set the socket timeout, so we don't hang around forever waiting
        // for an answer or to get rid of our content.
        //
        socket.setSoTimeout(timeout);
        //
        // Send method header and all the other useful headers.
        //
        if ( useProxy ) {
            writeln("POST http://" + hostname + path + " HTTP/1.1");
        } else {
            writeln("POST " + path + " HTTP/1.1");
        }
        //
        // Now send all the interesting headers...
        // This first involves indicating the host we intended to contact
        // while going through an HTTP proxy. Also send some interesting
        // agent information about us...
        //
        if ( !useProxy ) {
            writeln("Host: " + hostname);
        }
        writeln("User-Agent: " + userAgentId);
        //
        // Try to keep the connection alive.
        //
        if ( useProxy ) {
            writeln("Proxy-Connection: keep-alive");
        } else {
            writeln("Connection: keep-alive");
        }
        //
        // Ensure that proxies do not cache the POST request. On the other
        // side of the wire, the HTTP server -- respective the redirecting
        // CGI -- is responsible for returning appropriate cache control
        // headers.
        //
        writeln("Cache-Control: no-cache, no-store, private");
        writeln("Pragma: no-cache");
        //
        // Finish headers with a description of the content that will follow
        // within this POST request. If the exact content-length is not known
        // in advance we have to fall back to the old close-the-line principle.
        //
        // Note: this is currently not possible because Java does not allow
        // us to close only one side of the socket connection. Half-closes
        // have only been introduced lately with the JDK1.3 -- sloooooowly,
        // Sun is learning what is needed for an "Internet Platform". As if
        // that wasn't clear from the beginning... did no-one read the
        // BSD socket API for a start?! And what about Mr. Tanenbaum?!
        //
        writeln("Content-Type: " + mimeType);
        if ( contentLength > 0 ) {
            writeln("Content-Length: " + contentLength);
        } else {
            throw(new ProtocolException(
                "ONC/RPC HTTP-tunnel POST needs content length to keep the connection alive"));
        }
        //
        // Finish the header section of the HTTP request. This is marked
        // according to the HTTP specification by an empty line. The next
        // thing following is content...
        //
        writeln("");
        //
        // Let the real content roll -- or were that the "good times?"
        //
    }

    /**
     * Send (part) of the content to the HTTP server. Note that the output
     * is done unbuffered, so callers should write their content in large
     * chunks to avoid the calling overhead for sending data.
     *
     * @param bytes The data.
     * @param offset Start offset in the data.
     * @param length Number of bytes to write.
     *
     * @exception RuntimeException if too much content was sent.
     * @exception IOException if an I/O error occurs.
     * @exception NullPointerException if <code>bytes</code> is
     *   <code>null</code>.
     * @exception IndexOutOfBoundsException if <code>offset</code> is negative,
     *   or <code>length</code> is negative, or <code>offset + length</code> is
     *   greater than the length of the array <code>bytes</code>.
     */
    public void writeContentBytes(byte [] bytes, int offset, int length)
           throws IOException {
        if ( mode != HTTP_SENDING ) {
            throw(new ProtocolException(
                "ONC/RPC HTTP tunnel not in sending mode"));
        }
        //
        // Check incomming parameters...
        //
        if ( bytes == null) {
            throw(new NullPointerException());
        } else if ( (offset < 0)
                    || (offset > bytes.length)
                    || (length < 0)
                    || ((offset + length) > bytes.length)
                    || ((offset + length) < 0) ) {
            throw(new IndexOutOfBoundsException());
        } else if ( length == 0 ) {
            return;
        }
        //
        // First check that not too much amount of content gets sent.
        // (Looks like this should be a code template for marketing!)
        //
        if ( remainingContentLength >= 0 ) {
            remainingContentLength -= length;
            if ( remainingContentLength < 0 ) {
                close();
                throw(new ProtocolException(
                    "ONC/RPC HTTP tunnel received too much content"));
            }
        }
        //
        // Whow! Finally write some bytes... In case we get an I/O error,
        // we terminate the connection and rethrow the I/O exception.
        //
        try {
            out.write(bytes, offset, length);
        } catch ( IOException e ) {
            close();
            throw(e);
        }
    }

    /**
     * Ends the HTTP "POST" request. The next logical step for a caller is
     * then to call ... #FIXME
     */
    public void endPostRequest()
           throws IOException {
        if ( remainingContentLength > 0 ) {
            //
            // As not all content has been sent, abort the connection the
            // hard way and throw an exception to notify the caller.
            //
            close();
            throw(new ProtocolException(
                "ONC/RPC HTTP tunnel received not enough content"));
        }
        try {
            out.flush();
        } catch ( IOException e ) {
            //
            // If flushing the connection results in an I/O exception, tear
            // down the connection before rethrowing the exception. This allows
            // the caller to reconnect and retry the POST.
            //
            close();
            throw(e);
        }
        mode = HTTP_IDLE;
    }

    /**
     * Handle options sent by the HTTP server.
     *
     * <p>Currently the following options are handled by this class:
     * <ul>
     *  <li>Content-Length -- length of content following the header section</li>
     *  <li>Content-Type -- type of content sent</li>
     *  <li>Proxy-Connection -- handle keep-alive request</li>
     *  <li>Connection -- handle keep-alive request</li>
     *  <li>Transfer-Encoding -- transfer encoding choosen by HTTP server</li>
     * </ul>
     *
     * @param option Name of option sent by HTTP server.
     * @param value Value of option.
     */
    protected void handleOption(String option, String value) {
// FIXME        System.out.println("OPTION: \"" + option + "\" = \"" + value + "\"");

        if ( "Content-Length".equalsIgnoreCase(option) ) {
            try {
                remainingContentLength = Integer.parseInt(value);
            } catch ( NumberFormatException e ) {
            }
        } else if ( "Content-Type".equalsIgnoreCase(option) ) {
            contentType = value;
        } else if ( "Proxy-Connection".equalsIgnoreCase(option) ) {
            if ( useProxy ) {
                keepAlive = "Keep-Alive".equalsIgnoreCase(value);
            }
        } else if ( "Connection".equalsIgnoreCase(option) ) {
            if ( !useProxy ) {
                keepAlive = "Keep-Alive".equalsIgnoreCase(value);
            }
        } else if ( "Transfer-Encoding".equalsIgnoreCase(option) ) {
            chunkedTransfer = "chunked".equalsIgnoreCase(value);
        }
    }

    /**
     * Read the HTTP headers sent by the servers and also parse them at the
     * same time.
     *
     * @return HTTP status code.
     */
    private int readHeaders()
            throws IOException {
        //
        // Reset/init section.
        //
        keepAlive = false;
        remainingContentLength = 0;
        chunkedTransfer = false;
        remainingChunkLength = 0;
        contentType = null;

        //
        // Read response line, but handle the dreaded HTTP/1.1 100 Continue
        // server response.
        //
        String [] param;
        int httpStatus;
        for ( ;; ) {
            //
            // First, read in the response line, which contains the HTTP version
            // spoken by the server, the status code, and a reason phrase.
            //
            param = new String[1];
            if ( !readHeaderLine(param)
                 || !param[0].startsWith("HTTP/") ) {
                throw(new IOException("Invalid HTTP header"));
            }
            String header = param[0];
            //
            // Retrieve the status code from the HTTP header line. This involves
            // finding the end of the HTTP/x.x string and skipping all spaces
            // until we reach the HTTP status code in the form of XYZ.
            // Yes, we are not interested in the HTTP protocol spoken by the server.
            //
            int index = 0;
            int len = header.length();
            for ( ; (index < len) && (header.charAt(index) != ' ') ; ++index ) {
                // empty
            }
            for ( ; (index < len) && (header.charAt(index) == ' ') ; ++index ) {
                // empty
            }
            try {
                responseCode = 300; // #FIXME?
                httpStatus = Integer.parseInt(header.substring(index, index + 3));
                responseCode = httpStatus;
            } catch ( NumberFormatException e ) {
                throw(new IOException("Invalid HTTP header"));
            }
            //
            // If it's not a "100 Continue", then we can proceed reading
            // header lines. Otherwise we expect to see another HTTP response
            // line.
            //
            if ( responseCode != 100 ) {
                break;
            }
        }
        //
        // Now parse the following options within the HTTP header. Every
        // non-empty line contains an HTTP option, which is handed over to
        // the handleOption method for implementation-specific behaviour.
        //
        param = new String[2];
        for ( ;; ) {
            if ( readHeaderLine(param) ) {
                handleOption(param[0], param[1]);
            } else {
                //
                // End of HTTP header section reached, so leave the loop.
                //
                break;
            }
        }
        //
        // Some final sanity checks: if the server does not know how long
        // the content it sends is going to be, it sends a negative length.
        // In this case we can not keep the connection alive (and in fact
        // the server should not have been responded with such a header, but
        // we gracefully ignore this here).
        //
        if ( remainingContentLength <= 0 ) {
            keepAlive = false;
            remainingContentLength = -1; // means "don't know"
        }
        //
        // Done. Return the status of the HTTP request returned in this reply.
        //
        return httpStatus;
    }

    /**
     * Read in a header line coming over the HTTP connection from the server.
     *
     * @param keyvalue An array with room for either exactly one or two
     *   strings, receiving the header option and optionally its value. If
     *   only room for a single return string is supplied, then
     *   <code>readHeaderLine</code> will only read in the header line (like
     *   the first HTTP header) without separating the options's value from
     *   its name. If a header option has no option, then <code>null</code>
     *   is returned as the value string.
     *
     * @return <code>false</code> if the end of the headers has been reached.
     */
    private boolean readHeaderLine(String [] keyvalue)
            throws IOException {
        int headerSize = headerLine.length;
        int index = 0;
        boolean option = keyvalue.length > 1;
        //
        // Now parse the next header line.
        //
        int ch;
        int nextch;
        int colon = -1;
    ReadLine:
        while ( (ch = in.read()) >= 0 ) {
            switch ( ch ) {
            //
            // For header options in form of key: value remember the position.
            // Note that the use the invariant that the index can not be
            // negative. So if the colon position is not negative, it has
            // already been set and we must not set it again, as, for instance,
            // the value of the option might also contains colons.
            //
            case ':':
                if ( colon < 0 ) {
                    colon = index;
                }
                break;
            //
            // Replace HT with SP
            //
            case '\t':
                ch = ' ';
                break;
            //
            // Swallow CRLF. As we speak HTTP/1.1 (or at least we could), we
            // need to support header lines folded onto multiple lines.
            //
            case '\r':
            case '\n':
                //
                // Check for a continuation line, which is indicated by a
                // new line beginning with either a space or a horizontal tab.
                //
                in.mark(1);
                nextch = in.read();
                if ( (ch == '\r') && (nextch == '\n') ) {
                    //
                    // Okay. This *is* a CRLF. Now let's look for a continuation
                    // indication...
                    //
                    in.mark(1);
                    nextch = in.read();
                    if ( (nextch != ' ') && (nextch != '\t') ) {
                        //
                        // Nope. This is the final line of a header line. So we need
                        // to back up so the char can be read on the next round.
                        //
                        in.reset();
                        break ReadLine;
                    }
                } else {
                    in.reset();
                }
                //
                // This is either crap or a continuation line, so we replace
                // the crap or the CRLF and the following SP or HT (in case of
                // a continuation line) with a single SP.
                //
                ch = ' ';
                break;
            }
            //
            // Add character to header line buffer, growing the buffer as
            // needed.
            //
            if ( index >= headerSize ) {
                char [] newHeaderLine = new char[headerSize * 2];
                System.arraycopy(headerLine, 0, newHeaderLine, 0, headerSize);
                headerLine = newHeaderLine;
                headerSize *= 2;
            }
            headerLine[index++] = (char) ch;
        }
        //
        // End of headers reached (an empty line)?
        //
        if ( index == 0 ) {
            return false;
        }
        //
        // We are done. Return the header string, constructed from the buffer.
        // In case we're dealing with options which have values and parsing
        // is not disabled, we first separate the option and its value and
        // then return as two separate strings.
        //
        while ( (--index > 0) && (headerLine[index] <= ' ') ) {
            // empty
        }
        ++index;
        if ( option ) {
            if ( colon <= 0 ) {
                //
                // Only "option", but not "option: value"
                //
                keyvalue[0] = new String(headerLine, 0, index);
                keyvalue[1] = null;
            } else {
                keyvalue[0] = new String(headerLine, 0, colon);
                while ( (++colon < index) && (headerLine[colon] <= ' ') ) {
                    // empty
                }
                keyvalue[1] = new String(headerLine, colon, index - colon);
            }
        } else {
            keyvalue[0] = new String(headerLine, 0, index);
        }
        return true;
    }

    /**
     * Read exactly one line, termined by CRLF or either CR or LF, and return
     * it.
     *
     * @return Line without the terminating CR, LF or CRLF.
     */
    private String readLine()
            throws IOException {
        int headerSize = headerLine.length;
        int index = 0;
        //
        // Now parse the next header line.
        //
        int ch;
        int nextch;
    ReadLine:
        while ( (ch = in.read()) >= 0 ) {
            switch ( ch ) {
            //
            // Swallow CRLF.
            //
            case '\r':
            case '\n':
                in.mark(1);
                nextch = in.read();
                if ( (ch == '\r') && (nextch == '\n') ) {
                    //
                    // Okay. This *is* a CRLF.
                    //
                    break ReadLine;
                } else {
                    in.reset();
                }
                //
                // This is either crap or a continuation line, so we replace
                // the crap or the CRLF and the following SP or HT (in case of
                // a continuation line) with a single SP.
                //
                ch = ' ';
                break;
            }
            //
            // Add character to header line buffer, growing the buffer as
            // needed.
            //
            if ( index >= headerSize ) {
                char [] newHeaderLine = new char[headerSize * 2];
                System.arraycopy(headerLine, 0, newHeaderLine, 0, headerSize);
                headerLine = newHeaderLine;
                headerSize *= 2;
            }
            headerLine[index++] = (char) ch;
        }
        //
        // End of headers reached?
        //
        return new String(headerLine, 0, index);
    }


    /**
     * Begin receiving the content sent by the HTTP server. This
     * method blocks until at least the HTTP header section has been received
     * or until the connection times out.
     *
     * @return HTTP server response code (status code).
     */
    public int beginDecoding()
           throws IOException {

        mode = HTTP_RECEIVING;
        finalChunkSeen = false;
        //
        // Now read the HTTP headers sent by the server. If the response code
        // is not okay, we end decoding immediately. This will either result
        // in the connection getting closed of the content skipped, if the
        // connection can be kept alive.
        //
        readHeaders();
        switch ( responseCode ) {
        case 200:
            //
            // Accepted request...
            //
            break;
        default:
            //
            // All other response codes: not accepted!
            //
            endDecoding();
        }
        return responseCode;
    }

    /**
     * Returns the content type (MIME type, charset, etc.).
     *
     * @return content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Read content sent by the HTTP server. This method also handles the
     * HTTP/1.1 chunked transfer encoding if the HTTP server insisted on using
     * it. If only chunked transfer encoding has been introduced with the
     * first official 0.9 protocol version of HTTP, it would have made sending
     * ONC/RPC requests much easier, because we would not need to buffer the
     * while request before sending it just to know its exact length.
     * Unfortunately, chunking has only been introduced lately so we can not
     * expect servers and especially proxies to handle it. Sigh.
     *
     * @param buffer Buffer to receive the content sent by the HTTP server.
     * @param offset Start offset in the buffer.
     * @param length Number of bytes to receive.
     *
     * @exception ProtocolException if not enough content was available (the
     *   caller attempted to read more data than available) or if we received
     *   junk information violating the HTTP protocol specification.
     * @exception IOException if an I/O error occurs.
     * @exception NullPointerException if <code>bytes</code> is
     *   <code>null</code>.
     * @exception IndexOutOfBoundsException if <code>offset</code> is negative,
     *   or <code>length</code> is negative, or <code>offset + length</code> is
     *   greater than the length of the array <code>bytes</code>.
     */
    public int readContentBytes(byte [] buffer, int offset, int length)
           throws IOException {
        int bytesread;
        int total = 0;

        if ( mode != HTTP_RECEIVING ) {
            throw(new ProtocolException(
                "ONC/RPC HTTP tunnel not in receiving mode"));
        }
        //
        // Check incomming parameters...
        //
        if ( buffer == null ) {
            throw(new NullPointerException());
        } else if ( (offset < 0)
                    || (offset > buffer.length)
                    || (length < 0)
                    || ((offset + length) > buffer.length)
                    || ((offset + length) < 0) ) {
            throw(new IndexOutOfBoundsException());
        } else if ( length == 0 ) {
            return 0;
        }
        //
        // First check that there is still enough content to read. In case
        // the HTTP server did not indicate the content length, we return
        // as much as possible until we hit the "end of file". Note that
        // in case of chuncked transfers, we don't know the content length
        // in advance too, so we read 'til we drop.
        //
        // In contrast to Java's read() from the io package, we block until
        // we either got the specified amout of content or until we reach
        // eof.
        //
        if ( remainingContentLength >= 0 ) {
            //
            // Do not decrement remainingContentLength here to avoid getting
            // out of sync in case of exceptions...
            //
            if ( remainingContentLength < length ) {
                close();
                throw(new ProtocolException(
                    "ONC/RPC HTTP tunnel has not enough content available"));
            }
        }
        //
        // Whow! Finally read some bytes... In case we get an I/O error,
        // we terminate the connection and rethrow the I/O exception.
        // But beginning with HTTP/1.1 reading some bytes can actually be not
        // that easy as expected from earlier protocol versions, as we now
        // probably have to deal with transfer encodings. Currently, we only
        // support chunked transfers.
        //
        if ( chunkedTransfer ) {
            int toRead;
            //
            // Beginning with HTTP/1.1 we need to handle chunked transfers,
            // where the sender splits the content into chunks without telling
            // the receiver the overall total length, but only how long an
            // individual chunk is going to be. Advantage: the sender does not
            // need to buffer the content in order to find out the total length,
            // but can instead use a fixed-sized buffer and split the content
            // into appropriate-sized chunks.
            //
            try {
                for ( ;; ) {
                    //
                    // If the previous chunk has been completely read, we now
                    // need to read in the length of the following chunk. If
                    // we're currently in the middle of a chunk, we can skip
                    // this step and immediately proceed to slurp chunk content.
                    //
                    if ( remainingChunkLength <= 0 ) {
                        if ( finalChunkSeen ) {
                            throw(new ProtocolException(
                                "ONC/RPC HTTP tunnel has not enough content available"));
                        }
                        //
                        // Next chunk or first chunk:
                        // the length of the following chunk is encoded as
                        // an hex integer followed by a line break. If we read
                        // a zero length chunk, we've reached the end of the
                        // road. This is only acceptable if no more data needs
                        // to be read.
                        //
                        String hexLen = readLine();
                        //
                        // This is ridiculous: parseInt does not like white
                        // space, and Apache sends them after the chunk length
                        // and before the line termination CRLF. So we need
                        // to trim the string.
                        //
                        try {
                            remainingChunkLength = Integer.parseInt(hexLen.trim(), 16);
                            if ( remainingChunkLength < 0 ) {
                                throw(new NumberFormatException("must not be negative"));
                            }
                        } catch ( NumberFormatException e ) {
                            throw(new ProtocolException(
                                "HTTP chunking transfer protocol violation: invalid chunk length \""
                                + hexLen + "\""));
                        }
                        if ( remainingChunkLength == 0 ) {
                            finalChunkSeen = true;
                            //if ( length > 0 ) {
                                //
                                // If we got here, we reached the last chunk but
                                // the caller wanted even more data.
                                //
                            //    throw(new ProtocolException(
                            //        "ONC/RPC HTTP tunnel has not enough content available"));
                            //}
                            return total;
                        }
                    }
                    //
                    // Now read in as many data as the caller wants or as many
                    // as the current chunk contains, whichever is less. Note
                    // that the read method will only return *up to* the amount
                    // of data specified, so it can perfectly return with less
                    // data than expected if the I/O layer, and especially the
                    // TCP stack decides to do so although there's still data
                    // "in the pipe". To cope with this situation, we turn one
                    // round after another as long as there is still data for
                    // the current chunk outstanding.
                    //
                    while ( remainingChunkLength > 0 ) {
                        toRead = length <= remainingChunkLength ?
                                     length : remainingChunkLength;
                        bytesread = in.read(buffer, offset, toRead);
                        if ( bytesread < 0 ) {
                            throw(new ProtocolException(
                                "ONC/RPC HTTP tunnel has not enough content available"));
                        }
                        offset += bytesread;
                        total += bytesread;
                        length -= bytesread;
                        remainingChunkLength -= bytesread;
                        if ( length <= 0 ) {
                            //
                            // In case we also reached the end of the current
                            // chunk, swallow the CRLF terminating the chunk.
                            // Otherwise we would get into trouble when the
                            // next read expects to see a chunk length
                            // indication.
                            //
                            if ( remainingChunkLength <= 0 ) {
                                readLine();
                            }
                            return total;
                        }
                    }
                    //
                    // Swallow trailing CRLF terminating each chunk of data,
                    // then start over with the next chunk, because there is
                    // still data to be read.
                    //
                    readLine();
                }
            } catch ( IOException e ) {
                close();
                throw(e);
            }
        }
        //
        // Handle protocol versions HTTP/0.9, HTTP/1.0, or non-chunked
        // transfers with HTTP/1.1. This is the simple case, where we only
        // slurp in bytes, bytes, bytes...
        //
        try {
            while ( length > 0 ) {
                bytesread = in.read(buffer, offset, length);
                if ( bytesread < 0 ) {
                    //
                    // In case we reach eof, the read() method will first
                    // return all data up to eof, and then, on the next call,
                    // -1 to indicate eof. We only can reach eof when the
                    // server closed its sending side of the connection. When
                    // we see an eof, we make sure that the caller did not
                    // expected to get more data when we got.
                    //
                    if ( remainingContentLength >= 0 ) {
                        throw(new ProtocolException(
                        "ONC/RPC HTTP tunnel has not enough content available"));
                    }
                    break;
                }
                total += bytesread;
                offset += bytesread;
                length -= bytesread;
            }
        } catch ( IOException e ) {
            //
            // In case of I/O problems, and especially communication timeouts,
            // close the connection and rethrow the exception to notify the
            // caller.
            //
            close();
            throw(e);
        } finally {
            //
            // Don't forget to update the counter bean heads...
            //
            if ( remainingContentLength >= 0 ) {
                remainingContentLength -= total;
            }
        }
        //
        // Phew! Done with the easy case, now return the amount of data
        // received from the HTTP server in this turn.
        //
        return total;
    }

    /**
     * Returns amount of content still available (to be read). This always
     * shows the remaining amount and is updated whenever content is read
     * using {@link #readContentBytes}.
     *
     * @return Amount of content available.
     */
    public int getRemainingContentLength() {
        return remainingContentLength;
    }

    /**
     *
     * <p>This method silently discards any unread content, if the caller has
     * yet not read all content.
     */
    public void endDecoding()
           throws IOException {

        //
        // Make sure that we slurp all data yet unread to keep keep-alive
        // connections alive (I love the English tongue... but maybe this is
        // just the result of my German tongue and syntax going crazy, so
        // Mark Twain was right. No, Mark Twain did not invent the scanner API!)
        //
        if ( chunkedTransfer ) {
            if ( keepAlive && !finalChunkSeen ) {
                //
                // Only makes sense if the connection can be kept alive, otherwise
                // we don't want to bother ourselves slurping junk data...
                //
                int chunkLength;
                long bytesSkipped;
                try {
                    //
                    // First, get rid of the current chunk, if there is any.
                    //
                    while ( remainingChunkLength > 0 ) {
                        bytesSkipped = in.skip(remainingChunkLength);
                        if ( bytesSkipped < 0 ) {
                            throw(new IOException("Could not skip chunk"));
                        }
                        remainingChunkLength -= bytesSkipped;
                    }
                    //
                    // Then dispose any other chunks...
                    //
                    String hexLen;
                    while ( !finalChunkSeen ) {
                        //
                        // Read next chunk header, then flush chunk data, including
                        // the CRLF terminating each chunk.
                        //
                        hexLen = readLine();
                        chunkLength = Integer.parseInt(hexLen, 16);
                        if ( chunkLength < 0 ) {
                            throw(new NumberFormatException("must not be negative"));
                        }
                        if ( chunkLength == 0 ) {
                            break;
                        }
                        while ( chunkLength > 0 ) {
                            bytesSkipped = in.skip(remainingChunkLength);
                            if ( bytesSkipped < 0 ) {
                                throw(new IOException("Could not skip chunk"));
                            }
                            chunkLength -= bytesSkipped;
                        }
                        readLine();
                    }
                } catch ( Exception e ) {
                    //
                    // Got in any trouble? Then kill the connection.
                    //
                    close();
                }
            }
        } else if ( keepAlive ) {
            //
            // Skip remaining unread content, if the content length is known
            // in advance. Otherwise drop the connection.
            //
            if ( remainingContentLength > 0 ) {
                long bytesSkipped;
                while ( remainingContentLength > 0 ) {
                    bytesSkipped = in.skip(remainingContentLength);
                    if ( bytesSkipped < 0 ) {
                        close();
                        break;
                    }
                    remainingContentLength -= bytesSkipped;
                }
            } else if ( remainingContentLength < 0 ) {
                close();
            }
        }
        //
        // Indicate new mode, if the connection is still alive, or close it
        // if it can not kept alive.
        //
        if ( mode != HTTP_DEAD ) {
            if ( !keepAlive ) {
                close();
            } else {
                mode = HTTP_IDLE;
            }
        }
    }


    /**
     * Set the timout for sending or receiving information to/from the HTTP
     * server.
     *
     * @param milliseconds Timeout in milliseconds.
     */
    public void setTimeout(int milliseconds) {
        if ( milliseconds <= 0 ) {
            throw(new IllegalArgumentException("timeouts must be positive."));
        }
        timeout = milliseconds;
    }

    /**
     * Retrieve the current timeout set for remote procedure calls. A timeout
     * of zero indicates batching calls (no reply message is expected).
     *
     * @return Current timeout.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     *
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     *
     */
    public boolean getKeepAlive() {
        return keepAlive;
    }

    /**
     * Connects to the HTTP server. In case an HTTP proxy has been configured,
     * this connects to the proxy instead.
     *
     * <p>If the connection is in keep-alive mode and already open, then the
     * connection is reused.
     *
     * @exception SecurityException if a security manager exists and its
     *     <code>checkConnect</code> method does not allow the connect
     *     operation.
     */
    private void connect()
            throws IOException {
        //
        // Only connect, if we don't already have a socket at hand. Otherwise
        // we reuse the old connection, if that is okay.
        //
        if ( socket != null ) {
            if ( keepAlive ) {
                return;
            }
            close();
        }
        //
        // Check that we are allowed to connect to the HTTP server. Note that
        // this is the barrier which keeps callers from connecting to anywhere
        // through a proxy server -- note that connection to the proxy is
        // always done as a privileged operation, so we must be cautious here.
        //
        SecurityManager security = System.getSecurityManager();
        if ( security != null ) {
            security.checkConnect(hostname, port);
        }
        //
        // If we had already successfully contacted the proxy HTTP server
        // during the last request, we contact it once again. This way, we
        // don't need to go through all the system's property hasle, etc.
        //
        if ( cachedProxyHost != null ) {
            socket = new Socket(cachedProxyHost, cachedProxyPort);
            useProxy = true;
        } else {
            //
            // FIXME: support for non-proxied servers...
            //

            //
            // Check for proxy server. If one has configured, this is the
            // one to connect to. The proxy then will connect to the real
            // http server.
            //
            final String proxyHost = getProxyHost();
            if ( proxyHost != null ) {
                final int proxyPort = getProxyPort();
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction() {
                        public Object run()
                            throws IOException {
                            socket = new Socket(proxyHost, proxyPort);
                            useProxy = true;
                            cachedProxyHost = proxyHost;
                            cachedProxyPort = proxyPort;
                            return null; // return value is not used by caller
                        }
                    } );
                } catch ( PrivilegedActionException e ) {
                    //
                    // Okay, now we can really try to connect to the HTTP server.
                    //
                    useProxy = false;
                    socket = new Socket(InetAddress.getByName(hostname), port);
                }
            } else {
                //
                // Okay, now we can really try to connect to the HTTP server.
                //
                useProxy = false;
                socket = new Socket(InetAddress.getByName(hostname), port);
            }
        }
        //
        // It might help to disable Nagle, so that packets are not delayed
        // after we've handed it over to the network stack before going to
        // the wire.
        //
        socket.setTcpNoDelay(true);
        //
        // Get all the streams we like to work with. We do buffer both the
        // input and output streams to make the whole thing perform better.
        //
        out = new BufferedOutputStream(socket.getOutputStream(), 4096);
        in = new BufferedInputStream(socket.getInputStream(), 4096);
        //
        // FIXME??
        keepAlive = true;
    }

    /**
     * Writes an ASCII string to the HTTP server (this is buffered first).
     *
     * @param s String to write.
     */
    private void write(String s)
            throws IOException {
        int slen = s.length();
        int sindex = 0;
        int index;
        int blocklen = asciiBuffer.length;
        int len;
        while ( slen > 0 ) {
            len = slen > blocklen ? blocklen : slen;
            for ( index = 0; index < len; ++index ) {
                asciiBuffer[index] = (byte) s.charAt(sindex++);
            }
            slen -= len;
            out.write(asciiBuffer, 0, len);
        }
    }

    /**
     * Writes an ASCII string and appends a line termination in the form of
     * CR followed by LF.
     *
     * @param s String to write.
     */
    private void writeln(String s)
            throws IOException {
        write(s);
        out.write(CRLF);
    }


    /**
     * Retrieves the host name where the HTTP proxy server resided from the
     * system properties.
     *
     * @return Name of proxy host or <code>null</code> if no proxy has been
     *   configured.
     */
    private String getProxyHost() {
        String proxyHost = (String) AccessController.doPrivileged(
                               new GetPropertyPrivilegedAction("http.proxyHost"));
        if ( proxyHost == null ) {
            proxyHost = (String) AccessController.doPrivileged(
                            new GetPropertyPrivilegedAction("proxyHost"));
        }
        return proxyHost;
    }

    /**
     * Retrieves the port number where the HTTP proxy server resides from
     * the system properties. If no port number has been configured for the
     * HTTP proxy, then the default http port number of 80 is returned.
     *
     * @return Port number of HTTP proxy server.
     */
    private int getProxyPort() {
        String proxyPort = (String) AccessController.doPrivileged(
                               new GetPropertyPrivilegedAction("http.proxyPort"));
        if ( proxyPort == null ) {
            proxyPort = (String) AccessController.doPrivileged(
                            new GetPropertyPrivilegedAction("proxyPort"));
        }
        if ( proxyPort != null ) {
            //
            // If something has been specified as the proxy port system
            // property, try to convert this into a port number. If this
            // fails, return the default port number of an HTTP server.
            //
            try {
                return Integer.parseInt(proxyPort);
            } catch ( NumberFormatException e ) {
            }
        }
        return HTTP_DEFAULTPORT;
    }



    /**
     * Host name of HTTP server to contact in the form of
     * <code>www.acplt.org</code>.
     */
    private String hostname;

    /**
     * Port number where to contact the HTTP server. This defaults to the
     * standard port where HTTP servers are expected: port 80.
     */
    private int port = HTTP_DEFAULTPORT;

    /**
     * TCP/IP socket for communication with the HTTP server.
     */
    private Socket socket;

    /**
     * Timeout (in milliseconds) for communication with an HTTP server.
     */
    private int timeout = 30000;

    /**
     *
     */
    private boolean keepAlive = true;

    /**
     * Indicates whether the HTTP server send its reply using the chunked
     * transfer encoding.
     */
    private boolean chunkedTransfer;

    /**
     * Contains the amount of data still to be read for the current chunk.
     */
    private int remainingChunkLength;

    /**
     * Indicates whether the end-of-chunks chunk has been read (whow, another
     * one for the Purlitzer price).
     */
    private boolean finalChunkSeen;

    /**
     * Type of content sent by HTTP server.
     */
    private String contentType;

    /**
     * Buffered output stream used for sending the HTTP headers. This stream
     * is <b>not</b> used to send content in an HTTP request.
     */
    private OutputStream out;

    /**
     *
     */
    private InputStream in;

    /**
     * Contains the HTTP response code from the last request sent to the
     * HTTP server.
     */
    private int responseCode;

    /**
     * Buffer receiving ASCII characters from Unicode strings when sending
     * (header) strings to the HTTP server.
     */
    private byte [] asciiBuffer = new byte[1024];

    /**
     * Dynamically growing buffer used during header parsing.
     */
    private char [] headerLine = new char[80];

    /**
     *
     */
    private String userAgentId = "Not Mozilla, but RemoteTea v"
                                 + OncRpcConstants.REMOTETEA_VERSION_STRING;

    /**
     *
     */
    private final static int HTTP_DEAD = 0;
    private final static int HTTP_IDLE = 1;
    private final static int HTTP_SENDING = 2;
    private final static int HTTP_RECEIVING = 3;

    /**
     * Indicates sending/receiving mode of the HTTP connection.
     */
    private int mode = HTTP_IDLE;

    /**
     * Indicates the amount of content data which still has to be sent or
     * received.
     */
    private int remainingContentLength;

    /**
     *
     */
    public final static byte [] CRLF = { 13, 10 };

    /**
     * Indicates whether a proxy HTTP server needs to be contacted in order
     * to reach the real HTTP server.
     */
    private boolean useProxy = false;

    /**
     * Address of proxy host to contact instead of real HTTP server.
     */
    private String cachedProxyHost;

    /**
     * Port number of proxy HTTP server.
     */
    private int cachedProxyPort;



    /**
     *
     */
    class GetPropertyPrivilegedAction implements PrivilegedAction {

        /**
         *
         */
        public GetPropertyPrivilegedAction(String property) {
            this.property = property;
        }

        /**
         *
         */
        public GetPropertyPrivilegedAction(String property, String defaultValue) {
            this.property = property;
            this.defaultValue = defaultValue;
        }

        /**
         *
         */
        public Object run() {
            return System.getProperty(property, defaultValue);
        }

        /**
         * The property to retrieve.
         */
        private String property;

        /**
         * Default value or <code>null</code>.
         */
        private String defaultValue;

    }


}

// End of HttpClientConnection.java
