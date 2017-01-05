package java.util;

public interface Enumeration<E extends Object> {
  public abstract boolean hasMoreElements();
  public abstract E nextElement();
}
