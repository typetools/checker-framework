package daikon.simplify;

import utilMDE.Assert;

/**
 * Utility functions for the simplify package.
 **/
public class SimpUtil
{
  private SimpUtil() { throw new Error("do not instantiate"); }

  public static void assert_well_formed(String s) {
    if (!Assert.enabled) {
      return;
    }

    // Unfortunately, most of the tests here aren't sensible if the
    // string can contain quoted strings (like |))|). To do this
    // right, the paren counter would also have to be smarter about
    // details like the rules for | delimiting strings, and how it can
    // be escaped.

    Assert.assertTrue(s != null);
    // XXX not with strings
//     if (s.indexOf("((") != -1)
//       Assert.assertTrue(false, "'((' may not appear, '" + s + "'");
    Assert.assertTrue(s.length() >= 4, "too short, '" + s + "'");
    if (s.charAt(0) != '(')
      Assert.assertTrue(false, "starts with lparen, '" + s + "'");
    if (s.charAt(s.length()-1) != ')')
      Assert.assertTrue(false, "ends with rparen, '" + s + "'");

    int paren = 0;
    char[] cs = s.toCharArray();
    for (int i=0; i < cs.length; i++) {
      char c = cs[i];
      if (c == '(') {
        paren++;
      } else if (c == ')') {
        // XXX not with strings
//         if (paren <= 0)
//           Assert.assertTrue(paren > 0,
//                             "too deep at char " + i + " in '" + s + "'");
        paren--;
        // This check is only sensible for some callers; it needs a flag.
//         if (paren == 0 && i < cs.length -1)
//           Assert.assertTrue(false, "multiple SEXPs in " + s);
      }
    }
    // XXX not with strings
//     if (paren != 0)
//       Assert.assertTrue(paren == 0, "unbalanced parens in '" + s + "'");
  }

  /**
   * Represent a Java long integer as an uninterpreted function
   * applied to 6 moderately sized integers, to work around Simplify's
   * numeric limitations. The first integer is a sign, and the rest
   * are 13-bit (base 8192) limbs in order from most to least
   * significant.
   **/
  public static String formatInteger(long i) {
    int sign;
    int[] limbs = new int[5]; // limbs[0] is most significant
    if (i == 0) {
      sign = limbs[0] = limbs[1] = limbs[2] = limbs[3] = limbs[4] = 0;
    } else if (i == Long.MIN_VALUE) {
      sign = -1;
      limbs[0] = 2048;
      limbs[1] = limbs[2] = limbs[3] = limbs[4] = 0;
    } else {
      sign = 1;
      if (i < 0) {
        sign = -1;
        i = -i;
      }
      for (int j = 4; j >= 0; j--) {
        limbs[j] = (int)(i % 8192);
        i /= 8192;
      }
    }
    return "(|long-int| " + sign + " " + limbs[0] + " " + limbs[1] + " "
      + limbs[2] + " " + limbs[3] + " " + limbs[4] + ")";
  }
}
