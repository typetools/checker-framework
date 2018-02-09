// Test case for Issue #409
// https://github.com/typetools/checker-framework/issues/409

// @skip-test until the issue is fixed

public class Issue409 {
    public static void main(String[] args) {
        new Callback();
    }
}

class Callback {

    class MyProc {
        public void call() {
            doStuff();
        }
    }

    String foo;

    Callback() {
        MyProc p = new MyProc();
        // This call is illegal.  It passes an @UnderInitialization outer this, but MyProc.call is
        // declared to take an @Initialized outer this (whith is the default type).
        // :: error: (method.invocation.invalid)
        p.call();
        foo = "hello";
    }

    void doStuff() {
        System.out.println(
                foo.toLowerCase()); // this line throws a NullPointerException at run time
    }
}
