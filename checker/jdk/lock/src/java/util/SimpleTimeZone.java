package java.util;
import org.checkerframework.checker.lock.qual.*;

public class SimpleTimeZone extends TimeZone {
  private static final long serialVersionUID = 0L;
  public final static int WALL_TIME = 0;
  public final static int STANDARD_TIME = 1;
  public final static int UTC_TIME = 2;
  public SimpleTimeZone(int a1, String a2) { throw new RuntimeException("skeleton method"); }
  public SimpleTimeZone(int a1, String a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, int a10) { throw new RuntimeException("skeleton method"); }
  public SimpleTimeZone(int a1, String a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, int a10, int a11) { throw new RuntimeException("skeleton method"); }
  public SimpleTimeZone(int a1, String a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, int a10, int a11, int a12, int a13) { throw new RuntimeException("skeleton method"); }
  public void setStartYear(@GuardSatisfied SimpleTimeZone this, int a1) { throw new RuntimeException("skeleton method"); }
  public void setStartRule(@GuardSatisfied SimpleTimeZone this, int a1, int a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public void setStartRule(@GuardSatisfied SimpleTimeZone this, int a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void setStartRule(@GuardSatisfied SimpleTimeZone this, int a1, int a2, int a3, int a4, boolean a5) { throw new RuntimeException("skeleton method"); }
  public void setEndRule(@GuardSatisfied SimpleTimeZone this, int a1, int a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public void setEndRule(@GuardSatisfied SimpleTimeZone this, int a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void setEndRule(@GuardSatisfied SimpleTimeZone this, int a1, int a2, int a3, int a4, boolean a5) { throw new RuntimeException("skeleton method"); }
  public int getOffset(@GuardSatisfied SimpleTimeZone this, long a1) { throw new RuntimeException("skeleton method"); }
  public int getOffset(@GuardSatisfied SimpleTimeZone this, int a1, int a2, int a3, int a4, int a5, int a6) { throw new RuntimeException("skeleton method"); }
  public int getRawOffset(@GuardSatisfied SimpleTimeZone this) { throw new RuntimeException("skeleton method"); }
  public void setRawOffset(@GuardSatisfied SimpleTimeZone this, int a1) { throw new RuntimeException("skeleton method"); }
  public void setDSTSavings(@GuardSatisfied SimpleTimeZone this, int a1) { throw new RuntimeException("skeleton method"); }
  public int getDSTSavings(@GuardSatisfied SimpleTimeZone this) { throw new RuntimeException("skeleton method"); }
  public boolean useDaylightTime(@GuardSatisfied SimpleTimeZone this) { throw new RuntimeException("skeleton method"); }
  public boolean inDaylightTime(@GuardSatisfied SimpleTimeZone this, Date a1) { throw new RuntimeException("skeleton method"); }
   public synchronized int hashCode(@GuardSatisfied SimpleTimeZone this) { throw new RuntimeException("skeleton method"); }
   public boolean equals(@GuardSatisfied SimpleTimeZone this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasSameRules(TimeZone a1) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied SimpleTimeZone this) { throw new RuntimeException("skeleton method"); }

   public Object clone(@GuardSatisfied SimpleTimeZone this) { throw new RuntimeException("skeleton method"); }
}
