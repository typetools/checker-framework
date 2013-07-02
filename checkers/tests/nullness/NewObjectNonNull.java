import checkers.quals.*;
import checkers.nullness.quals.*;

class NewObjectNonNull {
  @DefaultQualifier(Nullable.class)
  class A {
    A() {}
  }

  void m() {
    new A().toString();
  }
}
