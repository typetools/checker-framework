import checkers.nullness.quals.*;

class NonNullOnEntryTest {

  @Nullable Object field1;
  @Nullable Object field2;

  @NonNullOnEntry("field1")
  void method1() {
    field1.toString(); // OK, field1 is known to be non-null
    this.field1.toString(); // OK, field1 is known to be non-null
    //:: error: (dereference.of.nullable)
    field2.toString(); // error, might throw NullPointerException
  }

  @NonNullOnEntry("field1")
  void method1also() {
    // ok, precondition satisfied by NNOE
    method1();
  }

  void method2() {
    field1 = new Object();
    method1(); // OK, satisfies method precondition
    field1 = null;
    //:: error: (nonnullonentry.precondition.not.satisfied)
    method1(); // error, does not satisfy method precondition
  }

  protected @Nullable Object field;

  @NonNullOnEntry("field")
  public void requiresNonNullField() {}

  public void clientFail(NonNullOnEntryTest arg1) {
    //:: error: (nonnullonentry.precondition.not.satisfied)
    arg1.requiresNonNullField();
  }

  public void clientOK(NonNullOnEntryTest arg2) {
    arg2.field = new Object();
    // note that the following line works
    @NonNull Object o = arg2.field;

    arg2.requiresNonNullField(); // OK, field is known to be non-null
  }

  // TODO: forbid the field in @NNOE to be less visible than the method

  protected static @Nullable Object staticfield;

  @NonNullOnEntry("staticfield")
  public void reqStaticName() {
    reqStaticQualName();
  }

  @NonNullOnEntry("NonNullOnEntryTest.staticfield")
  public void reqStaticQualName() {
    reqStaticName();
  }

  public void statClientOK(NonNullOnEntryTest arg1) {
    staticfield = new Object();
    arg1.reqStaticName();

    staticfield = new Object();
    arg1.reqStaticQualName();

    NonNullOnEntryTest.staticfield = new Object();
    arg1.reqStaticName();
    NonNullOnEntryTest.staticfield = new Object();
    arg1.reqStaticQualName();

  }

  public void statClientFail(NonNullOnEntryTest arg1) {
    //:: error: (nonnullonentry.precondition.not.satisfied)
    arg1.reqStaticName();
    //:: error: (nonnullonentry.precondition.not.satisfied)
    arg1.reqStaticQualName();
  }



  class NNOESubTest extends NonNullOnEntryTest {
    public void subClientOK(NNOESubTest arg3) {
      arg3.field = new Object();
      arg3.requiresNonNullField();
    }

    public void subClientFail(NNOESubTest arg4) {
      //:: error: (nonnullonentry.precondition.not.satisfied)
      arg4.requiresNonNullField();
    }

    public void subStat(NNOESubTest arg5) {
      NonNullOnEntryTest.staticfield = new Object();
      arg5.reqStaticQualName();

      staticfield = new Object();
      arg5.reqStaticQualName();

      NNOESubTest.staticfield = new Object();
      arg5.reqStaticQualName();
    }
  }

  private @Nullable Object notHidden;

  class NNOEHidingTest extends NonNullOnEntryTest {

    protected @Nullable String field;

    public void hidingClient1(NNOEHidingTest arg5) {
      arg5.field = "ha!";
      /* We should be testing that the Object "field" from the superclass
       * is non-null. We currently only match on the field name and do not
       * handle hiding correctly. Instead, we output an error, if we
       * detect that hiding happened.
       * TODO: correctly resolve hidden fields.
       */
      //:: error: (nonnull.hiding.violated)
      arg5.requiresNonNullField();
    }

    public void hidingClient2(NNOEHidingTest arg6) {
      // We also would get an (nonnullonentry.precondition.not.satisfied), but
      // this error wins.
      //:: error: (nonnull.hiding.violated)
      arg6.requiresNonNullField();
    }

    // @skip-test: 
//    // the field in the super class is also visible
//    //:: error: (nonnull.hiding.violated)
//    @NonNullOnEntry("field") void hiddenTest() {}


    protected @Nullable Object notHidden;
    @NonNullOnEntry("notHidden")
    void notHiddenTest() {
      // the field in the superclass is private -> don't complain about hiding
    }

  }

  static @Nullable Object o = "m";
  @NonNullOnEntry("o")
  void test() {
      o = null;
  }
}
