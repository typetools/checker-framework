package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class TimerTask implements Runnable{
  protected TimerTask() {}
  public abstract void run();
  public boolean cancel() { throw new RuntimeException("skeleton method"); }
  public long scheduledExecutionTime() { throw new RuntimeException("skeleton method"); }
}
