package java.util;
import checkers.igj.quals.*;

@I
public abstract class TimeZone{
  public final static int SHORT = 0;
  public final static int LONG = 1;
  public TimeZone() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public abstract int getOffset(int a1, int a2, int a3, int a4, int a5, int a6) @ReadOnly;
  public int getOffset(long a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public abstract void setRawOffset(int a1);
  public abstract int getRawOffset() @ReadOnly;
  public java.lang.String getID() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setID(java.lang.String a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public final java.lang.String getDisplayName() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public final java.lang.String getDisplayName(java.util.Locale a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public final java.lang.String getDisplayName(boolean a1, int a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.String getDisplayName(boolean a1, int a2, java.util.Locale a3) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getDSTSavings() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public abstract boolean useDaylightTime() @ReadOnly;
  public abstract boolean inDaylightTime(@ReadOnly java.util.Date a1) @ReadOnly;
  public static synchronized java.util.TimeZone getTimeZone(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static synchronized java.lang.String[] getAvailableIDs(int a1) { throw new RuntimeException("skeleton method"); }
  public static synchronized java.lang.String[] getAvailableIDs() { throw new RuntimeException("skeleton method"); }
  public static java.util.TimeZone getDefault() { throw new RuntimeException("skeleton method"); }
  public static void setDefault(java.util.TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasSameRules(java.util.TimeZone a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
}
