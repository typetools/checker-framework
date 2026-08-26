// Accessing a non-static field of a raw type.  Per JLS section 4.8, the type of such a field is
// the erasure of its declared type, so no type argument of the raw type is substituted into it.
// Failing to erase it produced a wildcard marked "INFERENCE FAILED", which crashed capture
// conversion.  Reduced from com.google.common.util.concurrent.AbstractFuture.

public class RawFieldAccess {

  interface Fut<T> {}

  static class SetFuture<V> {
    final Fut<? extends V> future;

    SetFuture(Fut<? extends V> future) {
      this.future = future;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  void rawFieldAccess(Object localValue) {
    Fut<?> futureToPropagateTo = ((SetFuture) localValue).future;
  }
}
