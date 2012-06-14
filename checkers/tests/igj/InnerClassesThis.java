import checkers.igj.quals.*; import java.util.ArrayList;

@I
class MutableClass {

    void mutableMethod(@Mutable MutableClass this) {
        class ReadOnlyClass {
            void readOnlyMethod(@ReadOnly ReadOnlyClass this) {
                class ImmutableClass {
                    void immutableMethod(@Immutable ImmutableClass this) {

                        class ThisClass {
                            void mostInnerReadOnly(@ReadOnly ThisClass this) {
                                assertReadOnly(this);
                                assertImmutable(ImmutableClass.this);
                                assertReadOnly(ReadOnlyClass.this);
                                assertMutable(MutableClass.this);

                                // Check out other things
                                assertMutable(this);    // error
                                assertMutable(ImmutableClass.this);    // error
                                assertMutable(ReadOnlyClass.this);  // error
                                assertMutable(MutableClass.this);

                                assertImmutable(this);  // error
                                assertImmutable(ImmutableClass.this);
                                assertImmutable(ReadOnlyClass.this);    // error
                                assertImmutable(MutableClass.this); // error

                                assertReadOnly(this);
                                assertReadOnly(ImmutableClass.this);
                                assertReadOnly(ReadOnlyClass.this);
                                assertReadOnly(MutableClass.this);
                            }
                        }
                    }
                }
            }
        }
    }

    static void assertReadOnly(@ReadOnly Object o) { }
    static void assertMutable(@Mutable Object o) { }
    static void assertImmutable(@Immutable Object o) { }

    int i;
    Object o;
    void setField(@AssignsFields MutableClass this) {
        i = 0;   // OK
        this.i = 0; // OK
        o = new Object() {
            public String toString() {
                i++; // error
                return super.toString();
            }
        };
    }

//    public @I ArrayList<ArrayList<String>> newList()
//    {
//       return new @I ArrayList<ArrayList<String>>() { };
//    }
}
