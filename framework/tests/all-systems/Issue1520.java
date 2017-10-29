// Test case for Issue 1520
// https://github.com/typetools/checker-framework/issues/1520

import java.io.IOException;

public class Issue1520 {
    void start() {
        new Runnable() {
            public void run() {
                try {
                    _run();
                } finally {
                    signal(); // Evaluating this node type as member of implicit `this` will throw
                    // NPE
                }
            }
        };
    }

    void signal() {}

    void _run() {}

    static class Inner {}

    void test2() throws IOException {
        try {
            throwIO();
        } finally {
            Inner inner = new Inner();
        }
    }

    void throwIO() throws IOException {}
}
