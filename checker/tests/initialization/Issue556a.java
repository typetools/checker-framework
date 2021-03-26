// @skip-test

// Minimal test case for issue #556: https://github.com/typetools/checker-framework/issues/556
// For explanations, see file Issue556b.java .

import org.checkerframework.checker.nullness.qual.NonNull;

public class Issue556a {

  public static final Issue556a SELF = new Issue556a();
  private static final Object OBJ = new Object();

  private Issue556a() {
    // :: error: (assignment.type.incompatible)
    @NonNull Object o = OBJ;
  }
}
