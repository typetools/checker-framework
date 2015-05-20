package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface Iterable<T extends @Nullable Object> {
  java.util.Iterator<T> iterator();
}
