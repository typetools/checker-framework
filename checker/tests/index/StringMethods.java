// Tests for index annotations on string methods in the annotated JDK

public class StringMethods {

  void testCharAt(String s, int i) {
    // ::  error: (argument)
    s.charAt(i);
    // ::  error: (argument)
    s.codePointAt(i);

    if (i >= 0 && i < s.length()) {
      s.charAt(i);
      s.codePointAt(i);
    }
  }

  void testCodePointBefore(String s) {
    // ::  error: (argument)
    s.codePointBefore(0);

    if (s.length() > 0) {
      s.codePointBefore(s.length());
    }
  }

  void testSubstring(String s) {
    s.substring(0);
    s.substring(0, 0);
    s.substring(s.length());
    s.substring(s.length(), s.length());
    s.substring(0, s.length());
    // ::  error: (argument)
    s.substring(1);
    // ::  error: (argument)
    s.substring(0, 1);
  }

  void testIndexOf(String s, char c) {
    int i = s.indexOf(c);
    if (i != -1) {
      s.charAt(i);
    }
  }
}
