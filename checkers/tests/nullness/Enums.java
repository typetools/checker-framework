import checkers.nullness.quals.*;

public class Enums {

    enum MyEnum { A, B, C, D }
    //:: (assignment.type.incompatible)
    MyEnum myEnum = null;    // invalid
    @Nullable MyEnum myNullableEnum = null;

    void testLocalEnum() {
        // Enums are allowed to be null:  no error here.
        MyEnum myNullableEnum = null;
        //:: (assignment.type.incompatible)
        @NonNull MyEnum myEnum = null;  // invalid
    }
}
