import checkers.nullness.quals.*;

class NonNullOnEntryTest {

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


  private @Nullable Object field;

  @NonNullOnEntry("field")
  public void requiresNonNullField() {
  }

  public static void client(NonNullOnEntryTest arg) {
    // XXX TODO FIXME: //:: precondition.not.satisfied
    arg.requiresNonNullField();
  }

}
