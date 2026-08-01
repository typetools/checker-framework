// Test case for a bug in AnnotatedContainsInferenceVariable, which used Collection.contains to
// decide whether an annotated type mentions an uninstantiated inference variable.
// Collection.contains uses reference equality, because javac's TypeVariable does not override
// equals.  A use of a type variable that is written with a type annotation, such as the "U" in
// "Map<T, @Nullable U>", is a different TypeVariable object than the "U" in the method's type
// parameter list.  Therefore, once "T" was instantiated, a type that still mentions "U" was
// misclassified as a proper type rather than as an inference type, and type argument inference
// crashed.
//
// Each method below invokes a generic method whose type variables are resolved in two stages.  The
// nested calls to "id" force "T" to be instantiated before "U", so that the type that
// AnnotatedContainsInferenceVariable examines still mentions the uninstantiated "U".

import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AnnotatedTypeVar {

  static <X> X id(X x) {
    return x;
  }

  static <T, U> U annotatedTypeArgIsSecond(Map<T, @Nullable U> m, T t) {
    throw new RuntimeException();
  }

  Object useAnnotatedTypeArgIsSecond(Map<String, @Nullable Integer> m, String s) {
    return annotatedTypeArgIsSecond(id(m), id(s));
  }

  static <T, U> T annotatedTypeArgIsFirst(Map<@Nullable U, T> m, T t) {
    throw new RuntimeException();
  }

  Object useAnnotatedTypeArgIsFirst(Map<@Nullable Integer, String> m, String s) {
    return annotatedTypeArgIsFirst(id(m), id(s));
  }
}
