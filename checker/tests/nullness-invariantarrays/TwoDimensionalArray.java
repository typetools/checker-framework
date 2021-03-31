// @skip-test
// Test case for issue 440: https://github.com/typetools/checker-framework/issues/440

public class TwoDimensionalArray {

  public static void main(String[] args) {
    assert any_null(new Object[][] {null}) == true;
  }

  public static boolean any_null(Object[] a) {
    return true;
  }
}
