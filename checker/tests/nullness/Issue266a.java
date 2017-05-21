// Test case for issue 266:
// https://github.com/typetools/checker-framework/issues/266

// @skip-test until the issue is fixed

import org.checkerframework.checker.nullness.qual.*;

class Issue266a {
    private final Object mBar;

    public Issue266a() {
        mBar = "test";
        Runnable runnable =
                new Runnable() {
                    @Override
                    public void run() {
                        // unexpected [dereference.of.nullable] error here
                        mBar.toString();
                    }
                };
        runnable.run();
    }
}
