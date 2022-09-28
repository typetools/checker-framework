// Tests that adding an @SameLen annotation to a primitive type is still
// an error.

import org.checkerframework.checker.index.qual.SameLen;

public class SameLenIrrelevant {
  // :: error: anno.on.irrelevant
  public void test(@SameLen("#2") int x, int y) {
    // do nothing
  }

  // :: error: anno.on.irrelevant
  public void test(@SameLen("#2") double x, double y) {
    // do nothing
  }

  // :: error: anno.on.irrelevant
  public void test(@SameLen("#2") char x, char y) {
    // do nothing
  }
}
