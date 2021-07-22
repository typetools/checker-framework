// A test for a false positive I found in Zookeeper. Sockets are must-close, but the
// result of calling toString on them shouldn't be!

import java.net.Socket;

class ToStringOnSocket {
    void log(String string) {
        System.out.println(string);
    }

    void test(Socket socket) {
        log("bad socket: " + socket);
    }

    void test2(Socket socket) {
        log("bad socket: " + socket.toString());
    }
}
