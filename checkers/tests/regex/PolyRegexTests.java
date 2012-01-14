import checkers.regex.quals.PolyRegex;
import checkers.regex.quals.Regex;
import java.util.regex.Pattern;

public class PolyRegexTests {

  public static @PolyRegex String method(@PolyRegex String s) {
    return s;
  }

  public void testRegex(@Regex String str) {
    @Regex String s = method(str);
  }

  public void testNonRegex(String str) {
    //:: error: (assignment.type.incompatible)
    @Regex String s = method(str); // error
  }

  public void testInternRegex(@Regex String str) {
    @Regex String s = str.intern();
  }

  public void testInternNonRegex(String str) {
    //:: error: (assignment.type.incompatible)
    @Regex String s = str.intern(); // error
  }
  
  public @PolyRegex String testPolyRegexConcat(@PolyRegex String s1, @PolyRegex String s2) {
    return s1 + s2;
  }
   
  public void testPolyRegexConcatErrors(@PolyRegex String polyReg, String nonPolyReg) {
    //:: error: (assignment.type.incompatible)
    @PolyRegex String test1 = polyReg + nonPolyReg; // error
    //:: error: (assignment.type.incompatible)
    @PolyRegex String test2 = nonPolyReg + polyReg; // error
    //:: error: (assignment.type.incompatible)
    @PolyRegex String test3 = nonPolyReg + nonPolyReg; // error
  }
}
