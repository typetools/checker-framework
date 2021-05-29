// Tests using the index returned from String.indexOf

public class StringIndexOf {

  public static String remove(String l, String s) {
    int i = l.indexOf(s);
    if (i != -1) {
      return l.substring(0, i) + l.substring(i + s.length());
    }
    return l;
  }

  public static String nocheck(String l, String s) {
    int i = l.indexOf(s);
    // :: error: (argument)
    return l.substring(0, i) + l.substring(i + s.length());
  }

  public static String remove(String l, String s, int from, boolean last) {
    int i = last ? l.lastIndexOf(s, from) : l.indexOf(s, from);
    if (i >= 0) {
      return l.substring(0, i) + l.substring(i + s.length());
    }
    return l;
  }

  public static String stringLiteral(String l) {
    int i = l.indexOf("constant");
    if (i != -1) {
      return l.substring(0, i) + l.substring(i + "constant".length());
    }
    // :: error: (argument)
    return l.substring(0, i) + l.substring(i + "constant".length());
  }

  public static char character(String l, char c) {
    int i = l.indexOf(c);
    if (i > -1) {
      return l.charAt(i);
    }
    // :: error: (argument)
    return l.charAt(i);
  }
}
