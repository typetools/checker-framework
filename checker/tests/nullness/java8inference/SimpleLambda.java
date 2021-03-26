// @skip-test until Issue 979 is fixed.

import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SimpleLambda {
  <T> T perform(Supplier<T> p) {
    return p.get();
  }

  void test() {
    @Nullable String s1 = perform(() -> (String) null);
    @Nullable String s2 = this.<@Nullable String>perform(() -> (String) null);
    @NonNull String s3 = perform(() -> "");
  }
}
