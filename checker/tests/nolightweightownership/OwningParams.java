// Tests that parameters (including receiver parameters) marked as @Owning are still checked.
// Modified for -AnoLightweightOwnership to do the opposite lol

import org.checkerframework.checker.mustcall.qual.*;

class OwningParams {
  static void o1(@Owning OwningParams o) {}

  void test(@Owning @MustCall({"a"}) OwningParams o) {
    o1(o);
  }
}
