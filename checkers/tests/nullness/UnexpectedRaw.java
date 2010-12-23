import checkers.nullness.quals.*;

interface Consumer<A extends @Nullable Object> {
  public void consume(A object);
}

class Utils {

  public static <A extends @Nullable Object>
      Consumer<A> cast(final @Nullable Consumer<? super A> consumer) {
          throw new RuntimeException();
  }


  public static <A extends @Nullable Object> Consumer<A>
      getConsumer() {
    // null for simplicity, but could be anything
    Consumer<@Nullable Object> nullConsumer = null;
    Consumer<A> result = Utils.<A>cast(nullConsumer);
    return result;
  }
}
