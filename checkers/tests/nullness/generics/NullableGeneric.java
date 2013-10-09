import checkers.nullness.quals.*;

public class NullableGeneric<T> {

  public static class Pair<T1 extends @Nullable Object, T2 extends @Nullable Object> {
      public @Nullable T1 a;
      public @Nullable T2 b;
      public @NonNull T1 nna;
      public @NonNull T2 nnb;

    public Pair(T1 a, T2 b) {
        this.a = a;
        this.b = b;
        //:: error: (assignment.type.incompatible)
        this.nna = a;
        //:: error: (assignment.type.incompatible)
        this.nnb = b;
    }
  }

  @Nullable T next1 = null, next2 = null;

  private Pair<@Nullable T,@Nullable T> return1() {
    Pair<@Nullable T,@Nullable T> result = new Pair<@Nullable T, @Nullable T>(next1, null);
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
