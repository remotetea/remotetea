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

import java.io.IOException;
import java.io.InterruptedIOException;
import org.acplt.oncrpc.web.*;

/**
 * ONC/RPC client which communicates with ONC/RPC servers over the network
 * using the ISO/OSI level 7 application protocol HTTP as a tunnel.
 *
 * <p>Please note that currently no standard exists about how to tunnel
 * XDR data over HTTP connections. There are a few solutions out there, but
 * they are more or less incompatible due to the lack of an RFC. So I'm now
 * adding yet another proprietary solution.
 *
 * <p>The protocol which is used here is rather simple and tries to be
 * compatible with as much firewall systems as possible. For this to achieve,
 * both the ONC/RPC calls as well as their replies are first base64 encoded,
 * before they are sent through the tunnel. This way, calls and replies appear
 * to be ordinary text documents of mime type "text/plain".
 *
 * <p>Calls will appear to be something like this, carrying redirection
 * information. Note that we do not include ONC/RPC call information in the
 * header section, as this is already included in the ONC/RPC call itself.
 * So including it in the header would just make it easier to play games
 * with the header.
 * <pre>
 *     CALL <i>host name</i>:<i>port</i> <i>protocol</i> TEA/1&lt;CR&gt;&lt;LF&gt;
 *     B0D0EADSDEADBEEF...&lt;CR&gt;&lt;LF&gt;
 *     ...&lt;CR&gt;&lt;LF&gt;
 *     DEADBE==&lt;CR&gt;&lt;LF&gt;
 * </pre>
 *
 * <p>Replies do not carry the redirection head, but only the base64 encoded
 * data of the ONC/RPC reply:
 * <pre>
 *     B0D0EADSDEADBEEF...&lt;CR&gt;&lt;LF&gt;
 *     ...&lt;CR&gt;&lt;LF&gt;
 *     DEADBE==&lt;CR&gt;&lt;LF&gt;
 * </pre>
 *
 * <p>The decoding from the base64 encoded data is carried out by the
 * {@link XdrHttpDecodingStream} class.
 *
 * <p>I'm not using eecks-emm-ell on purpose (net yet). While it is surely
 * fun to play with and it has its merits, just to create yet another RPC
 * tunnel parsing XML is still too large an overhead to accept. Despite cynics
 * pointing out that the Internet is already that slow so that XML overhead
 * will not be visible at all, I nevertheless disagree. On the other hand,
 * XML would be really fine to be misused for yet another proprietary and
 * misguided ASCII, pardon UTF-8, data format...
 * <pre>
 *     &lt;?xml version="1.0"?&gt;
 *     &lt;!DOCTYPE oncrpc-call SYSTEM "oncrpc-call.dtd"&gt;
 *     &lt;oncrpc-call server="foo.bar.com" protocol="tcp"&gt;
 *         B0D0EADSDEADBEEF...&lt;CR&gt;&lt;LF&gt;
 *         ...&lt;CR&gt;&lt;LF&gt;
 *         DEADBE==&lt;CR&gt;&lt;LF&gt;
 *     &lt;/oncrpc-call&gt;
 * </pre>
 *
 * <p>The answer then could be represented as follows:
 * <pre>
 *     &lt;?xml version="1.0"?&gt;
 *     &lt;!DOCTYPE oncrpc-reply SYSTEM "oncrpc-reply.dtd"&gt;
 *     &lt;oncrpc-reply&gt;
 *         B0D0EADSDEADBEEF...&lt;CR&gt;&lt;LF&gt;
 *         ...&lt;CR&gt;&lt;LF&gt;
 *         DEADBE==&lt;CR&gt;&lt;LF&gt;
 *     &lt;/oncrpc-reply&gt;
 * </pre>
 *
 * <p>So it should be fairly easy to switch over to XML if someone will
 * insist on it. Reminds me of my Xmas lecture about "Internet Technologies --
 * Sacred Land of the Automation Industry?"...
 *
 * @version $Revision$ $Date$ $State$ $Locker$
 * @author Harald Albrecht
 *
 * @see XdrHttpDecodingStream
 * @see org.acplt.oncrpc.web.HttpClientConnection
 */
public class OncRpcHttpClient extends OncRpcClient {

    /**
     * Constructs a new <code>OncRpcHttpClient</code> object, which connects
     * to the ONC/RPC server at <code>host</code> for calling remote procedures
     * of the given { program, version }. At the other end of the HTTP tunnel,
     * TCP/IP is used to call the ONC/RPC server.
     *
     * <p>Note that the HTTP connection is not build before the first ONC/RPC
     * call is done through the {@link #call} method. The HTTP client tries to
     * keep the connection alive but reconnects if necessary. Nevertheless, as
     * it signals all failures, the caller has to handle reconnect situations
     * -- but this is easy to achieve.
     *
     * @param hostname The DNS name of the host where the ONC/RPC server
     *   resides.
     * @param cgiHandlerPath The path to the CGI Handler which will redirect
     *   ONC/RPC calls to the particular ONC/RPC servers.
     * @param oncrpcHostname The DNS name of the ONC/RPC server to contact.
     * @param program Program number of the ONC/RPC server to call.
     * @param version Program version number.
     * @param port The port number where the ONC/RPC server can be contacted.
     *   If <code>0</code>, then the other end of the HTTP tunnel will try to
     *   ask the portmapper at <code>host</code> for the port number.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    public OncRpcHttpClient(String hostname,
                            String cgiHandlerPath,
                            String oncrpcHostname,
                            int program, int version, int port)
           throws OncRpcException, IOException {
        this(hostname,
             HttpClientConnection.HTTP_DEFAULTPORT,
             cgiHandlerPath,
             oncrpcHostname,
             program, version, port,
             OncRpcProtocols.ONCRPC_TCP);
    }

    /**
     * Constructs a new <code>OncRpcHttpClient</code> object, which connects
     * to the ONC/RPC server at <code>host</code> for calling remote procedures
     * of the given { program, version }.
     *
     * <p>Note that the HTTP connection is not build before the first ONC/RPC
     * call is done through the {@link #call} method. The HTTP client tries to
     * keep the connection alive but reconnects if necessary. Nevertheless, as
     * it signals all failures, the caller has to handle reconnect situations
     * -- but this is easy to achieve.
     *
     * @param hostname The DNS name of the host where the ONC/RPC server
     *   resides.
     * @param cgiHandlerPath The path to the CGI Handler which will redirect
     *   ONC/RPC calls to the particular ONC/RPC servers.
     * @param oncrpcHostname The DNS name of the ONC/RPC server to contact.
     * @param program Program number of the ONC/RPC server to call.
     * @param version Program version number.
     * @param port The port number where the ONC/RPC server can be contacted.
     *   If <code>0</code>, then the other end of the HTTP tunnel will try to
     *   ask the portmapper at <code>host</code> for the port number.
     * @param protocol Transport protocol to be used by the other end of
     *   the tunnel to call the ONC/RPC server.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    public OncRpcHttpClient(String hostname,
                            String cgiHandlerPath,
                            String oncrpcHostname,
                            int program, int version, int port,
                            int protocol)
           throws OncRpcException, IOException {
        this(hostname,
             HttpClientConnection.HTTP_DEFAULTPORT,
             cgiHandlerPath,
             oncrpcHostname,
             program, version, port,
             protocol);
    }

    /**
     * Constructs a new <code>OncRpcHttpClient</code> object, which connects
     * to the ONC/RPC server at <code>host</code> for calling remote procedures
     * of the given { program, version }.
     *
     * <p>Note that the HTTP connection is not build before the first ONC/RPC
     * call is done through the {@link #call} method. The HTTP client tries to
     * keep the connection alive but reconnects if necessary. Nevertheless, as
     * it signals all failures, the caller has to handle reconnect situations
     * -- but this is easy to achieve.
     *
     * @param hostname The DNS name of the host where the ONC/RPC server
     *   resides.
     * @param httpPort The port number where the HTTP server is to be
     *   contacted.
     * @param cgiHandlerPath The path to the CGI Handler which will redirect
     *   ONC/RPC calls to the particular ONC/RPC servers.
     * @param oncrpcHostname The DNS name of the ONC/RPC server to contact.
     * @param program Program number of the ONC/RPC server to call.
     * @param version Program version number.
     * @param port The port number where the ONC/RPC server can be contacted.
     *   If <code>0</code>, then the other end of the HTTP tunnel will try to
     *   ask the portmapper at <code>host</code> for the port number.
     * @param protocol Transport protocol to be used by the other end of
     *   the tunnel to call the ONC/RPC server.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     * @throws IOException if an I/O error occurs.
     */
    public OncRpcHttpClient(String hostname,
                            int httpPort,
                            String cgiHandlerPath,
                            String oncrpcHostname,
                            int program, int version, int port,
                            int protocol)
           throws OncRpcException, IOException {
        //
        // Construct the inherited part of our object. This will also try to
        // lookup the port of the desired ONC/RPC server, if no port number
        // was specified (port = 0).
        //
        // PLEASE NOTE:
        // - We supply a null InetAddress to our parent class, as we do not
        //   make use of an InetAddress at all but rather juggle with the
        //   plain host name. This is necessary in order to support virtual
        //   hosting of web servers, and the HTTP client object needs to
        //   know to which particular web server it contacts, despite of
        //   the server's internet address.
        // - The parent class will not try to look up the port address
        //   as we signal that we are going to use HTTP tunneling.
        //
        super(null, program, version, -1, OncRpcProtocols.ONCRPC_HTTP);
        this.httpPort = httpPort;
        this.cgiHandlerPath = cgiHandlerPath;
        this.oncrpcHostname = oncrpcHostname;
        this.oncrpcProtocol = protocol;
        this.port = port;
        //
        // We need to work with the host name instead of its address
        // so we can support virtual web hosting, where multiple DNS names
        // map to the same IP address, and the client sends the host name
        // it wants to contact within the HTTP requests.
        //
        this.hostname = hostname;
        httpClient = new HttpClientConnection(hostname, httpPort);
        //
        // Create the necessary encoding and decoding streams, so we can
        // communicate at all.
        // FIXME: write specialized dynamically buffered encoding stream!
        sendingXdr = new XdrBufferEncodingStream(8192);
        receivingXdr = new XdrHttpDecodingStream(httpClient);
    }

    /**
     * Close the connection to an ONC/RPC server and free all network-related
     * resources. Well -- at least hope, that the Java VM will sometimes free
     * some resources. Sigh.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     */
    public void close()
           throws OncRpcException {
        if ( httpClient != null ) {
            httpClient.close();
            httpClient = null;
        }
        //
        // Close both XDR streams individually to avoid missing the second
        // one when the first one throws an exception.
        //
        if ( sendingXdr != null ) {
            try {
                sendingXdr.close();
            } catch ( IOException e ) {
            } finally {
                sendingXdr = null;
            }
        }
        if ( receivingXdr != null ) {
            try {
                receivingXdr.close();
            } catch ( IOException e ) {
            } finally {
                receivingXdr = null;
            }
        }
    }

    /**
     * Calls a remote procedure on an ONC/RPC server.
     *
     * <p>FIXME: timeout control?
     *
     * @param procedureNumber Procedure number of the procedure to call.
     * @param versionNumber Protocol version number.
     * @param parameters The parameters of the procedure to call, contained
     *   in an object which implements the {@link XdrAble} interface.
     * @param result The object receiving the result of the procedure call.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     */
    public synchronized void call(int procedureNumber, int versionNumber,
                                  XdrAble params, XdrAble result)
        throws OncRpcException {
        int responseCode;
    Refresh:
        for ( int refreshesLeft = 1; refreshesLeft >= 0;
              --refreshesLeft ) {
            //
            // First, build the ONC/RPC call header. Then put the sending
            // stream into a known state and encode the parameters to be
            // sent. Finally tell the encoding stream to send all its data
            // to the server. Then wait for an answer, receive it and decode
            // it. So that's the bottom line of what we do right here.
            //
            nextXid();

            OncRpcClientCallMessage callHeader =
                new OncRpcClientCallMessage(xid,
                                            program, versionNumber, procedureNumber,
                                            auth);
            OncRpcClientReplyMessage replyHeader =
                new OncRpcClientReplyMessage(auth);

            //
            // Send call message to server. If we receive an IOException,
            // then we'll throw the appropriate ONC/RPC (client) exception.
            // Note that we use a connected stream, so we don't need to
            // specify a destination when beginning serialization.
            //
            try {
                sendingXdr.beginEncoding(null, 0);
                callHeader.xdrEncode(sendingXdr);
                params.xdrEncode(sendingXdr);
                sendingXdr.endEncoding();
            } catch ( IOException e ) {
                throw(new OncRpcException(OncRpcException.RPC_CANTSEND,
                                          e.getLocalizedMessage()));
            }
            //
            // Embedd the ONC/RPC call request within an unsuspiciously
            // looking ASCII page. Well, while I don't like this, it is
            // a good way to get access to a remote system from behind
            // a company's firewall, when the sysadmin disables free access
            // to that "domain of darkness" also known as "The Internet".
            //
            StringBuffer prefix = new StringBuffer(512);
            //
            // For calls we need to add some "bang information" (courtesty of
            // UU), so the other end of the HTTP tunnel can redirect the
            // ONC/RPC calls to the proper ONC/RPC server. Note that we
            // send only the bare minimum for routing. The missing information
            // is already contained in the ONC/RPC header following, so we
            // do not duplicate it. I want to avoid making it too easy to
            // spoof redirection information.
            //
            prefix.append("CALL "); // tunnel method
            prefix.append(oncrpcHostname); // host to contact
            if ( port > 0 ) { // optional port number
                prefix.append(":");
                prefix.append(port);
            }
            prefix.append(" ");
            prefix.append(oncrpcProtocol); // transport protocol
            prefix.append(" ");
            prefix.append(HttpTunnelConstants.TUNNEL_PROTO_ID); prefix.append("\r\n");
            //
            // Terminate header section.
            //
            prefix.append("\r\n");

            //
            // Ccalculate the length of the full content, including the
            // ASCII-ized ONC/RPC call record.
            //
            int contentLength = sendingXdr.getXdrLength();
            int lineCount = (contentLength + (HttpTunnelConstants.BYTES_PER_LINE - 1))
                              / HttpTunnelConstants.BYTES_PER_LINE;
            int realLength = prefix.length()
                             + ((contentLength + 2) / 3) * 4 + lineCount * 2
                             ;
            // FIXME
            byte [] xdrData = sendingXdr.getXdrData();
            //
            // We are now ready to start the POST request, which will carry
            // our ONC/RPC call record to the HTTP server and beyond it to
            // the destination ONC/RPC server.
            //
            try {
                httpClient.beginPostRequest(cgiHandlerPath,
                                            "text/plain", realLength);
                //
                // First, send the HTTP tunnel ONC/RPC header. This header
                // contains "routing information", so that the receiver at
                // the other end of the HTTP tunnel can redirect the call
                // to the appropriate ONC/RPC server.
                //
                String s = prefix.toString();
                httpClient.writeContentBytes(s.getBytes(), 0, s.length());
                //
                // Next, encode the binary XDR data. This is done in blocks,
                // speeding up things a little bit by avoiding making too
                // many single calls into the output stream (although buffered).
                //
                byte [] lines = new byte[HttpTunnelConstants.ENCODED_BYTES_PER_LINE_CRLF
                                         * HttpTunnelConstants.LINES_PER_BLOCK];
                int xdrOffset = 0;
                int offset = 0;
                while ( contentLength >= HttpTunnelConstants.BYTES_PER_LINE ) {
                    if ( offset >= (HttpTunnelConstants.ENCODED_BYTES_PER_LINE_CRLF
                                    * HttpTunnelConstants.LINES_PER_BLOCK) ) {
                        httpClient.writeContentBytes(lines, 0, offset);
                        offset = 0;
                    }
                    offset += Base64.encode(xdrData, xdrOffset,
                                            HttpTunnelConstants.BYTES_PER_LINE,
                                            lines, offset);
                    lines[offset++] = 13;
                    lines[offset++] = 10;
                    xdrOffset += HttpTunnelConstants.BYTES_PER_LINE;
                    contentLength -= HttpTunnelConstants.BYTES_PER_LINE;
                }
                //
                // If there's still data left over which did not fill a
                // complete line then generate the last line and flush it.
                //
                if ( contentLength > 0 ) {
                    if ( offset >= (HttpTunnelConstants.ENCODED_BYTES_PER_LINE_CRLF
                                    * HttpTunnelConstants.LINES_PER_BLOCK) ) {
                        httpClient.writeContentBytes(lines, 0, offset);
                        offset = 0;
                    }
                    offset += Base64.encode(xdrData, xdrOffset, contentLength,
                                           lines, offset);
                    lines[offset++] = 13;
                    lines[offset++] = 10;
                }
                if ( offset > 0 ) {
                    httpClient.writeContentBytes(lines, 0, offset);
                }
                //
                // Indicate the end of the request, so that all data gets sent
                // to the HTTP server for processing.
                //
                httpClient.endPostRequest();
            } catch ( IOException e ) {
                throw(new OncRpcException(OncRpcException.RPC_CANTSEND,
                                          e.getLocalizedMessage()));
            }

            //
            // Receive reply message from server -- at least try to do so...
            // IMPORTANT NOTE:
            // - we do not support batched calls through HTTP tunnels, as then
            //   an additional record layer would have been to be put between
            //   the HTTP post and the base64 encoding layer.
            //
            try {
                //
                // In contrast to TCP/IP and UDP/IP-based transports, we
                // can expect the tunnel to be responding with the matching
                // ONC/RPC reply. If it does not, there's no way of waiting
                // for another reply -- because we only have the strict
                // "one call, one reply" interaction scheme at our hands.
                // Nevertheless: we still check for a matching reply, but can
                // not wait for another one suddenly popping up from the
                // HTTP tunnel.
                //

                //
                // First, pull off the reply message header of the
                // XDR stream. In case we also received a verifier
                // from the server and this verifier was invalid, broken
                // or tampered with, we will get an
                // OncRpcAuthenticationException right here, which will
                // propagate up to the caller. If the server reported
                // an authentication problem itself, then this will
                // be handled as any other rejected ONC/RPC call.
                //
                // While that sounds easy, it is hard work in the face
                // of decoding the Base64 encoded data. But we have the
                // decoding HTTP/XDR stream to delegate all this dirty
                // work to...
                //
                receivingXdr.beginDecoding();
                //
                // Make sure that we got an okay from the web server and
                // some data.
                //
                responseCode = httpClient.getResponseCode();
                if ( (responseCode < 200) || (responseCode >= 300) ) {
                    // FIXME
                    throw(new OncRpcException(OncRpcException.RPC_FAILED,
                                              "HTTP tunnel response error "
                                              + responseCode));
                }
                //
                // Pull off the RPC header from the HTTP stream.
                //
                replyHeader.xdrDecode(receivingXdr);
                //
                // Only deserialize the result, if the reply matches the
                // call. Otherwise skip this record.
                //
                if ( replyHeader.messageId != callHeader.messageId ) {
                    receivingXdr.endDecoding();
                    // FIXME: CHECKME exception code
                    throw(new OncRpcException(OncRpcException.RPC_WRONGMESSAGE));
                }

                //
                // Make sure that the call was accepted. In case of unsuccessful
                // calls, throw an exception, if it's not an authentication
                // exception. In that case try to refresh the credential first.
                //
                if ( !replyHeader.successfullyAccepted() ) {
                    receivingXdr.endDecoding();
                    //
                    // Check whether there was an authentication
                    // problem. In this case first try to refresh the
                    // credentials.
                    //
                    if ( (refreshesLeft > 0)
                         && (replyHeader.replyStatus
                             == OncRpcReplyStatus.ONCRPC_MSG_DENIED)
                         && (replyHeader.rejectStatus
                             == OncRpcRejectStatus.ONCRPC_AUTH_ERROR)
                         && (auth.canRefreshCred()) ) {
                        continue Refresh;
                    }
                    //
                    // Nope. No chance. This gets tough.
                    //
                    throw(replyHeader.newException());
                }
                result.xdrDecode(receivingXdr);
                //
                // Free pending resources of buffer and exit the call loop,
                // returning the reply to the caller through the result
                // object.
                //
                receivingXdr.endDecoding();
                return;
            } catch ( InterruptedIOException e ) {
                //
                // In case our time run out, we throw an exception.
                //
                throw(new OncRpcTimeoutException());
            } catch ( IOException e ) {
                //
                // Argh. Trouble with the transport. Seems like we can't
                // receive data. Gosh. Go away!
                //
                throw(new OncRpcException(OncRpcException.RPC_CANTRECV,
                                          e.getLocalizedMessage()));
            }
        } // for ( refreshesLeft )
    }

    /**
     * DNS name of host where to contact HTTP server. Note that we can not
     * use an <code>InetAddress</code> here as we have to support virtual
     * hosting, where several DNS names share the same IP address.
     */
    private String hostname;

    /**
     * Port number where the HTTP server can be contacted.
     */
    private int httpPort;

    /**
     * Path of cgi program redirecting ONC/RPC calls to the appropriate
     * ONC/RPC servers.
     */
    private String cgiHandlerPath;

    /**
     * DNS name of ONC/RPC server receiving the calls.
     */
    private String oncrpcHostname;

    /**
     * Transport protocol to be used by the other end of the tunnel to
     * contact the ONC/RPC server.
     */
    private int oncrpcProtocol;

    /**
     * The HTTP client responsible for handling the HTTP connection. It is
     * a classic example of delegation, isn't it?
     */
    private HttpClientConnection httpClient;

    /**
     * FIXME: use the right encoding stream!
     * XDR encoding stream used for sending requests via UDP/IP to an ONC/RPC
     * server.
     */
    private XdrBufferEncodingStream sendingXdr;

    /**
     * XDR decoding stream used when receiving replies via an HTTP tunnel
     * from an ONC/RPC server.
     */
    private XdrHttpDecodingStream receivingXdr;

}

// End of OncRpcHttpClient.java
