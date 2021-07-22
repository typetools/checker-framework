// Based on some false positives I found in ZK.

import java.io.IOException;
import java.net.Socket;
import java.util.List;

class EnhancedFor {
    void test(List<Socket> list) {
        for (Socket s : list) {
            try {
                s.close();
            } catch (IOException i) {
            }
        }
    }

    void test2(List<Socket> list) {
        for (int i = 0; i < list.size(); i++) {
            Socket s = list.get(i);
            try {
                s.close();
            } catch (IOException io) {
            }
        }
    }

    void test3(List<Socket> list) {
        // This error is issued because `s` is a local variable, and
        // the foreach loop under the hood assigns the result of a call
        // to Iterator#next into it (which is owning by default, because it's
        // a method return type).
        // :: error: required.method.not.called
        for (Socket s : list) {}
    }
}
