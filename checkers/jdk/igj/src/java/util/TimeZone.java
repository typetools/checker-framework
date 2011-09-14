package java.util;
import checkers.igj.quals.*;

@I
public abstract class TimeZone implements java.io.Serializable, Cloneable {
  public final static int SHORT = 0;
  public final static int LONG = 1;
  public TimeZone() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public abstract int getOffset(int a1, int a2, int a3, int a4, int a5, int a6) @ReadOnly;
  public int getOffset(long a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public abstract void setRawOffset(int a1);
  public abstract int getRawOffset() @ReadOnly;
  public String getID() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setID(String a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName(Locale a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName(boolean a1, int a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String getDisplayName(boolean a1, int a2, Locale a3) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getDSTSavings() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public abstract boolean useDaylightTime() @ReadOnly;
  public abstract boolean inDaylightTime(@ReadOnly Date a1) @ReadOnly;
  public static synchronized TimeZone getTimeZone(String a1) { throw new RuntimeException("skeleton method"); }
  public static synchronized String[] getAvailableIDs(int a1) { throw new RuntimeException("skeleton method"); }
  public static synchronized String[] getAvailableIDs() { throw new RuntimeException("skeleton method"); }
  public static TimeZone getDefault() { throw new RuntimeException("skeleton method"); }
  public static void setDefault(TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasSameRules(TimeZone a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
