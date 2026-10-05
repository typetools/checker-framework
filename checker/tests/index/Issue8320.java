// Test case for https://github.com/typetools/checker-framework/issues/8320

public class Issue8320 {
  static void m(String[] a, int i) {
    if (i < a.length) {
      // :: warning: (cast.unsafe)
      char c = (char) ('a' + i);
    }
  }
}
