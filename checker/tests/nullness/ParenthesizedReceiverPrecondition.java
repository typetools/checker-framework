// Test that a precondition is checked against a parenthesized receiver, as in `(c).m()`.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class ParenthesizedReceiverPrecondition {

  @Nullable Object f;

  @RequiresNonNull("f")
  void m() {}

  static void satisfied(ParenthesizedReceiverPrecondition c) {
    if (c.f != null) {
      (c).m();
      ((c)).m();
    }
  }

  static void unsatisfied(ParenthesizedReceiverPrecondition c) {
    // :: error: [contracts.precondition]
    (c).m();
  }
}
