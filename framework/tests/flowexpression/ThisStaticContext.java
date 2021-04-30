import java.util.Map;
import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class ThisStaticContext {
  public static Map<Object, Object> staticField;
  public Map<Object, Object> instanceField;

  static void staticMethod1(
      // :: error: (expression.unparsable)
      @FlowExp("this.staticField") Object p1,
      @FlowExp("ThisStaticContext.staticField") Object p2,
      @FlowExp("staticField") Object p3) {
    p2 = p3;
  }

  static void staticMethod2(
      // :: error: (expression.unparsable)
      @FlowExp("this.instanceField") Object p1,
      // :: error: (expression.unparsable)
      @FlowExp("ThisStaticContext.instanceField") Object p2,
      // :: error: (expression.unparsable)
      @FlowExp("instanceField") Object p3) {}

  void instanceMethod1(
      @FlowExp("this.staticField") Object p1,
      @FlowExp("ThisStaticContext.staticField") Object p2,
      @FlowExp("staticField") Object p3) {
    p2 = p3;
    p2 = p1;
  }

  void instanceMethod2(
      @FlowExp("this.instanceField") Object p1,
      // :: error: (expression.unparsable)
      @FlowExp("ThisStaticContext.instanceField") Object p2,
      @FlowExp("instanceField") Object p3) {
    p1 = p3;
  }
}
