import org.checkerframework.framework.testchecker.javaexpression.qual.FlowExp;

public class Constructor {

  @SuppressWarnings({"inconsistent.constructor.type", "super.invocation"})
  static class MyClass {
    String field;

    @FlowExp("field") MyClass() {}
  }

  static class MyClass2 {
    String field;
    String field2;

    @SuppressWarnings("cast.unsafe.constructor.invocation")
    void method() {
      // TODO: This should be an error.
      MyClass c = new @FlowExp("field") MyClass();
      // :: error: (expression.unparsable) :: error: (constructor.invocation)
      MyClass c2 = new @FlowExp("bad") MyClass();
      // :: error: (constructor.invocation)
      MyClass c3 = new @FlowExp("field2") MyClass();
    }
  }
}
