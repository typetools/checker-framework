// Test that `(e) instanceof @Untainted T` refines `e`.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class InstanceOfParenthesized {

  static void isUntainted(@Untainted String a) {}

  void parenthesized(@Tainted String a) {
    // :: warning: [instanceof.unsafe]
    if ((a) instanceof @Untainted String) {
      isUntainted(a);
    }
  }
}
