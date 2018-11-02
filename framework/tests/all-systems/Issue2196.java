// Test case for Issue 2196.
@SuppressWarnings("unchecked")
class Issue2196 {
    interface A {}

    interface B<V extends A, T> {}

    interface C {}

    abstract class X {

        class D<T extends A> implements B<T, C> {}

        abstract <T extends A> void f(B<T, Integer> b);

        private void g() {
            f(new D());
        }
    }
}
