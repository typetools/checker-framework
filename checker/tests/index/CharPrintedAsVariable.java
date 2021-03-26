// Test case for https://github.com/typetools/checker-framework/issues/3167 .

public class CharPrintedAsVariable {
  void m1(char c) {
    if (c <= 'A') {
      int x = (int) c;
    }
  }

  void m2(char c) {
    if (c <= '\377') {
      int x = (int) c;
    }
  }
}
