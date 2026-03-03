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

  @SuppressWarnings("dereference.of.nullable") // requires refinement like for Optional.ifPresent.
  void m3(@Nullable String o) {
    Opt.ifPresentOrElse(o, s -> o.toString(), () -> System.out.println("empty"));
  }

  void m4(@Nullable String o) {
    Opt.ifPresentOrElse(o, s -> s.toString(), () -> System.out.println("empty"));
  }

  void m5(@Nullable String o) {
    boolean[] called = {false};
    Opt.ifPresentOrElse(o, s -> called[0] = true, () -> called[0] = false);
  }
}
