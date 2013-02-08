import checkers.quals.*;
import checkers.nonnull.quals.*;

class NewObjectNonNull {
  @DefaultQualifier(Nullable.class)
  class A {
    A() {}
  }

  void m() {
    new A().toString();
  }
}
