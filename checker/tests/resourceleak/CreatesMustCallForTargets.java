// A test that errors are correctly issued when re-assignments don't match the
// create obligation annotation on a method.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

import java.io.*;

@MustCall("a") class CreatesMustCallForTargets {
    @Owning InputStream is1;

    @CreatesMustCallFor
    // :: error: incompatible.creates.mustcall.for
    static void resetObj1(CreatesMustCallForTargets r) throws Exception {
        if (r.is1 == null) {
            r.is1 = new FileInputStream("foo.txt");
        }
    }

    @CreatesMustCallFor("#2")
    // :: error: incompatible.creates.mustcall.for
    static void resetObj2(CreatesMustCallForTargets r, CreatesMustCallForTargets other)
            throws Exception {
        if (r.is1 == null) {
            r.is1 = new FileInputStream("foo.txt");
        }
    }

    @CreatesMustCallFor("#1")
    static void resetObj3(CreatesMustCallForTargets r, CreatesMustCallForTargets other)
            throws Exception {
        if (r.is1 == null) {
            r.is1 = new FileInputStream("foo.txt");
        }
    }

    @CreatesMustCallFor
    void resetObj4(CreatesMustCallForTargets this, CreatesMustCallForTargets other)
            throws Exception {
        if (is1 == null) {
            is1 = new FileInputStream("foo.txt");
        }
    }

    @CreatesMustCallFor
    // :: error: incompatible.creates.mustcall.for
    void resetObj5(CreatesMustCallForTargets this, CreatesMustCallForTargets other)
            throws Exception {
        if (other.is1 == null) {
            other.is1 = new FileInputStream("foo.txt");
        }
    }

    @CreatesMustCallFor("#2")
    // :: error: incompatible.creates.mustcall.for
    void resetObj6(CreatesMustCallForTargets this, CreatesMustCallForTargets other)
            throws Exception {
        if (other.is1 == null) {
            other.is1 = new FileInputStream("foo.txt");
        }
    }

    @CreatesMustCallFor("#1")
    void resetObj7(CreatesMustCallForTargets this, CreatesMustCallForTargets other)
            throws Exception {
        if (other.is1 == null) {
            other.is1 = new FileInputStream("foo.txt");
        }
    }

    @EnsuresCalledMethods(value = "this.is1", methods = "close")
    void a() throws Exception {
        is1.close();
    }
}
