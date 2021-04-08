import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.DefaultQualifier;

public class NewObjectNonNull {
  @DefaultQualifier(Nullable.class)
  class A {
    A() {}
  }

  void m() {
    new A().toString();
  }
}
