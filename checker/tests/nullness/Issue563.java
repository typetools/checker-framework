// Test case for Issue 563:
// https://github.com/typetools/checker-framework/issues/563
public class Issue563 {
    void bar() {
        Object x = null;
        if (Object.class.isInstance(x)) {
            x.toString();
        }
    }
}
