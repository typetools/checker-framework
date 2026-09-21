import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.DoesNotUnrefineReceiver;

public class TestDoesNotUnrefine {
  static class MyClass {

    @DoesNotUnrefineReceiver("tainting")
    String doesNotUnrefine() {
      return "";
    }

    @DoesNotUnrefineReceiver("allcheckers")
    String doesNotUnrefineAllCheckers() {
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

    field = untainted;
    field.doesNotUnrefineAllCheckers();
    @Untainted MyClass anotherLocal3 = field;

    field.doesUnrefine();
    // :: error: [assignment]
    @Untainted MyClass anotherLocal2 = field;
  }
}
