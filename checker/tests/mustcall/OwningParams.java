// Tests that parameters marked as @Owning are still checked.

import org.checkerframework.checker.mustcall.qual.*;

class OwningParams {
  static void o1(@Owning OwningParams o) {}

  void test(@Owning @MustCall({"a"}) OwningParams o, @Owning OwningParams p) {
    // :: error: argument
    o1(o);
    o1(p);
  }
}
