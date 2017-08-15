package java.lang;


import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.initialization.qual.*;

public class Object {
  public Object() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@GuardSatisfied Object this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public String toString(@GuardSatisfied Object this) { throw new RuntimeException("skeleton method"); }
  public final void wait(Object this, long a1, int a2) throws InterruptedException { throw new RuntimeException("skeleton method"); }
  public final void wait(Object this) throws InterruptedException { throw new RuntimeException("skeleton method"); }

  private static native void registerNatives();
  public final native Class<? extends Object> getClass(@GuardSatisfied Object this);
  public native int hashCode(@GuardSatisfied Object this);
  protected native Object clone(@GuardSatisfied Object this) throws CloneNotSupportedException;
  public final native void notify();
  public final native void notifyAll();
  public final native void wait(long timeout) throws InterruptedException;
  protected void finalize() throws Throwable { throw new RuntimeException("skeleton method"); }
}
