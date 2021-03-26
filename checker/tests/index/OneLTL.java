public class OneLTL {
  public static boolean sorted(int[] a) {
    for (int i = 0; i < a.length - 1; i++) {
      if (a[i + 1] < a[i]) {
        return false;
      }
    }
    return true;
  }
}
