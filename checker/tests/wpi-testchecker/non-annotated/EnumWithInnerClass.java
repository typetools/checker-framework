// This test ensures that enums with inner classes are printed properly to avoid crashing the stub
// parser, which was a problem with an earlier version of stub-based WPI.

import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;

enum EnumWithInnerClass {
  CONSTANT;

  private static class MyInnerClass {
    int getSibling1() {
      return (@Sibling1 int) 0;
    }

    void requireSibling1(@Sibling1 int x) {}

    void test() {
      // :: warning: argument.type.incompatible
      requireSibling1(getSibling1());
    }
  }
}
