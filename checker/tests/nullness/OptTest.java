// Tests of the `Opt` utility class.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.util.Opt;

public class OptTest {

  @SuppressWarnings("dereference.of.nullable") // requires refinement like for Optional.ifPresent.
  void m1(@Nullable String o) {
    Opt.ifPresent(o, s -> o.toString());
  }

  void m2(@Nullable String o) {
    Opt.ifPresent(o, s -> s.toString());
  }
}
