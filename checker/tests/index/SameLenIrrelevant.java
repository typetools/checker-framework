// Tests that adding an @SameLen annotation to a primitive type is still
// an error.

// All the errors in this test case are disabled.  They were issued when `@SameLen` was restricted
// to arrays and CharSequence, but @SameLen can be written on an arbitrary user-defined type:
// https://checkerframework.org/manual/#index-annotating-fixed-size .

import org.checkerframework.checker.index.qual.SameLen;

public class SameLenIrrelevant {
  // NO :: error: (anno.on.irrelevant)
  public void test(@SameLen("#2") int x, int y) {
    // do nothing
  }

  // NO :: error: (anno.on.irrelevant)
  public void test(@SameLen("#2") double x, double y) {
    // do nothing
  }

  // NO :: error: (anno.on.irrelevant)
  public void test(@SameLen("#2") char x, char y) {
    // do nothing
  }
}
