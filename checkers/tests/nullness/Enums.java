import checkers.nullness.quals.*;

public class Enums {

    enum MyEnum { A, B, C, D }
    //:: (type.incompatible)
    MyEnum myEnum = null;    // invalid
    @Nullable MyEnum myNullableEnum = null;

    void testLocalEnum() {
        // Enums are allowed to be null:  no error here.
        MyEnum myNullableEnum = null;
        //:: (type.incompatible)
        @NonNull MyEnum myEnum = null;  // invalid
    }
}
