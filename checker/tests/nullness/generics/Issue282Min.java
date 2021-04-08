// Test case for Issue 282 (minimized)
// https://github.com/typetools/checker-framework/issues/282

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.*;

public class Issue282Min {
  static <M> Set<M> copyOf(Comparator<? super M> comparator, Collection<? extends M> elements) {
    // :: error: (return.type.incompatible)
    return null;
  }
}

class Example282Min {
  <T extends @NonNull Object> Set<T> foo(Comparator<Object> ord, Collection<T> set) {
    return Issue282Min.copyOf(ord, set);
  }
}
