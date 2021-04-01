// This tests normally tests that parameters (including receiver parameters) marked as @Owning are
// still checked.
// This version is modified for -AnoLightweightOwnership to expect the opposite behavior.

import org.checkerframework.checker.mustcall.qual.*;

class OwningParams {
  static void o1(@Owning OwningParams o) {}

  void test(@Owning @MustCall({"a"}) OwningParams o) {
    o1(o);
  }
}
