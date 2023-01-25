// https://github.com/typetools/checker-framework/issues/300
// Impure method can make the method return value nullable

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.*;

public final class EisopIssue300C {
  @Nullable EisopIssue300C f;

  EisopIssue300C() {
    this.f = this;
  }

  void m2() {
    f = null;
  }

  @Pure
  @Nullable EisopIssue300C getF() {
    return f;
  }

  public static void main(String[] args) {
    EisopIssue300C r = new EisopIssue300C();

    if (r.getF() != null) {
      r.getF().m2();
      // :: error: (dereference.of.nullable)
      r.getF().toString();
    }
  }
}
