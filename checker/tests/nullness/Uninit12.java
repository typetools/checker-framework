import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.*;

// This is a test case for issue #105:
// http://code.google.com/p/checker-framework/issues/detail?id=105

// field f1, which is non-null, but is never initialized.
// Fields f2 and f3 are OK.

//:: error: (initialization.fields.uninitialized)
public class Uninit12 {

    static Object f1;

    public Uninit12() {
        f1.toString();
    }
}

// no error here
class Uninit12b {

    public Uninit12b() {
        f2.toString();
        f3.toString();
    }

    static Object f2 = new Object();
    static Object f3;

    static {
        f3 = new Object();
    }
}
