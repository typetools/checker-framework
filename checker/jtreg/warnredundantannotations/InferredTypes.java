/*
 * @test
 * @summary Do not issue redundant.anno for a type that is not written in source code.
 * The Checker Framework stores annotations in Base.class, and JDK 22+ javac attaches them to
 * types read from that class file, including types that javac infers from them.
 * On JDK 21 and earlier, javac does not attach them, so this test passes even without the fix.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Foo.java Base.java
 * @compile/ref=InferredTypes.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnRedundantAnnotations InferredTypes.java
 */

import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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

  // The annotations written in source code are still reported.

  boolean explicitlyTypedLambda() {
    return any(foos, (@NonNull Foo foo) -> foo.hashCode() == 0);
  }

  int explicitLocal() {
    @Nullable Foo foo = getFoo();
    return foo.hashCode();
  }
}
