// Test case for Issue 1059:
// https://github.com/typetools/checker-framework/issues/1059

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1059 {
  @Nullable Object f;

  @EnsuresNonNull({"f"})
  void foo() {
    f = new Object();
  }

  void bar() {
    switch (this.hashCode()) {
      case 1:
        foo();
        Object dada = f.toString();
    }
  }
}
