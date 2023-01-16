// This test makes sure that if a class has two methods with the same name,
// the parameters are inferred correctly and are not confused.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class TwoMethodsSameName {

  void test(int x, int y) {
    // :: warning: (assignment)
    @AinferSibling1 int x1 = x;
    // :: warning: (assignment)
    @AinferSibling2 int y1 = y;
  }

  void test(int z) {
    // :: warning: (assignment)
    @AinferSibling2 int z1 = z;
  }

  void uses(@AinferSibling1 int a, @AinferSibling2 int b) {
    test(a, b);
    test(b);
  }
}
