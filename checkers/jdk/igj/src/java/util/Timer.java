package java.util;

import checkers.igj.quals.*;

@I
public class Timer{
  public Timer() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Timer(boolean a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Timer(String a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Timer(String a1, boolean a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void schedule(@Mutable TimerTask a1, long a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void schedule(@Mutable TimerTask a1, @Immutable Date a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void schedule(@Mutable TimerTask a1, long a2, long a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void schedule(@Mutable TimerTask a1, @Immutable Date a2, long a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void scheduleAtFixedRate(@Mutable TimerTask a1, long a2, long a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void scheduleAtFixedRate(@Mutable TimerTask a1, Date a2, long a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void cancel() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int purge() @AssignsFields { throw new RuntimeException("skeleton method"); }
}
