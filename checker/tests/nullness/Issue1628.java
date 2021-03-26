// Test case for Issue 1628
// https://github.com/typetools/checker-framework/issues/1628

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public class Issue1628<V extends Comparable<? super V>> implements Issue1628R<V> {

  public boolean isEmpty() {
    return false;
  }

  public boolean equals(@Nullable Object o) {
    return (o instanceof Issue1628R) && ((Issue1628R) o).isEmpty();
  }
}

interface Issue1628R<V extends Comparable<? super V>> {
  @Pure
  boolean isEmpty();
}
