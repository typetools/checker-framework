import checkers.igj.quals.*;

@I
class A {
    void isAMutable() @Mutable { }
    void isAImmutable() @Immutable { }
    void isAReadOnly() @ReadOnly { }

    void mutableMethodForA() @Mutable {
        @I
        class B {
            void isBMutable() @Mutable { }
            void isBImmutable() @Immutable { }
            void isBReadOnly() @ReadOnly { }

            void testImmutableForB() @Immutable {
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

            void testMutableForB() @Mutable {
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

