package java.lang;

import dataflow.quals.Pure;
import dataflow.quals.SideEffectFree;

import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

public class Object {
  public Object() { throw new RuntimeException("skeleton method"); }
  public @Pure boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public @Pure String toString() { throw new RuntimeException("skeleton method"); }
  public final void wait(@UnknownInitialization @Raw Object this, long a1, int a2) throws InterruptedException { throw new RuntimeException("skeleton method"); }
  public final void wait(@UnknownInitialization @Raw Object this) throws InterruptedException { throw new RuntimeException("skeleton method"); }

  private static native void registerNatives();
  public final native @Pure Class<?> getClass(@UnknownInitialization @Raw Object this);
  public native @Pure int hashCode();
  @SideEffectFree protected native Object clone() throws CloneNotSupportedException;
  public final native void notify();
  public final native void notifyAll();
  public final native void wait(long timeout) throws InterruptedException;
  protected void finalize() throws Throwable { throw new RuntimeException("skeleton method"); }
}
