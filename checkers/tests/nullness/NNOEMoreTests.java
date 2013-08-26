import checkers.nullness.quals.*;

class NNOEMoreTests {
  class NNOEMain {
    protected @Nullable
    String nullable = null;
    @Nullable
    String otherNullable = null;

    @NonNullOnEntry("nullable")
    void test1() {
      nullable.toString();
    }

    //:: error: (field.not.found.nullness.parse.error)
    @NonNullOnEntry("xxx") void test2() {
      //:: error: (dereference.of.nullable)
      nullable.toString();
    }
  }

  class NNOESeparate {
    void call1(NNOEMain p) {
      //:: error: (nonnullonentry.precondition.not.satisfied)
      p.test1();

      Object xxx = new Object();
      //:: error: (nullness.parse.error)
      p.test2();
    }

    void call2(NNOEMain p) {
      p.nullable = "";
      p.test1();
    }
  }
  
  @Nullable Object field1;
  
  @NonNullOnEntry("field1")
  void methWithIf1() {
      if (5 < 99) {
      } else {
          field1.hashCode();
      }
  }
  
}
