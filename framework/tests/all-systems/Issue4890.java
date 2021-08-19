import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;

@SuppressWarnings("all") // Just check for crashes.
public class Issue4890 {

  class R<P extends PK, E extends N<P>, K> {}

  interface PK {}

  interface N<K extends PK> {}

  interface Q<K extends S<P>, P extends @NonNull Object> extends N<K> {}

  interface S<P> extends PhysicalPK {}

  interface PhysicalPK extends PK {}

  interface I<T, R> extends Function<T, R> {}

  class T {

    I<String, R<? extends S<Integer>, ? extends Q<?, Integer>, ?>> reader;
  }
}
