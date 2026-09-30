// Test case for https://github.com/typetools/checker-framework/issues/1444

import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1444 {

  public boolean b(Supplier<Boolean> sb) {
    return t(sb, false);
  }

  public <T> T t(Supplier<@Nullable T> st, T ifNull) {
    T get = st.get();
    return get != null ? get : ifNull;
  }
}
