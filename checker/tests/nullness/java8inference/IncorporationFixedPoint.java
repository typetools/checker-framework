// Incorporation (JLS 18.3) must run until the bound set reaches a fixed point.
//
// Below, `m` has a chain of four inference variables α1 :> α2 :> α3 :> α4 plus the unused variable
// αZz.  The argument gives α4 the lower bound `@Nullable String`, and each incorporation pass
// propagates that bound one link up the chain, towards a variable that has already been visited in
// the current pass.  αZz is declared last and participates in no constraint at all, so its bounds
// never change.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class IncorporationFixedPoint {

  static <
          A1 extends @Nullable Object,
          A2 extends A1,
          A3 extends A2,
          A4 extends A3,
          Zz extends @Nullable Object>
      A1 m(A4 a) {
    throw new RuntimeException();
  }

  void use(@Nullable String arg) {
    // :: error: [assignment] :: error: [type.arguments.not.inferred]
    @NonNull String r = m(arg);
  }

  void ok(@Nullable String arg) {
    @Nullable String r = m(arg);
  }
}
