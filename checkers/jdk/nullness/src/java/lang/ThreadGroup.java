package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class ThreadGroup{
  public ThreadGroup(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public ThreadGroup(java.lang.ThreadGroup a1, @Nullable java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public final @Nullable java.lang.String getName() { throw new RuntimeException("skeleton method"); }
  public final @Nullable  java.lang.ThreadGroup getParent() { throw new RuntimeException("skeleton method"); }
  public final int getMaxPriority() { throw new RuntimeException("skeleton method"); }
  public final boolean isDaemon() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean isDestroyed() { throw new RuntimeException("skeleton method"); }
  public final void setDaemon(boolean a1) { throw new RuntimeException("skeleton method"); }
  public final void setMaxPriority(int a1) { throw new RuntimeException("skeleton method"); }
  public final boolean parentOf(java.lang.ThreadGroup a1) { throw new RuntimeException("skeleton method"); }
  public final void checkAccess() { throw new RuntimeException("skeleton method"); }
  public int activeCount() { throw new RuntimeException("skeleton method"); }
  public int enumerate(java.lang.Thread[] a1) { throw new RuntimeException("skeleton method"); }
  public int enumerate(java.lang.Thread[] a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public int activeGroupCount() { throw new RuntimeException("skeleton method"); }
  public int enumerate(java.lang.ThreadGroup[] a1) { throw new RuntimeException("skeleton method"); }
  public int enumerate(java.lang.ThreadGroup[] a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public final void stop() { throw new RuntimeException("skeleton method"); }
  public final void interrupt() { throw new RuntimeException("skeleton method"); }
  public final void suspend() { throw new RuntimeException("skeleton method"); }
  public final void resume() { throw new RuntimeException("skeleton method"); }
  public final void destroy() { throw new RuntimeException("skeleton method"); }
  public void list() { throw new RuntimeException("skeleton method"); }
  public void uncaughtException(java.lang.Thread a1, java.lang.Throwable a2) { throw new RuntimeException("skeleton method"); }
  public boolean allowThreadSuspension(boolean a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
