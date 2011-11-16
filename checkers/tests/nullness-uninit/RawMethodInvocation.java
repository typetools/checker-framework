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
    // TODO FIXME: This legal call yields a bizarre and incorrect error:
    // RawMethodInvocation.java:27: error: call to init_b() not allowed on the given receiver.
    //     init_b();
    //           ^
    //   found   : @Nullable RawMethodInvocation
    //   required: @NonNull RawMethodInvocation
    //:: error: (method.invocation.invalid)
    init_b();
  }

  //:: warning: (fields.uninitialized)
  RawMethodInvocation(byte constructor_inits_b) {
    b = 2;
    // TODO FIXME: This legal call yields a bizarre and incorrect error:
    // RawMethodInvocation.java:40: error: call to init_b() not allowed on the given receiver.
    //     init_b();
    //           ^
    //   found   : @Nullable RawMethodInvocation
    //   required: @NonNull RawMethodInvocation
    //:: error: (method.invocation.invalid)
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
    //:: error: (method.invocation.invalid)
    nonRawMethod();
    b = 2;
  }

  void nonRawMethod() { }
}
