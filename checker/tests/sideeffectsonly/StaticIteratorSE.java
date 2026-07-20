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
    // `Enumeration.nextElement` has no purity annotation in the annotated JDK, so the checker
    // must assume that it modifies arbitrary state.
    // :: error: (purity.unknown.sideeffectsonly)
    return e.nextElement();
  }

  @SideEffectsOnly("this")
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
