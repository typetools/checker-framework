import checkers.nullness.quals.*;
import java.util.*;

class WildcardAnnos {
  //:: error: (type.invalid)
  @Nullable List<@Nullable ? extends @NonNull Object> l1 = null;
  @Nullable List<@NonNull ? extends @Nullable Object> l2 = null;

  // The implicit upper bound is NonNull, which then conflicts with
  // the annotation on the wildcard. The upper bound cannot be made
  // explicit.
  // @Nullable List<@Nullable ? super @NonNull Object extends @Nullable Object> l3 = null;
  //:: error: (type.invalid)
  @Nullable List<@Nullable ? super @NonNull Object> l3 = null;

  //:: error: (type.invalid)
  @Nullable List<@NonNull ? super @Nullable Object> l4 = null;

  @Nullable List<? super @Nullable Object> l5 = null;

  @Nullable List<? extends @Nullable Object> inReturn() { return null; }
  void asParam(List<? extends @Nullable Object> p) {}
}
