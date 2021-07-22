// This test case was intended to simulate the code below, which issued
// a false positive at the call to LOG.warn() because the socket is @MustCall("close"):
//
//     synchronized void closeSockets() {
//       for (ServerSocket serverSocket : serverSockets) {
//           if (!serverSocket.isClosed()) {
//               try {
//                   serverSocket.close();
//               } catch (IOException e) {
//                   LOG.warn("Ignoring unexpected exception during close {}", serverSocket, e);
//               }
//           }
//       }
//    }
//

// This test is also coincidentally a test case for
// https://github.com/typetools/checker-framework/pull/3867.

import org.checkerframework.checker.mustcall.qual.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.SocketChannel;

class LogTheSocket {

    @NotOwning ServerSocket s;

    @MustCall("") Object s2;

    void testAssign(@Owning ServerSocket s1) {
        s = s1;
        // :: error: assignment
        s2 = s1;
    }

    void logVarargs(String s, Object... objects) {}

    void logNoVarargs(String s, Object object) {}

    void test(ServerSocket serverSocket) {
        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logVarargs("Ignoring unexpected exception during close {}", serverSocket, e);
            }
        }
    }

    void test2(ServerSocket serverSocket) {
        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logNoVarargs("Ignoring unexpected exception during close {}", serverSocket);
            }
        }
    }

    // This is (mostly) copied from ACSocketTest; under a previous implementation of the
    // ownership-transfer scheme,
    // it caused false positive warnings from the Must Call checker.
    SocketChannel createSock() throws IOException {
        SocketChannel sock;
        sock = SocketChannel.open();
        sock.configureBlocking(false);
        sock.socket().setSoLinger(false, -1);
        sock.socket().setTcpNoDelay(true);
        return sock;
    }

    void testPrintln(ServerSocket s) {
        System.out.println(s);
    }
}
