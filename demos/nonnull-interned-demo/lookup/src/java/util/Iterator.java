package java.util;



public abstract interface Iterator<E> {
  public abstract boolean hasNext();
  public abstract @NonNull E next();
  public abstract void remove();
}
