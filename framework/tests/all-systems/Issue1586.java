// Test case for Issue #1586:
// https://github.com/typetools/checker-framework/issues/1586

import java.util.concurrent.ExecutorService;

public class Issue1586 {
    void f(ExecutorService es) {
        es.execute(
                () -> {
                    try {
                        System.err.println();
                    } catch (Throwable throwable) {
                        System.err.println();
                    } finally {
                        es.execute(
                                () -> {
                                    System.err.println();
                                });
                    }
                });
    }
}
