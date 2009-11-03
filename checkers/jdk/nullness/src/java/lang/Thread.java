package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Thread implements java.lang.Runnable {
  public enum State {
      NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING,TERMINATED;
  }
  public static abstract interface UncaughtExceptionHandler{
    public abstract void uncaughtException(java.lang.Thread a1, java.lang.Throwable a2);
  }
  public final static int MIN_PRIORITY = 1;
  public final static int NORM_PRIORITY = 5;
  public final static int MAX_PRIORITY = 10;
  public Thread() { throw new RuntimeException("skeleton method"); }
  public Thread(@Nullable java.lang.Runnable a1) { throw new RuntimeException("skeleton method"); }
  public Thread(@Nullable java.lang.ThreadGroup a1, @Nullable java.lang.Runnable a2) { throw new RuntimeException("skeleton method"); }
  public Thread(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public Thread(java.lang.ThreadGroup a1, java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public Thread(java.lang.Runnable a1, java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public Thread(java.lang.ThreadGroup a1, java.lang.Runnable a2, java.lang.String a3) { throw new RuntimeException("skeleton method"); }
  public Thread(java.lang.ThreadGroup a1, java.lang.Runnable a2, java.lang.String a3, long a4) { throw new RuntimeException("skeleton method"); }
  public static Thread currentThread() { throw new RuntimeException("skeleton method"); }
  public static void yield() { throw new RuntimeException("skeleton method"); }
  public static void sleep(long a1) throws java.lang.InterruptedException { throw new RuntimeException("skeleton method"); }
  public static void sleep(long a1, int a2) throws java.lang.InterruptedException { throw new RuntimeException("skeleton method"); }
  public synchronized void start() { throw new RuntimeException("skeleton method"); }
  public void run() { throw new RuntimeException("skeleton method"); }
  public final void stop() { throw new RuntimeException("skeleton method"); }
  public final synchronized void stop(java.lang.Throwable a1) { throw new RuntimeException("skeleton method"); }
  public void interrupt() { throw new RuntimeException("skeleton method"); }
  public static boolean interrupted() { throw new RuntimeException("skeleton method"); }
  public boolean isInterrupted() { throw new RuntimeException("skeleton method"); }
  public void destroy() { throw new RuntimeException("skeleton method"); }
  public final void suspend() { throw new RuntimeException("skeleton method"); }
  public final void resume() { throw new RuntimeException("skeleton method"); }
  public final void setPriority(int a1) { throw new RuntimeException("skeleton method"); }
  public final int getPriority() { throw new RuntimeException("skeleton method"); }
  public final void setName(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public final java.lang.String getName() { throw new RuntimeException("skeleton method"); }
  public final @Nullable java.lang.ThreadGroup getThreadGroup() { throw new RuntimeException("skeleton method"); }
  public static int activeCount() { throw new RuntimeException("skeleton method"); }
  public static int enumerate(java.lang.Thread[] a1) { throw new RuntimeException("skeleton method"); }
  public final synchronized void join(long a1) throws java.lang.InterruptedException { throw new RuntimeException("skeleton method"); }
  public final synchronized void join(long a1, int a2) throws java.lang.InterruptedException { throw new RuntimeException("skeleton method"); }
  public final void join() throws java.lang.InterruptedException { throw new RuntimeException("skeleton method"); }
  public static void dumpStack() { throw new RuntimeException("skeleton method"); }
  public final void setDaemon(boolean a1) { throw new RuntimeException("skeleton method"); }
  public final boolean isDaemon() { throw new RuntimeException("skeleton method"); }
  public final void checkAccess() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.ClassLoader getContextClassLoader() { throw new RuntimeException("skeleton method"); }
  public void setContextClassLoader(java.lang.ClassLoader a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StackTraceElement[] getStackTrace() { throw new RuntimeException("skeleton method"); }
  public static java.util.Map<java.lang.Thread, java.lang.StackTraceElement[]> getAllStackTraces() { throw new RuntimeException("skeleton method"); }
  public long getId() { throw new RuntimeException("skeleton method"); }
  public java.lang.Thread.State getState() { throw new RuntimeException("skeleton method"); }
  public static void setDefaultUncaughtExceptionHandler(@Nullable java.lang.Thread.UncaughtExceptionHandler a1) { throw new RuntimeException("skeleton method"); }
  public static @Nullable java.lang.Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() { throw new RuntimeException("skeleton method"); }
  public @Nullable java.lang.Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() { throw new RuntimeException("skeleton method"); }
  public void setUncaughtExceptionHandler(@Nullable java.lang.Thread.UncaughtExceptionHandler a1) { throw new RuntimeException("skeleton method"); }

  public static boolean holdsLock(Object a1) { throw new RuntimeException("skeleton method"); }
  public final native boolean isAlive();
  public native int countStackFrames();
}
