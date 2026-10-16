// This test ensures that overloaded methods with different return types aren't confused.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class OverloadedMethodsTest {

  String f;

  String m1() {
    return this.f;
  }

  String m1(String x) {
    return getAinferSibling1();
  }

  @AinferSibling1 String getAinferSibling1() {
    return null;
  }
}
