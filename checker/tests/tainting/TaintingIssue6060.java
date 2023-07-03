import java.util.Spliterator;
import java.util.function.Consumer;
import org.checkerframework.checker.tainting.qual.Untainted;

public interface TaintingIssue6060<R> extends Iterable<@Untainted R> {

  default Spliterator<@Untainted R> spliterator() {
    return Iterable.super.spliterator();
  }

  default Spliterator<R> spliterator2() {
    // :: error: (return)
    return Iterable.super.spliterator();
  }

  default Spliterator<R> spliterator3() {
    // :: error: (return)
    return this.spliterator();
  }

  // :: error: (override.param)
  default void forEach(Consumer<? super R> action) {
    Iterable.super.forEach(action);
  }
}
