// Test case for https://github.com/typetools/checker-framework/issues/3146

public class Issue3146 {

  private static <T extends Comparable<T>, B extends Comparable<B>> void comparableRangeTest(
      String label, B bad, T a, T b, T c, T d) {}

  public void testComparableRange() {
    comparableRangeTest("integer", 20L, 1, 2, 3, 4);
  }
}
