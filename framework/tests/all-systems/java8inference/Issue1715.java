import java.util.List;
import java.util.function.Function;

public class Issue1715 {

  static final class A {

    public A() {}

    public Object foo(Object o) {
      return o;
    }
  }

  private Observable<List<Function<Object, Object>>> test(A a) {
    return Observable.just(ImmutableList.of(a::foo));
  }

  static class Observable<F> {

    static <T> Observable<T> just(T param) {
      throw new RuntimeException();
    }
  }

  public abstract static class ImmutableList<E> implements List<E> {

    public static <E> ImmutableList<E> of(E element) {
      throw new RuntimeException();
    }
  }
}
