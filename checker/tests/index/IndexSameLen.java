import org.checkerframework.checker.index.qual.*;

public class IndexSameLen {

  public static void bug2() {
    int[] a = {1, 2, 3, 4, 5};
    int @SameLen("a") [] b = a;

    @IndexFor("a") int i = 2;
    a[i] = b[i];

    for (int j = 0; j < a.length; j++) {
      a[j] = b[j];
    }
  }
}
