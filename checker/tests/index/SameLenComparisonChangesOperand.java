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

  // After the assignment, s + "x" is one character longer than t.
  void changedInAssignment(String s, String t) {
    if ((s = s + "x").length() == t.length()) {
      String u = s + "x";
      for (int k = 0; k < u.length(); k++) {
        // :: error: (argument)
        t.charAt(k);
      }
    }
  }

  void unchangedInAssignment(String s, String t, String v) {
    if ((s = v + "x").length() == t.length()) {
      String u = v + "x";
      for (int k = 0; k < u.length(); k++) {
        t.charAt(k);
      }
    }
  }

  static class Holder {
    String text = "";
  }

  static Holder current = new Holder();

  static class Swapper {
    @Override
    public String toString() {
      current = new Holder();
      return "";
    }
  }

  // The `+=` evaluates `current` before it converts `sw` to a string, and that conversion
  // reassigns `current`.  So the assignment stores into the `text` field of the old `current`.
  void stringConversionInCompoundAssignmentReassignsReceiver(String peer, Swapper sw) {
    if ((current.text += sw).length() == peer.length()) {
      for (int k = 0; k < peer.length(); k++) {
        // :: error: (argument)
        current.text.charAt(k);
      }
    }
  }

  static class Box {
    String text = "";

    @Override
    public String toString() {
      return text;
    }
  }

  // Not side-effect-free.
  static String setText(Box box, String t) {
    box.text = t;
    return t;
  }

  // The string conversion of `box` calls `box.toString()`, whose result the call to `setText`
  // changes.  So after the comparison, "p" + box might be longer than s.
  void changedByToString(Box box, String s) {
    if (("p" + box).length() == (s = setText(box, "abc")).length()) {
      String u = "p" + box;
      for (int k = 0; k < u.length(); k++) {
        // :: error: (argument)
        s.charAt(k);
      }
    }
  }

  void unchangedByToString(Box box, String s, String t) {
    if (("p" + box).length() == (s = t).length()) {
      String u = "p" + box;
      for (int k = 0; k < u.length(); k++) {
        s.charAt(k);
      }
    }
  }
}
