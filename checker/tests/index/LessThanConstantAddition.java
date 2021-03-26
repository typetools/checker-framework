// Test case for issue #2541: https://github.com/typetools/checker-framework/issues/2541

public class LessThanConstantAddition {

  public static void checkedPow(int b) {
    if (b <= 2) {
      int c = (int) b;
    }
  }
}
