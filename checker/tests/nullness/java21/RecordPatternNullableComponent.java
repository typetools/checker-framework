import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
public class RecordPatternNullableComponent {

  record Box<T>(T t) implements Supplier<T> {
    public T get() {
      return t;
    }
  }

  record ObjectBox(@Nullable Object o) {}

  record Pair<A, B>(A a, B b) {}

  void varPattern(Box<@Nullable String> box) {
    if (box instanceof Box(var s)) {
      // :: error: (dereference.of.nullable)
      s.length();
    }
  }

  void varPatternAssignNull(Box<@Nullable String> box) {
    if (box instanceof Box(var s)) {
      s = null;
    }
  }

  void varPatternNonNull(Box<String> box) {
    if (box instanceof Box(var s)) {
      s.length();
    }
  }

  // The pattern's explicit type arguments give `x` javac's type List<? extends CharSequence>, which
  // is wider than the inferred List<@Nullable String>.  `x` should have javac's shape with the
  // inferred qualifiers, List<? extends @Nullable CharSequence>, but it has the type
  // List<@Nullable String>.  See the TODO in TypeFromMemberVisitor.varPatternType.
  void varPatternKeepsJavacWildcard(Supplier<List<@Nullable String>> s) {
    if (s instanceof Box<? extends List<? extends CharSequence>>(var x)) {
      // :: error: (dereference.of.nullable)
      x.get(0).length();
      // TODO: false positive.
      // :: error: (assignment)
      x = new ArrayList<StringBuilder>();
    }
  }

  // javac gives `x` the type List<CAP#1>, which mentions a captured type variable.  `x` has the
  // upward projection of the record component's type instead: List<? extends @Nullable String>.
  void varPatternJavacCapture(Box<List<? extends @Nullable String>> box) {
    if (box instanceof Box(var x)) {
      // :: error: (dereference.of.nullable)
      x.get(0).length();
      List<? extends @Nullable String> y = x;
    }
  }

  // Like other local variables, an unannotated binding variable defaults to @Nullable, so it can be
  // assigned null.  Dataflow refines it by the qualifier of the record component.
  void assignNullToNonNullComponent(Box<String> box) {
    if (box instanceof Box(var s)) {
      s.length();
      s = null;
    }
    if (box instanceof Box(String s)) {
      s.length();
      s = null;
    }
  }

  // `String s` is unconditional for the component type, so it matches null.
  void unconditionalTypePattern(Box<@Nullable String> box) {
    if (box instanceof Box(String s)) {
      // :: error: (dereference.of.nullable)
      s.length();
    }
  }

  void unconditionalTypePatternSupertype(Box<@Nullable String> box) {
    if (box instanceof Box(CharSequence s)) {
      // :: error: (dereference.of.nullable)
      s.length();
    }
  }

  // `String s` is not unconditional for Object, so it does not match null.
  void conditionalTypePattern(ObjectBox box) {
    if (box instanceof ObjectBox(String s)) {
      s.length();
    }
  }

  // The written type argument makes the component type a capture of `? extends CharSequence`, so
  // `String s` is not unconditional and does not match null, even though the inferred component
  // type is `@Nullable String`.
  void conditionalWithExplicitTypeArgument(Supplier<@Nullable String> supplier) {
    if (supplier instanceof Box<? extends CharSequence>(String s)) {
      s.length();
    }
  }

  void switchVarPattern(Box<@Nullable String> box) {
    switch (box) {
      // :: error: (dereference.of.nullable)
      case Box(var s) -> s.length();
    }
  }

  void switchTypePattern(Box<@Nullable String> box) {
    switch (box) {
      // :: error: (dereference.of.nullable)
      case Box(String s) -> s.length();
    }
  }

  void nested(Box<Pair<@Nullable String, String>> box) {
    if (box instanceof Box(Pair(var a, String b))) {
      // :: error: (dereference.of.nullable)
      a.length();
      b.length();
    }
  }

  void supertype(Supplier<@Nullable String> supplier) {
    if (supplier instanceof Box(var s)) {
      // :: error: (dereference.of.nullable)
      s.length();
    }
  }

  void wildcard(Supplier<? extends @Nullable String> supplier) {
    if (supplier instanceof Box(String s)) {
      // :: error: (dereference.of.nullable)
      s.length();
    }
  }

  <T extends @Nullable Object> void typeVariable(Box<T> box) {
    if (box instanceof Box(var t)) {
      // :: error: (dereference.of.nullable)
      t.toString();
    }
    if (box instanceof Box(Object o)) {
      // :: error: (dereference.of.nullable)
      o.toString();
    }
  }

  // A binding variable whose type is a type variable can be assigned null, like a local variable of
  // that type.
  <T extends @NonNull Object> void typeVariableAssignNull(Box<T> box) {
    if (box instanceof Box(var t)) {
      t.toString();
      t = null;
    }
    if (box instanceof Box(T t)) {
      t.toString();
      t = null;
    }
  }

  // The pattern matches null, so an explicit @NonNull is not justified.
  void explicitNonNullUnconditional(Box<@Nullable String> box) {
    // :: error: (assignment)
    if (box instanceof Box(@NonNull String s)) {
      s.length();
    }
  }

  // The pattern does not match null, so an explicit @NonNull is justified.
  void explicitNonNullConditional(ObjectBox box) {
    if (box instanceof ObjectBox(@NonNull String s)) {
      s.length();
    }
  }

  // `s` is refined by the type of the record component.
  void explicitNullable(Box<String> box) {
    if (box instanceof Box(@Nullable String s)) {
      s.length();
    }
  }
}
