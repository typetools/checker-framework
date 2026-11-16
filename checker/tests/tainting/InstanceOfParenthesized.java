// Test that `(e) instanceof @Untainted T` refines `e`.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class InstanceOfParenthesized {

  @Tainted String f;

  static void isUntainted(@Untainted String a) {}

  void parenthesized(@Tainted String a) {
    // :: warning: [instanceof.unsafe]
    if ((a) instanceof @Untainted String) {
      isUntainted(a);
    }
  }

  void nested(@Tainted String a) {
    // :: warning: [instanceof.unsafe]
    if (((a)) instanceof @Untainted String) {
      isUntainted(a);
    }
  }

  void field() {
    // :: warning: [instanceof.unsafe]
    if ((this.f) instanceof @Untainted String) {
      isUntainted(f);
    }
  }

  void parenthesizedReceiver() {
    // :: warning: [instanceof.unsafe]
    if (((this).f) instanceof @Untainted String) {
      isUntainted(this.f);
    }
  }
}
