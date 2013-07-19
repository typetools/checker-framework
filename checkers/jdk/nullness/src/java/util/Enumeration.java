package java.util;
import checkers.nullness.quals.Nullable;

public interface Enumeration<E extends @Nullable Object> {
  public abstract boolean hasMoreElements();
  public abstract E nextElement();
}
