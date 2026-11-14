import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for record patterns whose matched type mentions captured type variables, such as a record
// pattern nested in another one, whose record component has the type Box<CAP#1>.
public class RecordPatternCapturedTypeArguments {

  record Box<T extends @Nullable Object>(T t) {}

  record NonNullOuter<U extends @Nullable Object>(Box<@NonNull U> b) {}

  record NullableOuter<U>(Box<@Nullable U> b) {}

  record Pair<A extends @Nullable Object, B extends Box<A>>(A a, B b) {}

  // The record component's type is Box<@NonNull CAP#1>.
  void nonNullCapture(NonNullOuter<?> o) {
    if (o instanceof NonNullOuter(Box(var t))) {
      t.toString();
    }
    if (o.b() instanceof Box(var t)) {
      t.toString();
    }
  }

  // The record component's type is Box<@Nullable CAP#1>.
  void nullableCapture(NullableOuter<?> o) {
    if (o instanceof NullableOuter(Box(var t))) {
      // :: error: (dereference.of.nullable)
      t.toString();
    }
  }

  // The type of b is CAP#2 extends Box<CAP#1>.
  void captureInBound(Pair<? extends @Nullable String, ?> p) {
    if (p instanceof Pair(var a, Box(var t))) {
      // :: error: (dereference.of.nullable)
      t.length();
    }
  }

  void captureInBoundNonNull(Pair<? extends String, ?> p) {
    if (p instanceof Pair(var a, Box(var t))) {
      t.length();
    }
  }
}
