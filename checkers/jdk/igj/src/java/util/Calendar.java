package java.util;
import checkers.igj.quals.*;

@I
public abstract class Calendar implements @I java.io.Serializable, @I java.lang.Cloneable, @I java.lang.Comparable<@ReadOnly java.util.Calendar> {
  public final static int ERA = 0;
  public final static int YEAR = 1;
  public final static int MONTH = 2;
  public final static int WEEK_OF_YEAR = 3;
  public final static int WEEK_OF_MONTH = 4;
  public final static int DATE = 5;
  public final static int DAY_OF_MONTH = 5;
  public final static int DAY_OF_YEAR = 6;
  public final static int DAY_OF_WEEK = 7;
  public final static int DAY_OF_WEEK_IN_MONTH = 8;
  public final static int AM_PM = 9;
  public final static int HOUR = 10;
  public final static int HOUR_OF_DAY = 11;
  public final static int MINUTE = 12;
  public final static int SECOND = 13;
  public final static int MILLISECOND = 14;
  public final static int ZONE_OFFSET = 15;
  public final static int DST_OFFSET = 16;
  public final static int FIELD_COUNT = 17;
  public final static int SUNDAY = 1;
  public final static int MONDAY = 2;
  public final static int TUESDAY = 3;
  public final static int WEDNESDAY = 4;
  public final static int THURSDAY = 5;
  public final static int FRIDAY = 6;
  public final static int SATURDAY = 7;
  public final static int JANUARY = 0;
  public final static int FEBRUARY = 1;
  public final static int MARCH = 2;
  public final static int APRIL = 3;
  public final static int MAY = 4;
  public final static int JUNE = 5;
  public final static int JULY = 6;
  public final static int AUGUST = 7;
  public final static int SEPTEMBER = 8;
  public final static int OCTOBER = 9;
  public final static int NOVEMBER = 10;
  public final static int DECEMBER = 11;
  public final static int UNDECIMBER = 12;
  public final static int AM = 0;
  public final static int PM = 1;
  public final static int ALL_STYLES = 0;
  public final static int SHORT = 1;
  public final static int LONG = 2;
  public static @I java.util.Calendar getInstance() { throw new RuntimeException("skeleton method"); }
  public static @I java.util.Calendar getInstance(@ReadOnly java.util.TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public static @I java.util.Calendar getInstance(@ReadOnly java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  public static @I java.util.Calendar getInstance(@ReadOnly java.util.TimeZone a1, java.util.Locale a2) { throw new RuntimeException("skeleton method"); }
  public static synchronized java.util.Locale @ReadOnly [] getAvailableLocales() { throw new RuntimeException("skeleton method"); }
  public final @I java.util.Date getTime() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public final void setTime(java.util.Date a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public long getTimeInMillis() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setTimeInMillis(long a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int get(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void set(int a1, int a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public final void set(int a1, int a2, int a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public final void set(int a1, int a2, int a3, int a4, int a5) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public final void set(int a1, int a2, int a3, int a4, int a5, int a6) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public final void clear() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public final void clear(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public final boolean isSet(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.String getDisplayName(int a1, int a2, java.util.Locale a3) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.util.Map<java.lang.String, java.lang.Integer> getDisplayNames(int a1, int a2, java.util.Locale a3) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly  { throw new RuntimeException("skeleton method"); }
  public int hashCode() @ReadOnly  { throw new RuntimeException("skeleton method"); }
  public boolean before(java.lang.Object a1) @ReadOnly  { throw new RuntimeException("skeleton method"); }
  public boolean after(java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int compareTo(@ReadOnly java.util.Calendar a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public abstract void add(int a1, int a2) @Mutable;
  public abstract void roll(int a1, boolean a2);
  public void roll(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void setTimeZone(java.util.TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public java.util.TimeZone getTimeZone() { throw new RuntimeException("skeleton method"); }
  public void setLenient(boolean a1) { throw new RuntimeException("skeleton method"); }
  public boolean isLenient() { throw new RuntimeException("skeleton method"); }
  public void setFirstDayOfWeek(int a1) { throw new RuntimeException("skeleton method"); }
  public int getFirstDayOfWeek() { throw new RuntimeException("skeleton method"); }
  public void setMinimalDaysInFirstWeek(int a1) { throw new RuntimeException("skeleton method"); }
  public int getMinimalDaysInFirstWeek() { throw new RuntimeException("skeleton method"); }
  public abstract int getMinimum(int a1);
  public abstract int getMaximum(int a1);
  public abstract int getGreatestMinimum(int a1);
  public abstract int getLeastMaximum(int a1);
  public int getActualMinimum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getActualMaximum(int a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
