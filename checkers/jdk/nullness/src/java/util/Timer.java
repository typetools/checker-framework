package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Timer{
  public Timer() { throw new RuntimeException("skeleton method"); }
  public Timer(boolean a1) { throw new RuntimeException("skeleton method"); }
  public Timer(String a1) { throw new RuntimeException("skeleton method"); }
  public Timer(String a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public void schedule(TimerTask a1, long a2) { throw new RuntimeException("skeleton method"); }
  public void schedule(TimerTask a1, Date a2) { throw new RuntimeException("skeleton method"); }
  public void schedule(TimerTask a1, long a2, long a3) { throw new RuntimeException("skeleton method"); }
  public void schedule(TimerTask a1, Date a2, long a3) { throw new RuntimeException("skeleton method"); }
  public void scheduleAtFixedRate(TimerTask a1, long a2, long a3) { throw new RuntimeException("skeleton method"); }
  public void scheduleAtFixedRate(TimerTask a1, Date a2, long a3) { throw new RuntimeException("skeleton method"); }
  public void cancel() { throw new RuntimeException("skeleton method"); }
  public int purge() { throw new RuntimeException("skeleton method"); }
}
