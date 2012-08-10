package java.lang;

import checkers.nonnull.quals.Nullable;


// See comment in ThreadLocal class about type parameter annotation.
public class InheritableThreadLocal<@Nullable T> extends ThreadLocal<T> {
  public InheritableThreadLocal() { throw new RuntimeException("skeleton method"); }
}
