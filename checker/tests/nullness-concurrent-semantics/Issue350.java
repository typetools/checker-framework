// Test case for Issue 350:
// https://github.com/typetools/checker-framework/issues/350

import org.checkerframework.checker.nullness.qual.*;

class Test1 {

    public @Nullable String y;

    public void test2() {
        y = "";
        // Sanity check that -AconcurrentSemantics is set
        //:: error: (dereference.of.nullable)
        y.toString();
    }

    @MonotonicNonNull
    private String x;

    void test() {
        if (x == null) {
            x = "";
        }
        x.toString();
    }
}

class Test2 {

    @MonotonicNonNull
    private String x;

    void setX(String x) {
        this.x = x;
    }

    void test() {
        if (x == null) {
            x = "";
        }
        setX(x);
    }
}

class Test3 {

    @MonotonicNonNull
    private String x;

    @EnsuresNonNull("#1")
    void setX(final String x) {
        this.x = x;
    }
}
