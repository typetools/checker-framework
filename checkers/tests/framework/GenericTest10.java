// Test case for (a part of) Issue 142:
// http://code.google.com/p/checker-framework/issues/detail?id=142
class GenericTest10 {
    abstract static class Bijection<A, B> {
        abstract B apply(A a);

        abstract A invert(B b);

        Bijection<B, A> inverse() {
            return new Bijection<B, A>() {
                A apply(B b) {
                    return Bijection.this.invert(b);
                }

                B invert(A a) {
                    return Bijection.this.apply(a);
                }

                Bijection<A, B> inverse() {
                    return Bijection.this;
                }
            };
        }
    }
}
