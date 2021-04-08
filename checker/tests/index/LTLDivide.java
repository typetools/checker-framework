import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;

public class LTLDivide {
  int[] test(int[] array) {
    //        @LTLengthOf("array") int len = array.length / 2;
    int len = array.length / 2;
    int[] arr = new int[len];
    for (int a = 0; a < len; a++) {
      arr[a] = array[a];
    }
    return arr;
  }

  void test2(int[] array) {
    int len = array.length;
    int lenM1 = array.length - 1;
    int lenP1 = array.length + 1;
    // :: error: (assignment.type.incompatible)
    @LTLengthOf("array") int x = len / 2;
    @LTLengthOf("array") int y = lenM1 / 3;
    @LTEqLengthOf("array") int z = len / 1;
    // :: error: (assignment.type.incompatible)
    @LTLengthOf("array") int w = lenP1 / 2;
  }
}
