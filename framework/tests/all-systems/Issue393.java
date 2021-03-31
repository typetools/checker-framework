// Test case for Issue 393
// https://github.com/typetools/checker-framework/issues/393

abstract class TypeVarTaintCheck {

  void test() {
    wrap(new Object());
  }

  abstract <T, U extends T> void wrap(U u);
}
