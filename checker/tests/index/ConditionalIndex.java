// test case for issue 162: https://github.com/kelloggm/checker-framework/issues/162

public class ConditionalIndex {
  public void f(boolean cond) {
    int[] a = new int[10];
    int[] b = new int[1];
    if (cond) {
      int[] c = a;
    } else {
      int[] c = b;
    }

    int[] d = (cond ? a : b);
  }
}
