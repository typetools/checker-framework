package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.initialization.qual.*;

public class Object {
  public Object() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  // toString() is @SideEffectFree rather than @Pure because it returns a string
  // that differs according to ==, and @Deterministic is with respect to ==.
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  public final void wait(@UnknownInitialization @Raw Object this, long a1, int a2) throws InterruptedException { throw new RuntimeException("skeleton method"); }
  public final void wait(@UnknownInitialization @Raw Object this) throws InterruptedException { throw new RuntimeException("skeleton method"); }

  private static native void registerNatives();
  @Pure public final native Class<?> getClass(@UnknownInitialization @Raw Object this);
  @Pure public native int hashCode();
  @SideEffectFree protected native Object clone() throws CloneNotSupportedException;
  public final native void notify();
  public final native void notifyAll();
  public final native void wait(long timeout) throws InterruptedException;
  protected void finalize() throws Throwable { throw new RuntimeException("skeleton method"); }
}
