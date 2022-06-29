import java.io.Closeable;
import java.util.IdentityHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class EnclosingClass<A> {
  private MyMap myMap = null;

  // Avoids generic type capture inconsistency problems where |? extends V| is incompatible with V.
  private <B extends Throwable, C extends A> EnclosingClass<A> catchingMoreGeneric(
      final MyFunction<? super B, C> fallback) {
    AFunction<B, C> applyFallback =
        new AFunction<B, C>() {
          @Override
          public MyFuture<C> apply(B exception) throws Exception {
            return myMap.apply(fallback, exception);
          }

          @Override
          public String toString() {
            return fallback.toString();
          }
        };
    return null;
  }

  private abstract class MyMap extends IdentityHashMap<AutoCloseable, Executor>
      implements Closeable {
    abstract <D, E> MyFuture<E> apply(MyFunction<? super D, E> transformation, D input)
        throws Exception;
  }

  public interface MyFunction<F extends Object, G extends Object> {}

  interface AFunction<H, I> {
    MyFuture<I> apply(H input) throws Exception;
  }

  interface MyFuture<J> extends Future<J> {}
}
