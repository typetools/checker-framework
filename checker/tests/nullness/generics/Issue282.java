// Test case for Issue 282
// https://github.com/typetools/checker-framework/issues/282

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.*;

@SuppressWarnings("nullness")
abstract class ImmutableSortedSet<E extends @NonNull Object> implements Set<E> {
  static <E> ImmutableSortedSet<E> copyOf(
      Comparator<? super E> comparator, Collection<? extends E> elements) {
    return null;
  }
}

@SuppressWarnings("nullness")
abstract class Ordering<T> implements Comparator<T> {
  static Ordering<Object> usingToString() {
    return null;
  }
}

abstract class Example {
  private static <@NonNull T extends @NonNull Object> ImmutableSortedSet<T> setSortedByToString(
      Collection<T> set) {
    return ImmutableSortedSet.copyOf(Ordering.usingToString(), set);
  }
}
