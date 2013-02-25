import checkers.initialization.quals.Unclassified;
import checkers.nonnull.quals.EnsuresNonNull;
import checkers.nullness.quals.*;
import java.util.*;
@checkers.quals.DefaultQualifier(Nullable.class)
class RawMethodInvocation {
  Object a;
  Object b;

  RawMethodInvocation(boolean constructor_inits_a) {
    a = 1;
    init_b();
  }

  @EnsuresNonNull("b")
  void init_b(@Raw @Unclassified RawMethodInvocation this) {
    b = 2;
  }

  RawMethodInvocation(int constructor_inits_none) {
    init_ab();
  }

  @EnsuresNonNull({"a", "b"})
  void init_ab(@Raw @Unclassified RawMethodInvocation this) {
    a = 1;
    b = 2;
  }

  RawMethodInvocation(long constructor_escapes_raw) {
    a = 1;
    // this call is not valid, this is still raw
    //:: error: (method.invocation.invalid)
    nonRawMethod();
    b = 2;
  }

  void nonRawMethod() { }
}
