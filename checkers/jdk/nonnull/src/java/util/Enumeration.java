package java.util;
import checkers.nonnull.quals.Nullable;

public interface Enumeration<E extends @Nullable Object> {
  public abstract boolean hasMoreElements();
  public abstract E nextElement();
}
