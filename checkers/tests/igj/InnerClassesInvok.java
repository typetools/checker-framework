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
                isAImmutable(); // error
                isAReadOnly();
                A.this.isAMutable();
                A.this.isAImmutable(); // error
                A.this.isAReadOnly();

                isBMutable();   // error
                isBImmutable();
                isBReadOnly();

                B.this.isBMutable();   // error
                B.this.isBImmutable();
                B.this.isBReadOnly();
            }

            void testMutableForB(@Mutable B this) {
                isAMutable();
                isAImmutable(); // error
                isAReadOnly();
                A.this.isAMutable();
                A.this.isAImmutable(); // error
                A.this.isAReadOnly();

                isBMutable();
                isBImmutable(); // error
                isBReadOnly();

                B.this.isBMutable();
                B.this.isBImmutable();  // error
                B.this.isBReadOnly();
            }

        }
    }
}

