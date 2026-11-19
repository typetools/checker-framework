// A method call whose receiver is a binary operation must not be confused with the binary
// operation whose right operand is the method call:  (a + b).trim() and a + b.trim() can differ in
// length.

public class SameLenMethodCallOnBinaryOperation {

  void differentGrouping(String a, String b) {
    String s = (a + b).trim();
    String u = a + b.trim();
    for (int k = 0; k < s.length(); k++) {
      // :: error: (argument)
      u.charAt(k);
    }
  }

  void sameGrouping(String a, String b) {
    String s = (a + b).trim();
    String u = (a + b).trim();
    for (int k = 0; k < s.length(); k++) {
      u.charAt(k);
    }
  }
}
