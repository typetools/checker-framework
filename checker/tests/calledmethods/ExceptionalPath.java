// A test that calling a method with exceptional exit paths leads to that method being
// considered "definitely called" (i.e. @CalledMethods of the method) on all paths.

import org.checkerframework.checker.calledmethods.qual.*;

import java.io.IOException;
import java.net.Socket;

class ExceptionalPath {
    void test(Socket s) {
        try {
            s.close();
            @CalledMethods("close") Socket s1 = s;
        } catch (IOException e) {
            @CalledMethods("close") Socket s2 = s;
        }
    }
}
