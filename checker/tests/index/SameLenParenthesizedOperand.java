// A parenthesized operand must not be confused with the same operands grouped differently:
// "x" + (i + j) and "x" + i + j can differ in length.

public class SameLenParenthesizedOperand {

  void binary(int i, int j) {
    String s = "x" + (i + j);
    String u = "x" + i + j;
    for (int k = 0; k < s.length(); k++) {
      // :: error: (argument)
      u.charAt(k);
    }
  }

  void unary(int i, int j) {
    String s = "x" + -(i + j);
    String u = "x" + -i + j;
    for (int k = 0; k < s.length(); k++) {
      // :: error: (argument)
      u.charAt(k);
    }
  }

  void sameGrouping(int i, int j) {
    String s = "x" + (i + j);
    String u = "x" + (i + j);
    for (int k = 0; k < s.length(); k++) {
      u.charAt(k);
    }
  }
}
