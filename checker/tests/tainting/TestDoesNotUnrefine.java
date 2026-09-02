import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.DoesNotUnrefineReceiver;

public class TestDoesNotUnrefine {
  static class MyClass {

    @DoesNotUnrefineReceiver("tainting")
    String doesNotUnrefine() {
      return "";
    }

    String doesUnrefine() {
      return "";
    }
  }

  MyClass field;

  void test(@Untainted MyClass untainted) {
    field = untainted;
    field.doesNotUnrefine();
    @Untainted MyClass anotherLocal = field;

    field.doesUnrefine();
    // :: error: [assignment]
    @Untainted MyClass anotherLocal2 = field;
  }
}
