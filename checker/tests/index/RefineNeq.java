import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

public class RefineNeq {
  int[] arr = {1};

  void testLTL(@LTLengthOf("arr") int test) {
    // :: error: (assignment)
    @LTLengthOf("arr") int a = Integer.parseInt("1");

    int b = 1;
    if (test != b) {
      // :: error: (assignment)
      @LTLengthOf("arr") int e = b;

    } else {

      @LTLengthOf("arr") int c = b;
    }
    // :: error: (assignment)
    @LTLengthOf("arr") int d = b;
  }

  void testLTEL(@LTEqLengthOf("arr") int test) {
    // :: error: (assignment)
    @LTEqLengthOf("arr") int a = Integer.parseInt("1");

    int b = 1;
    if (test != b) {
      // :: error: (assignment)
      @LTEqLengthOf("arr") int e = b;
    } else {
      @LTEqLengthOf("arr") int c = b;

      @LTLengthOf("arr") int g = b;
    }
    // :: error: (assignment)
    @LTEqLengthOf("arr") int d = b;
  }
}
