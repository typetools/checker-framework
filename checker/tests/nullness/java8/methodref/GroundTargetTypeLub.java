import org.checkerframework.checker.nullness.qual.*;

interface Supplier<T extends @NonNull Object> {
  T supply();
}

interface Supplier2<T extends @Nullable Object> {
  T supply();
}

class GroundTargetType {

  static @Nullable Object myMethod() {
    return null;
  }

  Supplier<? extends @Nullable Object> fn = GroundTargetType::myMethod;
  // :: error: (methodref.return)
  Supplier<? extends @NonNull Object> fn2 = GroundTargetType::myMethod;

  // Supplier2
  // :: error: (methodref.return)
  Supplier2<? extends @NonNull Object> fn3 = GroundTargetType::myMethod;
}
