package java.lang;

import org.checkerframework.checker.lock.qual.*;


public class ThreadGroup implements Thread.UncaughtExceptionHandler {
  public ThreadGroup(String a1) { throw new RuntimeException("skeleton method"); }
  public ThreadGroup(ThreadGroup a1, String a2) { throw new RuntimeException("skeleton method"); }
  public final String getName() { throw new RuntimeException("skeleton method"); }
  public final  ThreadGroup getParent() { throw new RuntimeException("skeleton method"); }
  public final int getMaxPriority() { throw new RuntimeException("skeleton method"); }
   public final boolean isDaemon(@GuardSatisfied ThreadGroup this) { throw new RuntimeException("skeleton method"); }
   public synchronized boolean isDestroyed(@GuardSatisfied ThreadGroup this) { throw new RuntimeException("skeleton method"); }
  public final void setDaemon(boolean a1) { throw new RuntimeException("skeleton method"); }
  public final void setMaxPriority(int a1) { throw new RuntimeException("skeleton method"); }
  public final boolean parentOf(ThreadGroup a1) { throw new RuntimeException("skeleton method"); }
  public final void checkAccess() { throw new RuntimeException("skeleton method"); }
  public int activeCount() { throw new RuntimeException("skeleton method"); }
  public int enumerate(Thread[] a1) { throw new RuntimeException("skeleton method"); }
  public int enumerate(Thread[] a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public int activeGroupCount() { throw new RuntimeException("skeleton method"); }
  public int enumerate(ThreadGroup[] a1) { throw new RuntimeException("skeleton method"); }
  public int enumerate(ThreadGroup[] a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public final void stop() { throw new RuntimeException("skeleton method"); }
  public final void interrupt() { throw new RuntimeException("skeleton method"); }
  public final void suspend() { throw new RuntimeException("skeleton method"); }
  public final void resume() { throw new RuntimeException("skeleton method"); }
  public final void destroy() { throw new RuntimeException("skeleton method"); }
  public void list() { throw new RuntimeException("skeleton method"); }
  public void uncaughtException(Thread a1, Throwable a2) { throw new RuntimeException("skeleton method"); }
  public boolean allowThreadSuspension(boolean a1) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied ThreadGroup this) { throw new RuntimeException("skeleton method"); }
}
