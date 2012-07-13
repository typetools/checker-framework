import checkers.nullness.quals.*;
import java.util.*;
@checkers.quals.DefaultQualifier("Nullable")
class RawMethodInvocation {
  Object a;
  Object b;

  RawMethodInvocation(boolean constructor_inits_a) {
    a = 1;
    init_b();
  }

  @AssertNonNullAfter("b")
  void init_b(@Raw RawMethodInvocation this) {
    b = 2;
  }

  RawMethodInvocation(int constructor_inits_none) {
    init_ab();
  }

  @AssertNonNullAfter({"a", "b"})
  void init_ab(@Raw RawMethodInvocation this) {
    a = 1;
    b = 2;
  }

  RawMethodInvocation(long constructor_escapes_raw) {
    a = 1;
    nonRawMethod();
    b = 2;
  }

  void nonRawMethod() { }
}
