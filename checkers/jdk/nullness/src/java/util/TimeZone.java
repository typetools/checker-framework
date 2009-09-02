package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class TimeZone{
  public final static int SHORT = 0;
  public final static int LONG = 1;
  public TimeZone() { throw new RuntimeException("skeleton method"); }
  public abstract int getOffset(int a1, int a2, int a3, int a4, int a5, int a6);
  public int getOffset(long a1) { throw new RuntimeException("skeleton method"); }
  public abstract void setRawOffset(int a1);
  public abstract int getRawOffset();
  public java.lang.String getID() { throw new RuntimeException("skeleton method"); }
  public void setID(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public final java.lang.String getDisplayName() { throw new RuntimeException("skeleton method"); }
  public final java.lang.String getDisplayName(java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  public final java.lang.String getDisplayName(boolean a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.String getDisplayName(boolean a1, int a2, java.util.Locale a3) { throw new RuntimeException("skeleton method"); }
  public int getDSTSavings() { throw new RuntimeException("skeleton method"); }
  public abstract boolean useDaylightTime();
  public abstract boolean inDaylightTime(java.util.Date a1);
  public static synchronized java.util.TimeZone getTimeZone(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static synchronized java.lang.String[] getAvailableIDs(int a1) { throw new RuntimeException("skeleton method"); }
  public static synchronized java.lang.String[] getAvailableIDs() { throw new RuntimeException("skeleton method"); }
  public static java.util.TimeZone getDefault() { throw new RuntimeException("skeleton method"); }
  public static void setDefault(@Nullable java.util.TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public boolean hasSameRules(@Nullable java.util.TimeZone a1) { throw new RuntimeException("skeleton method"); }
}
