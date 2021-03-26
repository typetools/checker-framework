// Test case for Issue #803
// https://github.com/typetools/checker-framework/issues/803
// @skip-test

import org.checkerframework.checker.nullness.qual.*;

interface GenFunc {
  <T extends @Nullable Number, U extends @Nullable Number> T apply(U u);
}

interface GenFunc2 {
  <T extends @Nullable Number, U extends @NonNull Number> T apply(U u);
}

class TestGenFunc {
  static <V extends @NonNull Number, P extends @Nullable Number> V apply(P u) {
    throw new RuntimeException("");
  }

  void context() {
    GenFunc f = TestGenFunc::apply;
    // :: error: (methodref.param.invalid)
    GenFunc2 f2 = TestGenFunc::apply;
  }
}
