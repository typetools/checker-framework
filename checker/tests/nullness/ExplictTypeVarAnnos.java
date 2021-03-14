import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ExplictTypeVarAnnos<E extends @Nullable Object, @Nullable F> {
  interface Consumer<A extends @Nullable Object> {}

  public static <B extends @Nullable Object> Consumer<B> cast(
      final @Nullable Consumer<? super B> consumer) {
    throw new RuntimeException();
  }

  public static <C extends @Nullable Object> Consumer<C> getConsumer0() {
    Consumer<@Nullable Object> nullConsumer = null;
    Consumer<C> result = ExplictTypeVarAnnos.<C>cast(nullConsumer);
    return result;
  }

  public static <@Nullable D> Consumer<D> getConsumer1() {
    Consumer<@Nullable Object> nullConsumer = null;
    Consumer<D> result = ExplictTypeVarAnnos.<D>cast(nullConsumer);
    return result;
  }

  public Consumer<E> getConsumer2() {
    Consumer<@Nullable Object> nullConsumer = null;
    Consumer<E> result = ExplictTypeVarAnnos.<E>cast(nullConsumer);
    return result;
  }

  public Consumer<F> getConsumer3() {
    Consumer<@Nullable Object> nullConsumer = null;
    Consumer<F> result = ExplictTypeVarAnnos.<F>cast(nullConsumer);
    return result;
  }

  @SuppressWarnings("method.invocation.invalid")
  Consumer<E> field = getConsumer2();

  public Consumer<E> getField() {
    return field;
  }

  static class A<Q extends @NonNull Object> {}

  // :: error: (type.argument.type.incompatible)
  static class B<S> extends A<S> {}
}
