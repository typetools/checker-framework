import java.util.Spliterator;
import java.util.function.Consumer;

public interface Issue6060<R> extends Iterable<R> {

  default Spliterator<R> spliterator() {
    return Iterable.super.spliterator();
  }

  default void forEach(Consumer<? super R> action) {
    Iterable.super.forEach(action);
  }
}
