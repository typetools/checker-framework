package nullness.generics;

// see also framework/tests/all-systems/WildcardSuper2

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

interface ToOverride<T> {
  // For nullness this should default to type @NonNull List<? [ extends @Nullable Object
  //                                                             super @NonNull  T ]>
  public abstract int transform(List<? super T> function);
}

public class WildcardOverride implements ToOverride<Object> {
  // invalid because the overriden method takes @Nullable args and this one doesn't
  @Override
  // :: error: (override.param.invalid)
  public int transform(List<Object> function) {
    return 0;
  }
}

interface ToOverride2<T> {
  // For nullness this should be typed as
  // @NonNull List<? [ extends @NonNull Object super T [ extends @Nullable super @NonNull ]>
  // :: error: (bound.type.incompatible)
  public abstract int transform(List<@NonNull ? super T> function);
}

class WildcardOverride2 implements ToOverride2<Object> {
  // valid because the overriden method takes ONLY @NonNull args and this one takes @NonNull args
  // as well
  @Override
  public int transform(List<Object> function) {
    return 0;
  }
}
