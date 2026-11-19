// The right-hand side of an assignment in a length comparison must not be related to the other
// operand if the assignment changes the right-hand side's value.  For example, after the
// comparison in changedByAssignment, s + "x" is one character longer than t.

public class SameLenAssignmentChangesRhs {

  void changedByAssignment(String s, String t) {
    if ((s = s + "x").length() == t.length()) {
      String u = s + "x";
      for (int k = 0; k < u.length(); k++) {
        // :: error: (argument)
        t.charAt(k);
      }
    }
  }

  void unchanged(String s, String t, String v) {
    if ((s = v + "x").length() == t.length()) {
      String u = v + "x";
      for (int k = 0; k < u.length(); k++) {
        t.charAt(k);
      }
    }
  }
}
