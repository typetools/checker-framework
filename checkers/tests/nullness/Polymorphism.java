import checkers.nullness.quals.*;

public class Polymorphism {
    // Test parameters
    @PolyNull String identity(@PolyNull String s) {
        return s;
    }

    void testParam() {
        // Test without inference
        String nullable = null;
        @NonNull String nonNull = "m";

        //:: error: (assignment.type.incompatible)
        nonNull = identity(nullable); // invalid
        nonNull = identity(nonNull);

        // test flow
        nullable = "m";
        nonNull = identity(nullable);  // valid
    }

    // Test within a method
    @PolyNull String random(@PolyNull String m) {
        if (m == "d")
            //:: error: (return.type.incompatible)
            return null;    // invalid
        return "m";         // valid
    }

    public static @PolyNull Object staticIdentity(@PolyNull Object a) {
        return a;
    }

    void testStatic(@Nullable Object nullable, @NonNull Object nonnull) {
        @Nullable Object nullable2 = staticIdentity(nullable);
        @NonNull Object nonnull2 = staticIdentity(nonnull);
    }

}
