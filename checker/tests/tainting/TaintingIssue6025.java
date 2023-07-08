import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintingIssue6025 {

  public interface A<T> {}

  private static final class B<T> {}

  public interface C<T1 extends A<?>, T2 extends A<?>> {}

  private final B<C<? extends A<?>, ? extends A<?>>> one = new B<>();
  private final B<C<? extends A<?>, ? extends A<?>>> two = new B<>();

  void f(boolean b) {
    // :: error: (assignment)
    B<C<@Untainted ?, ?>> three1 = one;
    // :: error: (assignment)
    B<C<?, @Untainted ?>> three = b ? two : one;
  }
}
