// Test case for issue 3668:
// https://github.com/typetools/checker-framework/issues/3669

public class CompareChars {
    void compareUnsignedChars(char c2) {
        char c1 = 'a';
        boolean res = c1 > c2;
        res = c1 >= c2;
        res = c1 < c2;
        res = c1 <= c2;
    }
}
