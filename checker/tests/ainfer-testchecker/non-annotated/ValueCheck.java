// Checks that annotations from the Value Checker (which is a subchecker of the WPI test checker)
// are actually present in the generated files, even when there is also an annotation from the main
// checker.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.common.value.qual.IntVal;

public class ValueCheck {

  // return value should be @AinferSibling1 @IntVal(5) int
  int getAinferSibling1withValue5() {
    return ((@AinferSibling1 int) 5);
  }

  void requireAinferSibling1(@AinferSibling1 int x) {}

  void requireIntVal5(@IntVal(5) int x) {}

  void test() {
    int x = getAinferSibling1withValue5();
    // :: warning: argument
    requireAinferSibling1(x);
    // :: warning: argument
    requireIntVal5(x);
  }
}
