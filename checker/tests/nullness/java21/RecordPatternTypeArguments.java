import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
public class RecordPatternTypeArguments {

  record Box<T>(T t) implements Supplier<T> {
    public T get() {
      return t;
    }
  }

  void explicit(Supplier<@Nullable String> s) {
    if (s instanceof Box<String>(var x)) {
      // :: error: (dereference.of.nullable)
      x.length();
    }
  }

  void explicitUnbounded(Supplier<@Nullable String> s) {
    if (s instanceof Box<?>(var x)) {
      // :: error: (dereference.of.nullable)
      x.toString();
    }
  }

  // The inferred type of the pattern, Box<@Nullable String>, is more specific than the written
  // type.
  void explicitBounded(Supplier<@Nullable String> s) {
    if (s instanceof Box<? extends CharSequence>(var x)) {
      // :: error: (dereference.of.nullable)
      x.length();
    }
  }

  void explicitBoundedTypePattern(Supplier<@Nullable String> s) {
    if (s instanceof Box<? extends CharSequence>(CharSequence x)) {
      // :: error: (dereference.of.nullable)
      x.length();
    }
  }

  void explicitBoundedNonNull(Supplier<String> s) {
    if (s instanceof Box<? extends CharSequence>(var x)) {
      x.length();
    }
  }

  // The matched type is already a captured type variable.
  void captured(List<? extends Supplier<@Nullable String>> l) {
    if (l.get(0) instanceof Box(var x)) {
      // :: error: (dereference.of.nullable)
      x.length();
    }
  }

  // The type argument of the matched type contains a wildcard.
  void nestedWildcard(Supplier<List<? extends @Nullable String>> s) {
    if (s instanceof Box(var list)) {
      list.size();
      // :: error: (dereference.of.nullable)
      list.get(0).length();
    }
  }

  // The inferred component type, ArrayList<@Nullable String>, is a subtype of the written type,
  // List<? extends String>, so the binding variable keeps the type argument's qualifier.
  void explicitBoundedTypeArgument(Supplier<ArrayList<@Nullable String>> s) {
    if (s instanceof Box<? extends List<? extends String>>(var x)) {
      // :: error: (dereference.of.nullable)
      x.get(0).length();
    }
  }
}
