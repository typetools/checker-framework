package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class InheritableThreadLocal<T> extends ThreadLocal<T> {
  public InheritableThreadLocal() { throw new RuntimeException("skeleton method"); }
}
