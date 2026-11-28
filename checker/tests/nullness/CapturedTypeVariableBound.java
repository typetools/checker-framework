import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// A value whose type is a captured type variable, such as the type of `list.get(0)` for a
// `List<? extends U>`, has the qualifiers of the type variable that bounds the captured wildcard,
// unless the wildcard's bound has its own annotation.
public class CapturedTypeVariableBound {
  <U extends @NonNull Object> void unannotatedWildcard(List<? extends U> list) {
    Object o = list.get(0);
    o.toString();
  }

  <U extends @NonNull Object> void annotatedWildcardObject(List<? extends @Nullable U> list) {
    Object o = list.get(0);
    // :: error: [dereference.of.nullable]
    o.toString();
  }

  <U extends @NonNull Object> void annotatedWildcardTypeVar(List<? extends @Nullable U> list) {
    U u = list.get(0);
    // :: error: [dereference.of.nullable]
    u.toString();
  }

  <U extends @NonNull Object> void annotatedWildcardDirect(List<? extends @Nullable U> list) {
    // :: error: [dereference.of.nullable]
    list.get(0).toString();
  }
}
