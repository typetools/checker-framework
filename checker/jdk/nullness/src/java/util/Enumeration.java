package java.util;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public interface Enumeration<E> {
  @Pure
  public abstract boolean hasMoreElements();
  public abstract E nextElement();
}
