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
    // :: error: (argument.type.incompatible)
    requiresB(a);
    // :: error: (argument.type.incompatible)
    requiresC(a);
    requiresAB(a);
    // :: error: (argument.type.incompatible)
    requiresBC(a);
    requiresAC(a);
    requiresAny(a);

    // :: error: (argument.type.incompatible)
    requiresA(b);
    requiresB(b);
    // :: error: (argument.type.incompatible)
    requiresC(b);
    requiresAB(b);
    requiresBC(b);
    // :: error: (argument.type.incompatible)
    requiresAC(b);
    requiresAny(b);

    // :: error: (argument.type.incompatible)
    requiresA(c);
    // :: error: (argument.type.incompatible)
    requiresB(c);
    requiresC(c);
    // :: error: (argument.type.incompatible)
    requiresAB(c);
    requiresBC(c);
    requiresAC(c);
    requiresAny(c);

    // :: error: (argument.type.incompatible)
    requiresA(d);
    // :: error: (argument.type.incompatible)
    requiresB(d);
    // :: error: (argument.type.incompatible)
    requiresC(d);
    // :: error: (argument.type.incompatible)
    requiresAB(d);
    // :: error: (argument.type.incompatible)
    requiresBC(d);
    // :: error: (argument.type.incompatible)
    requiresAC(d);
    requiresAny(d);

    // :: error: (argument.type.incompatible)
    requiresA(e);
    // :: error: (argument.type.incompatible)
    requiresB(e);
    // :: error: (argument.type.incompatible)
    requiresC(e);
    // :: error: (argument.type.incompatible)
    requiresAB(e);
    // :: error: (argument.type.incompatible)
    requiresBC(e);
    // :: error: (argument.type.incompatible)
    requiresAC(e);
    requiresAny(e);
  }
}
