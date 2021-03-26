import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.BottomVal;

public class NegativeArrayLen {
  void test1() {
    int @BottomVal [] a = new int[-1];
    int @BottomVal [] b = new int[Integer.MAX_VALUE + 1];
    // :: warning: (negative.arraylen)
    int @ArrayLen(-1) [] c = new int[-1];
  }

  void test2(
      // :: warning: (negative.arraylen)
      int @ArrayLen(Integer.MIN_VALUE) [] a,
      // :: warning: (negative.arraylen)
      int @ArrayLen({-1, 2, 0}) [] b) {
    int @BottomVal [] y = a;
    int @BottomVal [] x = b;
  }
}
