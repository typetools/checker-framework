import org.checkerframework.common.value.qual.*;

public class MinLenLUB {

  public static void MinLen(int @MinLen(10) [] arg, int @MinLen(4) [] arg2) {
    int[] arr;
    if (true) {
      arr = arg;
    } else {
      arr = arg2;
    }
    // :: error: (assignment)
    int @MinLen(10) [] res = arr;
    int @MinLen(4) [] res2 = arr;
    // :: error: (assignment)
    int @BottomVal [] res3 = arr;
  }

  public static void Bottom(int @BottomVal [] arg, int @MinLen(4) [] arg2) {
    int[] arr;
    if (true) {
      arr = arg;
    } else {
      arr = arg2;
    }
    // :: error: (assignment)
    int @MinLen(10) [] res = arr;
    int @MinLen(4) [] res2 = arr;
    // :: error: (assignment)
    int @BottomVal [] res3 = arr;
  }

  public static void BothBottom(int @BottomVal [] arg, int @BottomVal [] arg2) {
    int[] arr;
    if (true) {
      arr = arg;
    } else {
      arr = arg2;
    }
    int @MinLen(10) [] res = arr;
    int @BottomVal [] res2 = arr;
  }
}
