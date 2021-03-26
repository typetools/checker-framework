import org.checkerframework.checker.nullness.qual.*;

public class Uninit6 {
  // Failure to initialize these fields does not directly compromise the
  // guarantee of no null pointer errors.
  @MonotonicNonNull Object f;
  @Nullable Object g;

  Uninit6() {}
}
