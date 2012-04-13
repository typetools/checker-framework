package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class SimpleTimeZone extends TimeZone{
    private static final long serialVersionUID = 0L;
  public final static int WALL_TIME = 0;
  public final static int STANDARD_TIME = 1;
  public final static int UTC_TIME = 2;
  public SimpleTimeZone(int a1, String a2) { throw new RuntimeException("skeleton method"); }
  public SimpleTimeZone(int a1, String a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, int a10) { throw new RuntimeException("skeleton method"); }
  public SimpleTimeZone(int a1, String a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, int a10, int a11) { throw new RuntimeException("skeleton method"); }
  public SimpleTimeZone(int a1, String a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9, int a10, int a11, int a12, int a13) { throw new RuntimeException("skeleton method"); }
  public void setStartYear(int a1) { throw new RuntimeException("skeleton method"); }
  public void setStartRule(int a1, int a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public void setStartRule(int a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void setStartRule(int a1, int a2, int a3, int a4, boolean a5) { throw new RuntimeException("skeleton method"); }
  public void setEndRule(int a1, int a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public void setEndRule(int a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public void setEndRule(int a1, int a2, int a3, int a4, boolean a5) { throw new RuntimeException("skeleton method"); }
  public int getOffset(long a1) { throw new RuntimeException("skeleton method"); }
  public int getOffset(int a1, int a2, int a3, int a4, int a5, int a6) { throw new RuntimeException("skeleton method"); }
  public int getRawOffset() { throw new RuntimeException("skeleton method"); }
  public void setRawOffset(int a1) { throw new RuntimeException("skeleton method"); }
  public void setDSTSavings(int a1) { throw new RuntimeException("skeleton method"); }
  public int getDSTSavings() { throw new RuntimeException("skeleton method"); }
  public boolean useDaylightTime() { throw new RuntimeException("skeleton method"); }
  public boolean inDaylightTime(Date a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasSameRules(TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }

  public Object clone() { throw new RuntimeException("skeleton method"); }
}
