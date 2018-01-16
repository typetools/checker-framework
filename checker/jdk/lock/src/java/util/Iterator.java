package java.util;

public interface Iterator<E extends Object> {
  public abstract boolean hasNext();
  public abstract E next(@GuardSatisfied Iterator<E> this);
  public abstract void remove(@GuardSatisfied Iterator<E> this);
}
