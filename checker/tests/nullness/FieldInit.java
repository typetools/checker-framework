import org.checkerframework.checker.nullness.qual.*;

public class FieldInit {
  // :: error: (argument) :: error: (method.invocation)
  String f = init(this);

  String init(FieldInit o) {
    return "";
  }

  void test() {
    String local = init(this);
  }
}
