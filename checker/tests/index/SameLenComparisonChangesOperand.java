// An operand of a length comparison must not be related to the other operand if the comparison
// changes the operand's value after evaluating it.  For example, if i is 12, then after the
// comparison in changedLater, s is "ab3" but "x" + i is "x3".

import org.checkerframework.dataflow.qual.Pure;

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

  @Pure
  static int[] keepSecond(int[] x, int[] y) {
    return y;
  }

  // The type of a is @SameLen("c") when a is evaluated, but the right operand then reassigns c.
  void staleNameInType(int[] a, int[] b) {
    int[] c = a;
    if (a.length == keepSecond(c = new int[0], b).length) {
      for (int k = 0; k < a.length; k++) {
        // :: error: (array.access.unsafe.high)
        c[k] = 0;
      }
    }
  }

  void unchangedNameInType(int[] a, int[] b, int[] d) {
    int[] c = a;
    if (a.length == keepSecond(d = new int[0], b).length) {
      for (int k = 0; k < a.length; k++) {
        c[k] = 0;
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
}
