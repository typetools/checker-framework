import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// The primary qualifier of each cast type equals the qualifier upper bound of `List`, but with
// -AcheckCastElementType the type arguments must still be checked.
public class CastToTopBoundType {
  @SuppressWarnings("unchecked")
  void nullableElements(Object nonNullObject) {
    // :: warning: [cast.unsafe]
    List<@Nullable String> l = (List<@Nullable String>) nonNullObject;
  }

  @SuppressWarnings("unchecked")
  void nonNullElements(@Nullable Object nullableObject) {
    // :: warning: [cast.unsafe]
    List<@NonNull String> l = (List<@NonNull String>) nullableObject;
  }

  void safeUpcast(List<@Nullable String> list) {
    List<@Nullable String> l = (List<@Nullable String>) list;
  }
}
