// Test case for https://github.com/typetools/checker-framework/issues/2896

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue2896 {
  enum C {
    @Untainted A
  }

  void use(@Tainted C t) {
    @Untainted C a = C.A;
    // :: error: (assignment)
    @Untainted C c = t;
  }
}
