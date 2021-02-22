// Test case for Issue 1991:
// https://github.com/typetools/checker-framework/issues/1991

@SuppressWarnings("all") // Check for crashes only
public class Issue1991 {
    interface Comp<T extends Comp<T>> {}

    interface C<X extends Comp<? super X>> {}

    class D implements Comp<D> {}

    void f(C<D> p) {
        C<?> x = p;
    }
}
