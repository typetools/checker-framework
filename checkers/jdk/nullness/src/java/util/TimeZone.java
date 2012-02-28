package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class TimeZone implements java.io.Serializable, Cloneable{
  public final static int SHORT = 0;
  public final static int LONG = 1;
  public TimeZone() { throw new RuntimeException("skeleton method"); }
  public abstract int getOffset(int a1, int a2, int a3, int a4, int a5, int a6);
  public int getOffset(long a1) { throw new RuntimeException("skeleton method"); }
  public abstract void setRawOffset(int a1);
  public abstract int getRawOffset();
  public String getID() { throw new RuntimeException("skeleton method"); }
  public void setID(String a1) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName() { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName(Locale a1) { throw new RuntimeException("skeleton method"); }
  public final String getDisplayName(boolean a1, int a2) { throw new RuntimeException("skeleton method"); }
  public String getDisplayName(boolean a1, int a2, Locale a3) { throw new RuntimeException("skeleton method"); }
  public int getDSTSavings() { throw new RuntimeException("skeleton method"); }
  public abstract boolean useDaylightTime();
  public abstract boolean inDaylightTime(Date a1);
  public static synchronized TimeZone getTimeZone(String a1) { throw new RuntimeException("skeleton method"); }
  public static synchronized String[] getAvailableIDs(int a1) { throw new RuntimeException("skeleton method"); }
  public static synchronized String[] getAvailableIDs() { throw new RuntimeException("skeleton method"); }
  public static TimeZone getDefault() { throw new RuntimeException("skeleton method"); }
  public static void setDefault(@Nullable TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasSameRules(@Nullable TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
