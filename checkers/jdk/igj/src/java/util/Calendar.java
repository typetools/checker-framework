package java.util;
import checkers.igj.quals.*;

@I
public abstract class Calendar implements @I java.io.Serializable, @I Cloneable, @I Comparable<@ReadOnly Calendar> {
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
  protected Calendar(@ReadOnly Calendar this) {}
  public static @I Calendar getInstance() { throw new RuntimeException("skeleton method"); }
  public static @I Calendar getInstance(@ReadOnly TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public static @I Calendar getInstance(@ReadOnly Locale a1) { throw new RuntimeException("skeleton method"); }
  public static @I Calendar getInstance(@ReadOnly TimeZone a1, Locale a2) { throw new RuntimeException("skeleton method"); }
  public static synchronized Locale @ReadOnly [] getAvailableLocales() { throw new RuntimeException("skeleton method"); }
  public final @I Date getTime(@ReadOnly Calendar this) { throw new RuntimeException("skeleton method"); }
  public final void setTime(@AssignsFields Calendar this, Date a1) { throw new RuntimeException("skeleton method"); }
  public long getTimeInMillis(@ReadOnly Calendar this) { throw new RuntimeException("skeleton method"); }
  public void setTimeInMillis(@AssignsFields Calendar this, long a1) { throw new RuntimeException("skeleton method"); }
  public int get(@ReadOnly Calendar this, int a1) { throw new RuntimeException("skeleton method"); }
  public void set(@AssignsFields Calendar this, int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public final void set(@AssignsFields Calendar this, int a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public final void set(@AssignsFields Calendar this, int a1, int a2, int a3, int a4, int a5) { throw new RuntimeException("skeleton method"); }
  public final void set(@AssignsFields Calendar this, int a1, int a2, int a3, int a4, int a5, int a6) { throw new RuntimeException("skeleton method"); }
  public final void clear(@AssignsFields Calendar this) { throw new RuntimeException("skeleton method"); }
  public final void clear(@AssignsFields Calendar this, int a1) { throw new RuntimeException("skeleton method"); }
  public final boolean isSet(@ReadOnly Calendar this, int a1) { throw new RuntimeException("skeleton method"); }
  public String getDisplayName(@ReadOnly Calendar this, int a1, int a2, Locale a3) { throw new RuntimeException("skeleton method"); }
  public Map<String, Integer> getDisplayNames(@ReadOnly Calendar this, int a1, int a2, Locale a3) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly Calendar this, @ReadOnly Object a1)  { throw new RuntimeException("skeleton method"); }
  public int hashCode(@ReadOnly Calendar this)  { throw new RuntimeException("skeleton method"); }
  public boolean before(@ReadOnly Calendar this, Object a1)  { throw new RuntimeException("skeleton method"); }
  public boolean after(@ReadOnly Calendar this, Object a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(@ReadOnly Calendar this, @ReadOnly Calendar a1) { throw new RuntimeException("skeleton method"); }
  public abstract void add(@Mutable Calendar this, int a1, int a2);
  public abstract void roll(int a1, boolean a2);
  public void roll(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void setTimeZone(TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public TimeZone getTimeZone() { throw new RuntimeException("skeleton method"); }
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
  public String toString() { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
