package intellijannotations;

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
}
