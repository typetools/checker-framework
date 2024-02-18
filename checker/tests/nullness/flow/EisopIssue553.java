// Test case for EISOP Issue 553:
// https://github.com/eisop/checker-framework/issues/553
import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue553 {
  static @Nullable Object sfield = "";
  Object field = "";

  static void n(Object o) {
    sfield = null;
  }

  public static void main(String[] args) {
    EisopIssue553 x = null;
    Object o = x.sfield;
    // :: error: (dereference.of.nullable)
    o = x.field;
    if (x.sfield == null) {
      return;
    }
    x.n(x.sfield);
    // :: error: (dereference.of.nullable)
    x.sfield.toString();
  }
}
