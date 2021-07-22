// A simple socket example for debugging.

import java.io.IOException;
import java.net.Socket;

class SimpleSocketExample {
    void basicTest(String address, int port) {
        try {
            // :: error: required.method.not.called
            Socket socket2 = new Socket(address, port);
            Socket specialSocket = new Socket(address, port);
            specialSocket.close();
        } catch (IOException i) {

        }
    }
}
