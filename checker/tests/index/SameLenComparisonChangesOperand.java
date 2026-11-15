// An operand of a length comparison must not be related to the other operand if the comparison
// changes the operand's value after evaluating it.  For example, if i is 12, then after the
// comparison in changedLater, s is "ab3" but "x" + i is "x3".

public class SameLenComparisonChangesOperand {

  void changedLater(int i, String s) {
    if (("x" + i).length() == (s = "ab" + (i = 3)).length()) {
      String u = "x" + i;
      for (int k = 0; k < s.length(); k++) {
        // :: error: (argument)
        u.charAt(k);
      }
    }
  }

  void unchanged(int i, String s) {
    if (("x" + i).length() == s.length()) {
      String u = "x" + i;
      for (int k = 0; k < s.length(); k++) {
        u.charAt(k);
      }
    }
  }
}
