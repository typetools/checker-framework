package java.util;
import checkers.igj.quals.*;

@I
public abstract class TimerTask{
  public abstract void run() @Mutable;
  public boolean cancel() @Mutable { throw new RuntimeException("skeleton method"); }
  public long scheduledExecutionTime() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
