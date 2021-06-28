// Checks that annotations from the Value Checker (which is a subchecker of the WPI test checker)
// are actually present in the generated files, even when there is also an annotation from the main
// checker.

import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;
import org.checkerframework.common.value.qual.IntVal;

public class ValueCheck {

  // return value should be @Sibling1 @IntVal(5) int
  int getSibling1withValue5() {
    return ((@Sibling1 int) 5);
  }

  void requireSibling1(@Sibling1 int x) {}

  void requireIntVal5(@IntVal(5) int x) {}

  void test() {
    int x = getSibling1withValue5();
    // :: warning: argument
    requireSibling1(x);
    // :: warning: argument
    requireIntVal5(x);
  }
}
