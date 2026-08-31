// A qualified superclass constructor invocation, `outer.super(...)`, where the qualifier is a raw
// type.  The invoked constructor is a member of the raw type `Outer.Inner`, so javac erases its
// signature; it accepts `o.super(new Desc<@NonNull String>())` for a constructor declared as
// `Inner(Desc<@Nullable String>)`.
//
// TreeUtils.isRawCall handles the unqualified `super(...)` form (see
// framework/tests/all-systems/RawSuper.java), but for the qualified form
// TreeUtils.getReceiverTree returns the enclosing-instance expression `o`, so the
// isSuperConstructorCall branch is never reached and TypesUtils.isRawCall is asked whether
// `Inner`'s constructor is a member of the raw type `Outer`, which it is not.

import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawQualifiedSuperCall {

  static class Desc<T> {}

  static class Outer<K> {
    class Inner {
      Inner(Desc<@Nullable String> d) {}
    }
  }

  // Control: the qualifier is parameterized, so nothing is erased.
  static class SubOfParameterized extends Outer<Object>.Inner {
    SubOfParameterized(Outer<Object> o, Desc<@Nullable String> d) {
      o.super(d);
    }
  }

  static class SubOfRaw extends Outer.Inner {
    SubOfRaw(Outer o, Desc<String> d) {
      // TODO: This is a false positive.  javac erases the constructor's signature because the
      // qualifier `o` is raw.
      // :: error: [argument]
      o.super(d);
    }
  }
}
