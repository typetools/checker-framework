// Test case for Issue 904:
// https://github.com/typetools/checker-framework/issues/904

// @skip-test

public class Issue904 {
    final Object mBar;
    final Runnable mRunnable =
            new Runnable() {
                @Override
                public void run() {
                    // :: error: (dereference.of.nullable)
                    mBar.toString();
                }
            };

    public Issue904() {
        mRunnable.run();
        mBar = "";
    }

    public static void main(String[] args) {
        new Issue904();
    }
}
