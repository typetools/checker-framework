package java.util;

import checkers.igj.quals.*;

@I
public class Timer{
  public Timer() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Timer(boolean a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Timer(java.lang.String a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Timer(java.lang.String a1, boolean a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void schedule(@Mutable java.util.TimerTask a1, long a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void schedule(@Mutable java.util.TimerTask a1, @Immutable java.util.Date a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void schedule(@Mutable java.util.TimerTask a1, long a2, long a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void schedule(@Mutable java.util.TimerTask a1, @Immutable java.util.Date a2, long a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void scheduleAtFixedRate(@Mutable java.util.TimerTask a1, long a2, long a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void scheduleAtFixedRate(@Mutable java.util.TimerTask a1, java.util.Date a2, long a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void cancel() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int purge() @AssignsFields { throw new RuntimeException("skeleton method"); }
}
