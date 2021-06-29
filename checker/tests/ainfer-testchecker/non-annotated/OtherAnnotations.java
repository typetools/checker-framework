// Test that having other, unrelated annotations on fields/methods/etc doesn't foul up inference.

import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;
import org.checkerframework.common.aliasing.qual.Unique;

public class OtherAnnotations {

  void requireSibling1(@Sibling1 int a) {}

  @Unique int x;

  void assignX(@Sibling1 int y) {
    x = y;
  }

  void useX() {
    // :: warning: argument
    requireSibling1(x);
  }

  void methodWithAnnotatedParam(@Unique int z) {
    // :: warning: argument
    requireSibling1(z);
  }

  void useMethodWithAnnotatedParam(@Sibling1 int w) {
    methodWithAnnotatedParam(w);
  }

  @Sibling1 int getSibling1() {
    return 5;
  }

  @Unique int getIntVal5() {
    return getSibling1();
  }

  void useGetIntVal5() {
    // :: warning: argument
    requireSibling1(getIntVal5());
  }
}
