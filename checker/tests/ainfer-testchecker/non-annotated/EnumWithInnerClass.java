// This test ensures that enums with inner classes are printed properly to avoid crashing the stub
// parser, which was a problem with an earlier version of stub-based WPI.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

enum EnumWithInnerClass {
  CONSTANT;

  private static class MyInnerClass {
    int getAinferSibling1() {
      return (@AinferSibling1 int) 0;
    }

    void requireAinferSibling1(@AinferSibling1 int x) {}

    void test() {
      // :: warning: argument
      requireAinferSibling1(getAinferSibling1());
    }
  }
}
