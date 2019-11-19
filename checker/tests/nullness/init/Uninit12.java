// This is a test case for issue #105:
// https://github.com/typetools/checker-framework/issues/105

import org.checkerframework.checker.nullness.qual.*;

// :: error: (initialization.static.fields.uninitialized)
public class Uninit12 {

    static Object f;

    public Uninit12() {
        f.toString();
    }

    static Object g = new Object();

    static Object h;

    static {
        h = new Object();
    }
}

class Uninit12_OK {

    static Object g = new Object();

    static Object h;

    static {
        h = new Object();
    }
}
