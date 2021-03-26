import java.io.Serializable;
import java.util.AbstractMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("all") // Just check for crashes.
public class Bug2 {

  public <C, D> ConcurrentMap<C, D> makeMap() {
    return Bug2.create(this);
  }

  static <A, B> MyMap<A, B, ? extends MyMap.MyEntry<A, B, ?>, ?> create(Bug2 builder) {
    throw new RuntimeException();
  }

  abstract static class MyMap<
          E, F, G extends MyMap.MyEntry<E, F, G>, H extends MyMap.MyLock<E, F, G, H>>
      extends AbstractMap<E, F> implements ConcurrentMap<E, F>, Serializable {

    interface MyEntry<I, J, K extends MyEntry<I, J, K>> {}

    abstract static class MyLock<L, M, N extends MyEntry<L, M, N>, O extends MyLock<L, M, N, O>>
        extends ReentrantLock {}
  }
}
