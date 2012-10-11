package java.util;
import checkers.igj.quals.*;

@I
public abstract class TimerTask implements Runnable {
  protected TimerTask(@ReadOnly TimerTask this) {}
  public abstract void run(@Mutable TimerTask this);
  public boolean cancel(@Mutable TimerTask this) { throw new RuntimeException("skeleton method"); }
  public long scheduledExecutionTime(@ReadOnly TimerTask this) { throw new RuntimeException("skeleton method"); }
}
