// Test case for https://github.com/typetools/checker-framework/issues/2561

public class Issue2561 {
  public static void pr(CharSequence ch) {
    char[] newCh = new char[ch.length()];
    for (int i = 0; i < newCh.length; i++) {
      newCh[i] = ch.charAt(i);
    }
  }

  public static void pr2(CharSequence ch) {
    String b = ch.toString();
    char[] newCh = new char[b.length()];
    for (int i = 0; i < newCh.length; i++) {
      newCh[i] = ch.charAt(i);
    }
  }
}
