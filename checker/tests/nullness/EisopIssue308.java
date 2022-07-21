// Test case for eisop issue #308:
// https://github.com/eisop/checker-framework/issues/308

class EisopIssue308Other {
    abstract class Inner implements Runnable {}
}

class EisopIssue308 {
    EisopIssue308Other other = new EisopIssue308Other();

    EisopIssue308Other.Inner foo() {
        return other.new Inner() {
            public void run() {}
        };
    }
}
