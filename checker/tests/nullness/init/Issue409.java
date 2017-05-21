// Test case for Issue 409:
// https://github.com/typetools/checker-framework/issues/409
// @skip-test

public class Issue409 {
    static interface Proc {
        void call();
    }

    Proc p;

    class MyProc implements Proc {
        @Override
        public void call() {
            doStuff();
        }
    }

    String foo;

    Issue409() {
        p = new MyProc();
        p.call();
        foo = "hello";
    }

    void doStuff() {
        System.out.println(foo.toLowerCase());
    }

    public static void main(String[] args) {
        new Issue409();
    }
}
