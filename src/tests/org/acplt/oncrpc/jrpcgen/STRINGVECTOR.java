/*
 * Automatically generated by jrpcgen 1.0.5 on 11.11.05 21:11
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 */
package tests.org.acplt.oncrpc.jrpcgen;
import org.acplt.oncrpc.*;
import java.io.IOException;

public class STRINGVECTOR implements XdrAble {

    public STRING [] value;

    public STRINGVECTOR() {
    }

    public STRINGVECTOR(STRING [] value) {
        this.value = value;
    }

    public STRINGVECTOR(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        { int $size = value.length; xdr.xdrEncodeInt($size); for ( int $idx = 0; $idx < $size; ++$idx ) { value[$idx].xdrEncode(xdr); } }
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        { int $size = xdr.xdrDecodeInt(); value = new STRING[$size]; for ( int $idx = 0; $idx < $size; ++$idx ) { value[$idx] = new STRING(xdr); } }
    }

}
// End of STRINGVECTOR.java
