import java.util.Enumeration;
import java.util.Iterator;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public final class StaticIteratorSE<T> implements Iterator<T> {
  Enumeration<T> e;

  public StaticIteratorSE(Enumeration<T> e) {
    this.e = e;
  }

  public boolean hasNext() {
    return e.hasMoreElements();
  }

  @SideEffectsOnly("this")
  public T next() {
    return e.nextElement();
  }

  @SideEffectsOnly("this")
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
