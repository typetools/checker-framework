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

    //:: (field.not.found.nullness.parse.error)
    @NonNullOnEntry("xxx") void test2() {
      //:: (dereference.of.nullable)
      nullable.toString();
    }
  }

  class NNOESeparate {
    void call1(NNOEMain p) {
      //:: (nonnullonentry.precondition.not.satisfied)
      p.test1();

      Object xxx = new Object();
      //:: (nullness.parse.error)
      p.test2();
    }

    void call2(NNOEMain p) {
      p.nullable = "";
      p.test1();
    }
  }

}
