import org.checkerframework.framework.testchecker.util.*;

public class StringPatternsUsage {

  void requiresA(@PatternA String arg) {}

  void requiresB(@PatternB String arg) {}

  void requiresC(@PatternC String arg) {}

  void requiresAB(@PatternAB String arg) {}

  void requiresBC(@PatternBC String arg) {}

  void requiresAC(@PatternAC String arg) {}

  void requiresAny(String arg) {}

  void m() {

    String a = "A";
    String b = "B";
    String c = "C";
    String d = "D";
    String e = "";

    requiresA(a);
    // :: error: (argument)
    requiresB(a);
    // :: error: (argument)
    requiresC(a);
    requiresAB(a);
    // :: error: (argument)
    requiresBC(a);
    requiresAC(a);
    requiresAny(a);

    // :: error: (argument)
    requiresA(b);
    requiresB(b);
    // :: error: (argument)
    requiresC(b);
    requiresAB(b);
    requiresBC(b);
    // :: error: (argument)
    requiresAC(b);
    requiresAny(b);

    // :: error: (argument)
    requiresA(c);
    // :: error: (argument)
    requiresB(c);
    requiresC(c);
    // :: error: (argument)
    requiresAB(c);
    requiresBC(c);
    requiresAC(c);
    requiresAny(c);

    // :: error: (argument)
    requiresA(d);
    // :: error: (argument)
    requiresB(d);
    // :: error: (argument)
    requiresC(d);
    // :: error: (argument)
    requiresAB(d);
    // :: error: (argument)
    requiresBC(d);
    // :: error: (argument)
    requiresAC(d);
    requiresAny(d);

    // :: error: (argument)
    requiresA(e);
    // :: error: (argument)
    requiresB(e);
    // :: error: (argument)
    requiresC(e);
    // :: error: (argument)
    requiresAB(e);
    // :: error: (argument)
    requiresBC(e);
    // :: error: (argument)
    requiresAC(e);
    requiresAny(e);
  }
}
