// Test case for Issue 2195.
@SuppressWarnings("unchecked")
class Issue2195 {
    interface A {}

    interface B {}

    class C<T extends B & A> {
        C(T t) {}
    }

    class X {
        X(B b) {
            new C(b);
        }
    }
}
