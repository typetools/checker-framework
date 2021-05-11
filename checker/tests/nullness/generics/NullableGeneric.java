import org.checkerframework.checker.nullness.qual.*;

public class NullableGeneric<T> {

  public static class NullablePair<T1 extends @Nullable Object, T2 extends @Nullable Object> {
    public @Nullable T1 a;
    public @Nullable T2 b;
    public @NonNull T1 nna;
    public @NonNull T2 nnb;

    public NullablePair(T1 a, T2 b) {
      this.a = a;
      this.b = b;
      // :: error: (assignment)
      this.nna = a;
      // :: error: (assignment)
      this.nnb = b;
    }
  }

  @Nullable T next1 = null, next2 = null;

  private NullablePair<@Nullable T, @Nullable T> return1() {
    NullablePair<@Nullable T, @Nullable T> result =
        new NullablePair<@Nullable T, @Nullable T>(next1, null);
    // setnext1();
    return result;
  }

  public static <T3> @NonNull T3 checkNotNull(@Nullable T3 object) {
    if (object == null) {
      throw new NullPointerException();
    } else {
      return object;
    }
  }
}
