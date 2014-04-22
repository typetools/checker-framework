package java.util;
import org.checkerframework.checker.igj.qual.*;

@I
public abstract class TimerTask implements Runnable {
  protected TimerTask() {}
  public abstract void run(@Mutable TimerTask this);
  public boolean cancel(@Mutable TimerTask this) { throw new RuntimeException("skeleton method"); }
  public long scheduledExecutionTime(@ReadOnly TimerTask this) { throw new RuntimeException("skeleton method"); }
}
