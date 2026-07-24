import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class NonNullIteratorNext {
  interface MyIterator<E> extends java.util.Iterator<E> {
    @SideEffectsOnly("this")
    @NonNull E next();
  }

  interface MyList<E> extends java.util.Collection<E> {
    MyIterator<E> iterator();
  }

  <T> void forEachLoop(MyList<T> list) {
    for (T elem : list) {}
  }
}
