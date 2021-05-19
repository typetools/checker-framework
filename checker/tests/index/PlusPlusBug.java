import org.checkerframework.checker.index.qual.*;

public class PlusPlusBug {
  int[] array = {};

  void test(@LTLengthOf("array") int x) {
    // :: error: (unary.increment)
    x++;
    // :: error: (unary.increment)
    ++x;
    // :: error: (assignment)
    x = x + 1;
  }
}
