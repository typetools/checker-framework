package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Thread implements Runnable {
  public enum State {
      NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING,TERMINATED;
  }
  public static abstract interface UncaughtExceptionHandler{
    public abstract void uncaughtException(Thread a1, Throwable a2);
  }
  public final static int MIN_PRIORITY = 1;
  public final static int NORM_PRIORITY = 5;
  public final static int MAX_PRIORITY = 10;
  public Thread() { throw new RuntimeException("skeleton method"); }
  public Thread(@Nullable Runnable a1) { throw new RuntimeException("skeleton method"); }
  public Thread(@Nullable ThreadGroup a1, @Nullable Runnable a2) { throw new RuntimeException("skeleton method"); }
  public Thread(String a1) { throw new RuntimeException("skeleton method"); }
  public Thread(ThreadGroup a1, String a2) { throw new RuntimeException("skeleton method"); }
  public Thread(Runnable a1, String a2) { throw new RuntimeException("skeleton method"); }
  public Thread(ThreadGroup a1, Runnable a2, String a3) { throw new RuntimeException("skeleton method"); }
  public Thread(ThreadGroup a1, Runnable a2, String a3, long a4) { throw new RuntimeException("skeleton method"); }
  public static Thread currentThread() { throw new RuntimeException("skeleton method"); }
  public static void yield() { throw new RuntimeException("skeleton method"); }
  public static void sleep(long a1) throws InterruptedException { throw new RuntimeException("skeleton method"); }
  public static void sleep(long a1, int a2) throws InterruptedException { throw new RuntimeException("skeleton method"); }
  public synchronized void start() { throw new RuntimeException("skeleton method"); }
  public void run() { throw new RuntimeException("skeleton method"); }
  public final void stop() { throw new RuntimeException("skeleton method"); }
  public final synchronized void stop(Throwable a1) { throw new RuntimeException("skeleton method"); }
  public void interrupt() { throw new RuntimeException("skeleton method"); }
  public static boolean interrupted() { throw new RuntimeException("skeleton method"); }
  public boolean isInterrupted() { throw new RuntimeException("skeleton method"); }
  public void destroy() { throw new RuntimeException("skeleton method"); }
  public final void suspend() { throw new RuntimeException("skeleton method"); }
  public final void resume() { throw new RuntimeException("skeleton method"); }
  public final void setPriority(int a1) { throw new RuntimeException("skeleton method"); }
  public final int getPriority() { throw new RuntimeException("skeleton method"); }
  public final void setName(String a1) { throw new RuntimeException("skeleton method"); }
  public final String getName() { throw new RuntimeException("skeleton method"); }
  public final @Nullable ThreadGroup getThreadGroup() { throw new RuntimeException("skeleton method"); }
  public static int activeCount() { throw new RuntimeException("skeleton method"); }
  public static int enumerate(Thread[] a1) { throw new RuntimeException("skeleton method"); }
  public final synchronized void join(long a1) throws InterruptedException { throw new RuntimeException("skeleton method"); }
  public final synchronized void join(long a1, int a2) throws InterruptedException { throw new RuntimeException("skeleton method"); }
  public final void join() throws InterruptedException { throw new RuntimeException("skeleton method"); }
  public static void dumpStack() { throw new RuntimeException("skeleton method"); }
  public final void setDaemon(@Raw Thread this, boolean a1) { throw new RuntimeException("skeleton method"); }
  public final boolean isDaemon() { throw new RuntimeException("skeleton method"); }
  public final void checkAccess() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public @Nullable ClassLoader getContextClassLoader() { throw new RuntimeException("skeleton method"); }
  public void setContextClassLoader(ClassLoader a1) { throw new RuntimeException("skeleton method"); }
  public StackTraceElement[] getStackTrace() { throw new RuntimeException("skeleton method"); }
  public static java.util.Map<Thread, StackTraceElement[]> getAllStackTraces() { throw new RuntimeException("skeleton method"); }
  public long getId() { throw new RuntimeException("skeleton method"); }
  public Thread.State getState() { throw new RuntimeException("skeleton method"); }
  public static void setDefaultUncaughtExceptionHandler(@Nullable Thread.UncaughtExceptionHandler a1) { throw new RuntimeException("skeleton method"); }
  public static @Nullable Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() { throw new RuntimeException("skeleton method"); }
  public @Nullable Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() { throw new RuntimeException("skeleton method"); }
  public void setUncaughtExceptionHandler(@Nullable Thread.UncaughtExceptionHandler a1) { throw new RuntimeException("skeleton method"); }

  public static boolean holdsLock(Object a1) { throw new RuntimeException("skeleton method"); }
  public final native boolean isAlive();
  public native int countStackFrames();
}
