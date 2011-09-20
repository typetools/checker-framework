package java.util;
import checkers.igj.quals.*;

@I
public abstract class TimeZone implements java.io.Serializable, Cloneable {
  public final static int SHORT = 0;
  public final static int LONG = 1;
  public TimeZone(@AssignsFields TimeZone this) { throw new RuntimeException("skeleton method"); }
  public abstract int getOffset(@ReadOnly TimeZone this, int a1, int a2, int a3, int a4, int a5, int a6);
  public int getOffset(@ReadOnly TimeZone this, long a1) { throw new RuntimeException("skeleton method"); }
  public abstract void setRawOffset(int a1);
  public abstract int getRawOffset(@ReadOnly TimeZone this);
  public String getID(@ReadOnly TimeZone this) { throw new RuntimeException("skeleton method"); }
  public void setID(@AssignsFields TimeZone this, String a1) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName(@ReadOnly TimeZone this) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName(@ReadOnly TimeZone this, Locale a1) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName(@ReadOnly TimeZone this, boolean a1, int a2) { throw new RuntimeException("skeleton method"); }
  public String getDisplayName(@ReadOnly TimeZone this, boolean a1, int a2, Locale a3) { throw new RuntimeException("skeleton method"); }
  public int getDSTSavings(@ReadOnly TimeZone this) { throw new RuntimeException("skeleton method"); }
  public abstract boolean useDaylightTime(@ReadOnly TimeZone this);
  public abstract boolean inDaylightTime(@ReadOnly TimeZone this, @ReadOnly Date a1);
  public static synchronized TimeZone getTimeZone(String a1) { throw new RuntimeException("skeleton method"); }
  public static synchronized String[] getAvailableIDs(int a1) { throw new RuntimeException("skeleton method"); }
  public static synchronized String[] getAvailableIDs() { throw new RuntimeException("skeleton method"); }
  public static TimeZone getDefault() { throw new RuntimeException("skeleton method"); }
  public static void setDefault(TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasSameRules(@ReadOnly TimeZone this, TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
