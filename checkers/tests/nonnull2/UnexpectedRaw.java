import checkers.nullness.quals.*;

interface Consumer<A extends @Nullable Object> {
  public void consume(A object);
}

class Utils {

  public static <B extends @Nullable Object>
      Consumer<B> cast(final @Nullable Consumer<? super B> consumer) {
          throw new RuntimeException();
  }


  public static <C extends @Nullable Object> Consumer<C>
      getConsumer() {
    // null for simplicity, but could be anything
    Consumer<@Nullable Object> nullConsumer = null;
    // C could be @NonNull Object, so argument is incompatible?
    // Should this fail?
    //TODO:: error: (argument.type.incompatible)
    Consumer<C> result = Utils.<C>cast(nullConsumer);
    return result;
  }
}
