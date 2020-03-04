package java.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public interface ListIterator<E> extends Iterator<E> {
  @Pure
  public abstract boolean hasNext();
  public abstract E next();
  @Pure
  public abstract boolean hasPrevious();
  public abstract E previous();
  @Pure
  public abstract int nextIndex();
  @Pure
  public abstract int previousIndex();
  public abstract void remove();
  public abstract void set(E a1);
  public abstract void add(E a1);
}
