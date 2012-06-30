import checkers.nullness.quals.*;
import java.util.*;
@checkers.quals.DefaultQualifier("Nullable")
class RawMethodInvocation {
  int a;
  int b;

  RawMethodInvocation(boolean constructor_inits_a) {
    a = 1;
    init_b();
  }

  @AssertNonNullAfter("b")
  void init_b(@Raw RawMethodInvocation this) {
    b = 2;
  }

  //:: warning: (fields.uninitialized)
  RawMethodInvocation(Byte constructor_inits_b) {
    init_b();
  }

  //:: warning: (fields.uninitialized)
  RawMethodInvocation(byte constructor_inits_b) {
    b = 2;
    init_b();
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
    //:: warning: (method.invocation.invalid.rawness)
    nonRawMethod();
    b = 2;
  }

  void nonRawMethod() { }
}
