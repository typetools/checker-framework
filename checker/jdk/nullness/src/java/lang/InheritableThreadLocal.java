package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;


// See comment in ThreadLocal class about type parameter annotation.
public class InheritableThreadLocal<@Nullable T> extends ThreadLocal<T> {
  public InheritableThreadLocal() { throw new RuntimeException("skeleton method"); }
}
