// Test case for Issue 1520
// xhttps://github.com/typetools/checker-framework/issues/1520
public class Issue1520 {
    void start() {
        new Runnable() {
            public void run() {
                try {
                    _run();
                } finally {
                    signal(); //Evaluating this node type as member of implicit `this` will throw NPE
                }
            }
        };
    }

    void signal() {}

    void _run() {}
}
