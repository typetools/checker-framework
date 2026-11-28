package intellijannotations;

import java.util.Comparator;
import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class IntellijAnnotationsTest {

  void testReturn(String s) {
    @H1S1 String s1 = s.trim();
    @H1Top String s2 = s.trim();
    // :: error: [assignment]
    @H1S2 String s3 = s.trim();
  }

  void testParam(String s, @H1Top String top, @H1S2 String s2) {
    s.concat(s2);
    // :: error: [argument]
    s.concat(top);
  }

  void testField() {
    @H1S1 Comparator<String> c1 = String.CASE_INSENSITIVE_ORDER;
    // :: error: [assignment]
    @H1S2 Comparator<String> c2 = String.CASE_INSENSITIVE_ORDER;
  }

  void testConstructor(char[] chars) {
    @H1S1 String s1 = new String(chars);
    // :: error: [assignment]
    @H1S2 String s2 = new String(chars);
    // The no-argument constructor is not annotated, so it retains the default qualifier.
    // :: error: [assignment]
    @H1S1 String s3 = new String();
  }

  void testClass(StringBuilder sb) {
    @H1S1 StringBuilder sb1 = sb;
  }
}
