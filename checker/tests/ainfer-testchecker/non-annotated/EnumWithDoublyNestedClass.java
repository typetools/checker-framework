// This test ensures that a class nested two levels deep within an enum is printed properly.  Every
// enclosing type must be printed with the correct keyword, not just the immediately-enclosing one:
// if the outermost type is printed as "class EnumWithDoublyNestedClass" rather than as an enum,
// then the stub parser rejects the whole stub file and the inferred annotations are lost.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

enum EnumWithDoublyNestedClass {
  CONSTANT;

  private static class MyInnerClass {
    private static class MyInnerInnerClass {
      int getAinferSibling1() {
        return (@AinferSibling1 int) 0;
      }

      void requireAinferSibling1(@AinferSibling1 int x) {}

      void test() {
        // :: warning: [argument]
        requireAinferSibling1(getAinferSibling1());
      }
    }
  }
}
