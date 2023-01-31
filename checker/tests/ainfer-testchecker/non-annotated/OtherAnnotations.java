// Test that having other, unrelated annotations on fields/methods/etc doesn't foul up inference.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.common.aliasing.qual.Unique;

public class OtherAnnotations {

  void requireAinferSibling1(@AinferSibling1 int a) {}

  @Unique int x;

  void assignX(@AinferSibling1 int y) {
    x = y;
  }

  void useX() {
    // :: warning: (argument)
    requireAinferSibling1(x);
  }

  void methodWithAnnotatedParam(@Unique int z) {
    // :: warning: (argument)
    requireAinferSibling1(z);
  }

  void useMethodWithAnnotatedParam(@AinferSibling1 int w) {
    methodWithAnnotatedParam(w);
  }

  @AinferSibling1 int getAinferSibling1() {
    return 5;
  }

  @Unique int getIntVal5() {
    return getAinferSibling1();
  }

  void useGetIntVal5() {
    // :: warning: (argument)
    requireAinferSibling1(getIntVal5());
  }
}
