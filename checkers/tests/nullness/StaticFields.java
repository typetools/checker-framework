import checkers.nullness.quals.*;

class StaticFields {
    @NonNullOnEntry("nullable")
    void testF() {
        nullable.toString();
    }

    static @Nullable String nullable = null;
    static @Nullable String otherNullable = null;

    void trueNegative() {
        //:: (dereference.of.nullable)
        nullable.toString();
        //:: (dereference.of.nullable)
        otherNullable.toString();
    }

    @NonNullOnEntry("nullable")
    void test1() {
        nullable.toString();
        //:: (dereference.of.nullable)
        otherNullable.toString();
    }

    @NonNullOnEntry("otherNullable")
    void test2() {
        //:: (dereference.of.nullable)
        nullable.toString();
        otherNullable.toString();
    }

    @NonNullOnEntry({"nullable", "otherNullable"})
    void test3() {
        nullable.toString();
        otherNullable.toString();
    }

}
