import org.checkerframework.checker.regex.qual.Regex;

public class LubRegex {

  void test1(@Regex(4) String s4, boolean b) {
    String s = null;
    if (b) {
      s = s4;
    }
    @Regex(4) String test = s;

    // :: error: (assignment.type.incompatible)
    @Regex(5) String test2 = s;
  }

  void test2(@Regex(2) String s2, @Regex(4) String s4, boolean b) {
    String s = s4;
    if (b) {
      s = s2;
    }
    @Regex(2) String test = s;

    // :: error: (assignment.type.incompatible)
    @Regex(3) String test2 = s;
  }

  void test3(@Regex(6) String s6, boolean b) {
    String s;
    if (b) {
      s = s6;
    } else {
      s = null;
    }
    @Regex(6) String test = s;

    // :: error: (assignment.type.incompatible)
    @Regex(7) String test2 = s;
  }

  void test4(@Regex(8) String s8, @Regex(9) String s9, boolean b) {
    String s;
    if (b) {
      s = s8;
    } else {
      s = s9;
    }
    @Regex(8) String test = s;

    // :: error: (assignment.type.incompatible)
    @Regex(9) String test2 = s;
  }

  void test5(@Regex(10) String s10, @Regex(11) String s11, boolean b) {
    String s;
    if (b) {
      s = s11;
    } else {
      s = s10;
      return;
    }
    @Regex(11) String test = s;

    // :: error: (assignment.type.incompatible)
    @Regex(12) String test2 = s;
  }
}
