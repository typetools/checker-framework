import org.checkerframework.framework.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class NewObjectNonNull {
  @DefaultQualifier(Nullable.class)
  class A {
    A() {}
  }

  void m() {
    new A().toString();
  }
}
