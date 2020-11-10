// Test case for Issue 1431
// https://github.com/typetools/checker-framework/issues/1431
public class Issue1431 {
    static class Outer<V> {
        class Inner<T extends V> {}
    }

    Outer<Object>.Inner<int[]> ic;

    Issue1431(Outer<Object>.Inner<int[]> ic) {
        this.ic = ic;
    }
}
