import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

public class Conditions {

  @Nullable Object f;

  void test1(Conditions c) {
    if (!(c.f != null)) {
      return;
    }
    c.f.hashCode();
  }

  void test2(Conditions c) {
    if (!(c.f != null) || 5 > 9) {
      return;
    }
    c.f.hashCode();
  }

  @EnsuresNonNullIf(expression = "f", result = true)
  public boolean isNN() {
    return (f != null);
  }

  void test1m(Conditions c) {
    if (!(c.isNN())) {
      return;
    }
    c.f.hashCode();
  }

  void test2m(Conditions c) {
    if (!(c.isNN()) || 5 > 9) {
      return;
    }
    c.f.hashCode();
  }
}
