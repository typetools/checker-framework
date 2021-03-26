import org.checkerframework.checker.index.qual.*;

public class PlusPlusBug {
  int[] array = {};

  void test(@LTLengthOf("array") int x) {
    // :: error: (unary.increment.type.incompatible)
    x++;
    // :: error: (unary.increment.type.incompatible)
    ++x;
    // :: error: (assignment.type.incompatible)
    x = x + 1;
  }
}
