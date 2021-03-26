public class IndexByChar {
  public int m(char c) {
    int[] i = new int[128];
    if (c < 128) {
      return i[c];
    } else {
      return -1;
    }
  }
}
