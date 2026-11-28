/*
 * @test
 * @summary Do not issue immutable.type.guardedby for a type that is not written in source code.
 * The Checker Framework stores annotations in InferredTypesLockBase.class, and JDK 22+ javac
 * attaches them to types read from that class file, including types that javac infers from them.
 * On JDK 21 and earlier, javac does not attach them, so this test passes even without the fix.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.lock.LockChecker InferredTypesLockBase.java
 * @compile/fail/ref=InferredTypesLock.goal -XDrawDiagnostics -processor org.checkerframework.checker.lock.LockChecker InferredTypesLock.java
 */

import java.util.function.Predicate;
import org.checkerframework.checker.lock.qual.GuardedBy;

public class InferredTypesLock extends InferredTypesLockBase {
  static <T> boolean any(Iterable<T> iterable, Predicate<? super T> predicate) {
    for (T elt : iterable) {
      if (predicate.test(elt)) {
        return true;
      }
    }
    return false;
  }

  boolean implicitlyTypedLambda() {
    return any(strings, s -> s.isEmpty());
  }

  boolean varLambda() {
    return any(strings, (var s) -> s.isEmpty());
  }

  int varLocalString() {
    var s = getString();
    return s.length();
  }

  int varLocalInteger() {
    var i = getInteger();
    return i;
  }

  int varEnhancedFor() {
    int result = 0;
    for (var s : strings) {
      result += s.length();
    }
    return result;
  }

  // The annotations written in source code are still reported.

  boolean explicitlyTypedLambda() {
    return any(
        strings,
        (@GuardedBy({}) String s) -> s.isEmpty());
  }

  int explicitLocal() {
    @GuardedBy({}) String s = getString();
    return s.length();
  }
}
