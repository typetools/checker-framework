package java.util;

public interface Iterator<E extends Object> {
  public abstract boolean hasNext();
  public abstract E next();
  public abstract void remove();
}
