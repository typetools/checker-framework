import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

public class WildcardAnnos {
  // :: error: (bound)
  @Nullable List<@Nullable ? extends @NonNull Object> l1 = null;
  @Nullable List<@NonNull ? extends @Nullable Object> l2 = null;

  // The implicit upper bound is Nullable, because the annotation
  // on the wildcard is propagated. Therefore this type is:
  // @Nullable List<? super @NonNull String extends @Nullable Object> l3 = null;
  @Nullable List<@Nullable ? super @NonNull String> l3 = null;

  // The bounds need to have the same annotations because capture conversion
  // converts the type argument to just Object.
  // :: error: (super.wildcard)
  @Nullable List<@Nullable ? super @NonNull Object> l3b = null;

  // :: error: (bound)
  @Nullable List<@NonNull ? super @Nullable String> l4 = null;

  @Nullable List<? super @Nullable String> l5 = null;

  @Nullable List<? extends @Nullable Object> inReturn() {
    return null;
  }

  void asParam(List<? extends @Nullable Object> p) {}

  // :: error: (conflicting.annos)
  @Nullable List<@Nullable @NonNull ? extends @Nullable Object> l6 = null;
  // :: error: (conflicting.annos)
  @Nullable List<@Nullable @NonNull ? super @NonNull String> l7 = null;
  // :: error: (conflicting.annos)
  @Nullable List<? extends @Nullable @NonNull Object> l8 = null;
  // :: error: (conflicting.annos)
  @Nullable List<? super @Nullable @NonNull String> l9 = null;
}
