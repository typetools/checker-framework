// @skip-test until fixed.

public class CompareBySubtraction {
  public int compare(int[] a1, int[] a2) {
    if (a1 == a2) {
      return 0;
    }
    int tmp;
    tmp = a1.length - a2.length;
    if (tmp != 0) {
      return tmp;
    }
    for (int i = 0; i < a1.length; i++) {
      if (a1[i] != a2[i]) {
        return ((a1[i] > a2[i]) ? 1 : -1);
      }
    }
    return 0;
  }
}
