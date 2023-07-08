public class Issue6025 {

  public interface A<T> {}

  private static final class B<T> {}

  public interface C<T1 extends A<?>, T2 extends A<?>> {}

  private final B<C<? extends A<?>, ? extends A<?>>> one = new B<>();
  private final B<C<? extends A<?>, ? extends A<?>>> two = new B<>();

  void f(boolean b) {
    B<C<?, ?>> three1 = one;
    B<C<?, ?>> three = b ? two : one;
  }
}
