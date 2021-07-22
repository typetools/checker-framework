// Based on a Zookeeper false positive that requires unconnected socket support.

import org.checkerframework.checker.mustcall.qual.*;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

class ZookeeperReport1 {

    static int tickTime, initLimit;

    protected static @MustCall({}) Socket createSocket() throws IOException {
        Socket sock;
        sock = new Socket();
        sock.setSoTimeout(tickTime * initLimit);
        return sock;
    }

    protected static @MustCall({}) Socket createSocket2() throws IOException {
        Socket sock;
        sock = createCustomSocket();
        sock.setSoTimeout(tickTime * initLimit);
        return sock;
    }

    // This is the full version of case 1.
    protected static @MustCall({}) Socket createSocket3(boolean b) throws IOException {
        Socket sock;
        if (b) {
            sock = createCustomSocket();
        } else {
            sock = new Socket();
        }
        sock.setSoTimeout(tickTime * initLimit);
        return sock;
    }

    private static @MustCall({}) Socket createCustomSocket() {
        return new Socket();
    }

    static void use1() throws IOException {
        Socket s = createSocket();
    }

    static void use2(SocketAddress endpoint) throws IOException {
        // :: error: required.method.not.called
        Socket s = createSocket();
        s.connect(endpoint);
    }

    static void use3() throws IOException {
        Socket s = createSocket2();
    }

    static void use4(SocketAddress endpoint) throws IOException {
        // :: error: required.method.not.called
        Socket s = createSocket2();
        s.connect(endpoint);
    }

    static void use5(boolean b) throws IOException {
        Socket s = createSocket3(b);
    }

    static void use6(SocketAddress endpoint, boolean b) throws IOException {
        // :: error: required.method.not.called
        Socket s = createSocket3(b);
        s.connect(endpoint);
    }
}
