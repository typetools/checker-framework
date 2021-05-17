import org.checkerframework.checker.regex.qual.PolyRegex;
import org.checkerframework.checker.regex.qual.Regex;

public class PolyRegexTests {

  @Regex(0) String field1 = "abc".toString();

  public static @PolyRegex String method(@PolyRegex String s) {
    return s;
  }

  public void testRegex(@Regex String str) {
    @Regex String s = method(str);
  }

  public void testNonRegex(String str) {
    // :: error: (assignment)
    @Regex String s = method(str); // error
  }

  public void testInternRegex(@Regex String str) {
    @Regex String s = str.intern();
  }

  public void testInternNonRegex(String str) {
    // :: error: (assignment)
    @Regex String s = str.intern(); // error
  }

  public void testToStringRegex(@Regex String str) {
    @Regex String s = str.toString();
  }

  public void testToStringNonRegex(String str) {
    // :: error: (assignment)
    @Regex String s = str.toString(); // error
  }

  public @PolyRegex String testPolyRegexConcat(@PolyRegex String s1, @PolyRegex String s2) {
    return s1 + s2;
  }

  public void testPolyRegexConcatErrors(@PolyRegex String polyReg, String nonPolyReg) {
    // :: error: (assignment)
    @PolyRegex String test1 = polyReg + nonPolyReg; // error
    // :: error: (assignment)
    @PolyRegex String test2 = nonPolyReg + polyReg; // error
    // :: error: (assignment)
    @PolyRegex String test3 = nonPolyReg + nonPolyReg; // error
  }

  public void testRegexPolyRegexConcat(@PolyRegex String polyReg, @Regex String reg) {
    @PolyRegex String test1 = polyReg + reg;
    @PolyRegex String test2 = reg + polyReg;
  }

  public void testRegexPolyRegexConcatErrors(
      @PolyRegex String polyReg, @Regex String reg, String str) {
    // :: error: (assignment)
    @PolyRegex String test1 = polyReg + str; // error
    // :: error: (assignment)
    @PolyRegex String test2 = str + polyReg; // error
    // :: error: (assignment)
    @PolyRegex String test3 = reg + str; // error
    // :: error: (assignment)
    @PolyRegex String test4 = str + reg; // error

    // :: error: (assignment)
    @PolyRegex String test5 = str + str; // error
  }

  public static @PolyRegex String slice(@PolyRegex String seq, int start, int end) {
    if (seq == null) {
      return null;
    }
    return seq;
  }

  public static @PolyRegex String slice(@PolyRegex String seq, long start, int end) {
    return slice(seq, (int) start, end);
  }
}
