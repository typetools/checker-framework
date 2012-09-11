import checkers.nullness.quals.*;
import java.util.*;

class WildcardAnnos {
  // TODO: analogously to type variables, shouldn't the annotation on the wildcard
  // override whatever is on the bounds?
  //:: error: (type.invalid)
  @Nullable List<@Nullable ? extends @NonNull Object> l1 = null;
  @Nullable List<@NonNull ? extends @Nullable Object> l2 = null;

  // The implicit upper bound is Nullable, because the annotation
  // on the wildcard is propagated. Therefore this type is:
  // @Nullable List<@Nullable ? super @NonNull Object extends @Nullable Object> l3 = null;
  @Nullable List<@Nullable ? super @NonNull Object> l3 = null;

  //:: error: (type.invalid)
  @Nullable List<@NonNull ? super @Nullable Object> l4 = null;

  @Nullable List<? super @Nullable Object> l5 = null;

  @Nullable List<? extends @Nullable Object> inReturn() { return null; }
  void asParam(List<? extends @Nullable Object> p) {}

  //:: error: (type.invalid)
  @Nullable List<@Nullable @NonNull ? extends @Nullable Object> l6 = null;
  //:: error: (type.invalid)
  @Nullable List<@Nullable @NonNull ? super @NonNull Object> l7 = null;
  //:: error: (type.invalid)
  @Nullable List<? super @Nullable @NonNull  Object> l8 = null;
  //:: error: (type.invalid)
  @Nullable List<? super @Nullable @NonNull  Object> l9 = null;

}
