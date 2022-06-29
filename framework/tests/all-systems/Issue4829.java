import java.util.concurrent.Future;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("all") // Just check for crashes.
public class Issue4829 {

  interface ListenableFuture<V extends @Nullable Object> extends Future<V> {}

  enum E {}

  ListenableFuture<Object> f(E e) {
    return g(e);
  }

  ListenableFuture<? super Object> g(E e) {
    throw new AssertionError();
  }
}
