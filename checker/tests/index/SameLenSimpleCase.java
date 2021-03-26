public class SameLenSimpleCase {
  public int compare(int[] a1, int[] a2) {
    if (a1.length != a2.length) {
      return a1.length - a2.length;
    }
    for (int i = 0; i < a1.length; i++) {
      if (a1[i] != a2[i]) {
        return ((a1[i] > a2[i]) ? 1 : -1);
      }
    }
    return 0;
  }
}
