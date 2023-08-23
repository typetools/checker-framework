import java.util.Iterator;
import java.util.function.Consumer;

class Issue6116<T> {
  private final Iterator<? extends T> it;

  public Issue6116(Iterator<? extends T> iterator) {
    this.it = iterator;
  }

  public void test(Consumer<? super T> action) {
    it.forEachRemaining(action);
  }
}
