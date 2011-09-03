package java.util;
import checkers.igj.quals.*;

@I
public class SimpleTimeZone extends TimeZone {
    private static final long serialVersionUID = 0L;
  public final static int WALL_TIME = 0;
  public final static int STANDARD_TIME = 1;
  public final static int UTC_TIME = 2;
  public SimpleTimeZone(int a1, String a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public SimpleTimeZone(int a1, String a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, int a10) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public SimpleTimeZone(int a1, String a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, int a10, int a11) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public SimpleTimeZone(int a1, String a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, int a10, int a11, int a12, int a13) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void setStartYear(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void setStartRule(int a1, int a2, int a3, int a4) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void setStartRule(int a1, int a2, int a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void setStartRule(int a1, int a2, int a3, int a4, boolean a5) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void setEndRule(int a1, int a2, int a3, int a4) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void setEndRule(int a1, int a2, int a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void setEndRule(int a1, int a2, int a3, int a4, boolean a5) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int getOffset(long a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getOffset(int a1, int a2, int a3, int a4, int a5, int a6) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getRawOffset() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setRawOffset(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void setDSTSavings(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int getDSTSavings() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean useDaylightTime() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean inDaylightTime(Date a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean hasSameRules(@ReadOnly TimeZone a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
