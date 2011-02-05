package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public interface ListIterator<E extends @Nullable Object> extends Iterator<E> {
  public abstract boolean hasNext();
  public abstract E next();
  public abstract boolean hasPrevious();
  public abstract E previous();
  public abstract int nextIndex();
  public abstract int previousIndex();
  public abstract void remove();
  public abstract void set(E a1);
  public abstract void add(E a1);
}
