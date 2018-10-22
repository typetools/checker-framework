package java.util;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Enumeration<E> {
  public abstract boolean hasMoreElements();
  public abstract E nextElement();
}
