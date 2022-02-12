// Test case for issue 3668:
// https://github.com/typetools/checker-framework/issues/3669

public class CompareChars {
  void compareUnsignedChars(char c2) {
    char c1 = 'a';
    // :: error: (comparison.unsignedrhs)
    boolean res = c1 > c2;
    // :: error: (comparison.unsignedrhs)
    res = c1 >= c2;
    // :: error: (comparison.unsignedrhs)
    res = c1 < c2;
    // :: error: (comparison.unsignedrhs)
    res = c1 <= c2;
  }
}
