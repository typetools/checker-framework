// A test that the checker is sound in the presence of instance initializer blocks.
// In the resourceleak-permitinitializationleak/ directory, it's a test that the
// checker is unsound with the -ApermitInitializationLeak command-line argument.

import org.checkerframework.checker.mustcall.qual.*;

import java.net.Socket;

class InstanceInitializer {
    // :: error: required.method.not.called
    private @Owning Socket s;

    private final int DEFAULT_PORT = 5;
    private final String DEFAULT_ADDR = "localhost";

    {
        try {
            // This assignment is OK, because it's the first assignment.
            s = new Socket(DEFAULT_ADDR, DEFAULT_PORT);
        } catch (Exception e) {
        }
    }

    {
        try {
            // This assignment is not OK, because it's a reassignment without satisfying the
            // mustcall obligations of the previous value of `s`.
            // With -ApermitInitializationLeak, the Resource Leak Checker unsoundly permits it.
            s = new Socket(DEFAULT_ADDR, DEFAULT_PORT);
        } catch (Exception e) {
        }
    }

    {
        try {
            // :: error: required.method.not.called
            Socket s1 = new Socket(DEFAULT_ADDR, DEFAULT_PORT);
        } catch (Exception e) {
        }
    }

    {
        Socket s1 = null;
        try {
            s1 = new Socket(DEFAULT_ADDR, DEFAULT_PORT);
        } catch (Exception e) {
        }
        s1.close();
    }

    public InstanceInitializer() throws Exception {
        s = new Socket(DEFAULT_ADDR, DEFAULT_PORT);
    }
}
