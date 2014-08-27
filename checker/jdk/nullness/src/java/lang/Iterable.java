package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;


public abstract interface Iterable<T extends @Nullable Object> {
  public abstract java.util.Iterator<T> iterator();
}
