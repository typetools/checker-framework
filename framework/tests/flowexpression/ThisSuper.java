// Test case for
// https://github.com/typetools/checker-framework/issues/152
import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

// @skip-test
public class ThisSuper {
  static class SuperClass {
    protected final Object field = new Object();

    private @FlowExp("field") Object superField;
  }

  static class SubClass extends SuperClass {
    /* Hides SuperClass.field */
    private final Object field = new Object();

    private @FlowExp("field") Object subField;

    void method() {
      // super.superField : @FlowExp("super.field")
      // this.subField : @FlowExp("this.field")
      // :: error: (assignment)
      this.subField = super.superField;
      // :: error: (assignment)
      super.superField = this.subField;

      @FlowExp("super.field") Object o1 = super.superField;
      @FlowExp("this.field") Object o2 = this.subField;
    }
  }

  class OuterClass {
    private final Object lock = new Object();

    @FlowExp("this.lock") Object field;

    class InnerClass {
      private final Object lock = new Object();
      // :: error: (assignment)
      @FlowExp("this.lock") Object field2 = field;
    }
  }
}
