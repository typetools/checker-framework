/*
 * @test
 * @summary Do not issue redundant.anno for a type that is not written in source code.
 * The Checker Framework stores annotations in Base.class, and JDK 22+ javac attaches them to
 * types read from that class file, including types that javac infers from them.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Foo.java Base.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnRedundantAnnotations -Werror InferredTypes.java
 */

import java.util.function.Predicate;

public class InferredTypes extends Base {
  static <T> boolean any(Iterable<T> iterable, Predicate<? super T> predicate) {
    for (T elt : iterable) {
      if (predicate.test(elt)) {
        return true;
      }
    }
    return false;
  }

  boolean implicitlyTypedLambda() {
    return any(foos, foo -> foo.hashCode() == 0);
  }

  boolean varLambda() {
    return any(foos, (var foo) -> foo.hashCode() == 0);
  }

  int varLocal() {
    var foo = getFoo();
    return foo.hashCode();
  }

  int varEnhancedFor() {
    int result = 0;
    for (var foo : foos) {
      result += foo.hashCode();
    }
    return result;
  }
}
