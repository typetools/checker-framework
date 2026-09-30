// Test case for https://github.com/typetools/checker-framework/issues/2637

public class Issue2637 {
  void issue(int[] a, int[] b) {
    int i;
    for (i = 0; i < b.length && i < a.length; i++)
      ;
    for (; i < a.length; i++) {
      a[i] = 0;
    }
  }
}
