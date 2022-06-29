// This test makes sure that if a class has two methods with the same name,
// the parameters are inferred correctly and are not confused.

import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling2;

public class TwoMethodsSameName {

  void test(int x, int y) {
    // :: warning: assignment
    @Sibling1 int x1 = x;
    // :: warning: assignment
    @Sibling2 int y1 = y;
  }

  void test(int z) {
    // :: warning: assignment
    @Sibling2 int z1 = z;
  }

  void uses(@Sibling1 int a, @Sibling2 int b) {
    test(a, b);
    test(b);
  }
}
