// Test case for Issue 1697:
// https://github.com/typetools/checker-framework/issues/1697

class Issue1697 {

    interface G<A> {
        A h(byte[] l);
    }

    interface F {}

    interface E extends F {
        interface M extends F {}
    }

    abstract static class D<A extends D<A, B>, B extends D.M<A, B>> implements E {
        abstract static class M<A extends D<A, B>, B extends M<A, B>> implements E.M {}
    }

    abstract static class C<A extends C<A, B>, B extends C.M<A, B>> extends D<A, B> {
        abstract static class M<A extends C<A, B>, B extends M<A, B>> extends D.M<A, B> {}
    }

    static class W<T extends C<T, ?>> {
        private W(T proto) {}
    }

    <T extends C<T, ?>> W<T> i(G<T> j, byte[] k) {
        return new W<>(j.h(k));
    }
}
