import org.checkerframework.checker.nullness.qual.Nullable;

// A call's NullPointerException for a null receiver must not carry a non-null refinement of the
// receiver into a catch block.
public class ReceiverRefinementInCatch {

  void m(@Nullable Object o) {
    try {
      // :: error: (dereference.of.nullable)
      o.toString();
    } catch (NullPointerException e) {
      // :: error: (dereference.of.nullable)
      o.hashCode();
    }
  }

  void afterNormalCompletion(@Nullable Object o) {
    // :: error: (dereference.of.nullable)
    o.toString();
    // The call completed normally, so o is non-null.
    o.hashCode();
  }
}
