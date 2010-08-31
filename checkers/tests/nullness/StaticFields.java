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


    @Nullable Object field1;
    @Nullable Object field2;
 
    @NonNullOnEntry("field1")
        void method1() {
        field1.toString();        // OK, field1 is known to be non-null
        //:: (dereference.of.nullable)
        field2.toString();        // error, might throw NullPointerException
   }
 
    void method2() {
        field1 = new Object();
        method1();                // OK, satisfies method precondition
        field1 = null;
        // XXX TODO FIXME: //:: precondition.not.satisfied
        method1();                // error, does not satisfy method precondition
    }

}
