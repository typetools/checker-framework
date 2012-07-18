import checkers.igj.quals.*;

@I
class A {
    void isAMutable(@Mutable A this) { }
    void isAImmutable(@Immutable A this) { }
    void isAReadOnly(@ReadOnly A this) { }

    void mutableMethodForA(@Mutable A this) {
        @I
        class B {
            void isBMutable(@Mutable B this) { }
            void isBImmutable(@Immutable B this) { }
            void isBReadOnly(@ReadOnly B this) { }

            void testImmutableForB(@Immutable B this) {
                isAMutable();
                //:: error: (method.invocation.invalid)
                isAImmutable();
                isAReadOnly();

                // TODO: this call should be allowed, as above, but
                // we currently take the @Immutable receiver type annotation
                // from the most enclosing method.
                //:: error: (method.invocation.invalid)
                A.this.isAMutable();
                //TODO:: error: (method.invocation.invalid)
                A.this.isAImmutable();
                A.this.isAReadOnly();

                //:: error: (method.invocation.invalid)
                isBMutable();
                isBImmutable();
                isBReadOnly();

                //:: error: (method.invocation.invalid)
                B.this.isBMutable();
                B.this.isBImmutable();
                B.this.isBReadOnly();
            }

            void testMutableForB(@Mutable B this) {
                isAMutable();
                //:: error: (method.invocation.invalid)
                isAImmutable();
                isAReadOnly();
                A.this.isAMutable();
                //:: error: (method.invocation.invalid)
                A.this.isAImmutable();
                A.this.isAReadOnly();

                isBMutable();
                //:: error: (method.invocation.invalid)
                isBImmutable();
                isBReadOnly();

                B.this.isBMutable();
                //:: error: (method.invocation.invalid)
                B.this.isBImmutable();
                B.this.isBReadOnly();
            }
        }
    }
    // TODO: add many more combinations with outer A being immutable, etc.
}

