// Test case for Issue 282 (minimized)
// https://code.google.com/p/checker-framework/issues/detail?id=282
import org.checkerframework.checker.nullness.qual.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

class Test {
  static <M> Set<M> copyOf(
      Comparator<? super M> comparator, Collection<? extends M> elements) {
    //:: error: (return.type.incompatible)
    return null;
  }
}

class Example {
  <T extends @NonNull Object> Set<T> foo(Comparator<Object> ord, Collection<T> set) {
    return Test.copyOf(ord, set);
  }
}
