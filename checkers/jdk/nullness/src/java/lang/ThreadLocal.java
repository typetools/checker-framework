package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class ThreadLocal<T> {
  public ThreadLocal() { throw new RuntimeException("skeleton method"); }
  public T get() { throw new RuntimeException("skeleton method"); }
  public void set(T a1) { throw new RuntimeException("skeleton method"); }
  public void remove() { throw new RuntimeException("skeleton method"); }
}
