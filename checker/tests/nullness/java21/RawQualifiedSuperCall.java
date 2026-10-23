// A qualified superclass constructor invocation, `outer.super(...)`, where the qualifier is a raw
// type.  The invoked constructor is a member of the raw type `Outer.Inner`, so javac erases its
// signature; it accepts `o.super(d)` for `d` of type `Desc<@NonNull String>` even though the
// constructor is declared as `Inner(Desc<@Nullable String>)`.
//
// For this form, TreeUtils.getReceiverTree returns the enclosing-instance expression `o` rather
// than a receiver, so the constructor is not a member of `o`'s type or of any of its supertypes;
// it is a member of a class that `o`'s type encloses.

import org.checkerframework.checker.nullness.qual.Nullable;

// Javaparsers fails on this file for Java 17, so just skip it on Java 17.
// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
@SuppressWarnings({"rawtypes", "unchecked"})
public class RawQualifiedSuperCall {

  static class Desc<T> {}

  static class Outer<K> {
    class Inner {
      Inner(Desc<@Nullable String> d) {}
    }
  }

  // The qualifier is parameterized, so nothing is erased.
  static class SubOfParameterized extends Outer<Object>.Inner {
    SubOfParameterized(Outer<Object> o, Desc<@Nullable String> d) {
      o.super(d);
    }
  }

  static class SubOfRaw extends Outer.Inner {
    SubOfRaw(Outer o, Desc<String> d) {
      o.super(d);
    }
  }
}
