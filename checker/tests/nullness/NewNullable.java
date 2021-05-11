import org.checkerframework.checker.nullness.qual.*;

public class NewNullable {
  Object o = new Object();
  Object nn = new @NonNull Object();
  // :: warning: (new.class)
  @Nullable Object lazy = new @MonotonicNonNull Object();
  // :: warning: (new.class)
  @Nullable Object poly = new @PolyNull Object();
  // :: warning: (new.class)
  @Nullable Object nbl = new @Nullable Object();
}
