// The right-hand side of an assignment must not be related to the other operand of a length
// comparison if the assignment changes the right-hand side's value.  For example, if s is "" and
// t is "y", then after the comparison in changedByAssignment, s is "x" and s + "x" is "xx", so
// s + "x" is longer than t.

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

  void changedByAssignmentInCast(String s, String t) {
    if (((String) (s = s + "x")).length() == t.length()) {
      String u = s + "x";
      for (int k = 0; k < t.length(); k++) {
        // :: error: (argument)
        u.charAt(k);
      }
    }
  }

  void unchangedInCast(String s, String t, String v) {
    if (((String) (v = s + "x")).length() == t.length()) {
      String u = s + "x";
      for (int k = 0; k < t.length(); k++) {
        u.charAt(k);
      }
    }
  }
}
