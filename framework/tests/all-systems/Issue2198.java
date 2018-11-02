// Test case for Issue 2198.
@SuppressWarnings("unchecked")
class Issue2198 {
    interface A {}

    class B {}

    class C<T extends B & A> {
        C(T t) {}
    }

    class X {
        X(B b) {
            new C(b);
        }
    }
}
