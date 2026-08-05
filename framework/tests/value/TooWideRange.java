// test case for https://github.com/typetools/checker-framework/issues/5486

public class TooWideRange {
  // From StringsPlume
  @org.checkerframework.dataflow.qual.Pure
  public static @org.checkerframework.common.value.qual.IntRange(
      from = -2147483648,
      to = 2147483647) int count(String s, int ch) {
    int result = 0;
    int pos = s.indexOf(ch);
    while (pos > -1) {
      result++;
      pos = s.indexOf(ch, pos + 1);
    }
    return result;
  }

  // From ArraysPlume
  @org.checkerframework.dataflow.qual.Pure
  public static @org.checkerframework.common.value.qual.IntRange(
      from = -2147483648,
      to = 2147483647) int indexOfEq(Object[] a, Object elt) {
    for (int i = 0; i < a.length; i++) {
      if (elt == a[i]) {
        return i;
      }
    }
    return -1;
  }
}
