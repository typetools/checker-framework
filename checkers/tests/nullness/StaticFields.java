import checkers.nullness.quals.*;

class StaticFields {
    @NonNullVariable("nullable")
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

    @NonNullVariable("nullable")
    void test1() {
        nullable.toString();
        //:: (dereference.of.nullable)
        otherNullable.toString();
    }

    @NonNullVariable("otherNullable")
    void test2() {
        //:: (dereference.of.nullable)
        nullable.toString();
        otherNullable.toString();
    }

    @NonNullVariable({"nullable", "otherNullable"})
    void test3() {
        nullable.toString();
        otherNullable.toString();
    }

}
