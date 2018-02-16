package java.util;

import org.checkerframework.checker.lock.qual.*;

public abstract class Calendar implements java.io.Serializable, Cloneable, Comparable<Calendar> {
  protected Calendar() {}
    static final long       serialVersionUID = -1807547505821590642L;
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
  public static Calendar getInstance() { throw new RuntimeException("skeleton method"); }
  public static Calendar getInstance(TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public static Calendar getInstance(Locale a1) { throw new RuntimeException("skeleton method"); }
  public static Calendar getInstance(TimeZone a1, Locale a2) { throw new RuntimeException("skeleton method"); }
  public static synchronized Locale[] getAvailableLocales() { throw new RuntimeException("skeleton method"); }
  public final Date getTime(@GuardSatisfied Calendar this) { throw new RuntimeException("skeleton method"); }
  public final void setTime(@GuardSatisfied Calendar this, Date a1) { throw new RuntimeException("skeleton method"); }
  public long getTimeInMillis(@GuardSatisfied Calendar this) { throw new RuntimeException("skeleton method"); }
  public void setTimeInMillis(@GuardSatisfied Calendar this, long a1) { throw new RuntimeException("skeleton method"); }
  public int get(@GuardSatisfied Calendar this, int a1) { throw new RuntimeException("skeleton method"); }
  public void set(@GuardSatisfied Calendar this, int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public final void set(@GuardSatisfied Calendar this, int a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public final void set(@GuardSatisfied Calendar this, int a1, int a2, int a3, int a4, int a5) { throw new RuntimeException("skeleton method"); }
  public final void set(@GuardSatisfied Calendar this, int a1, int a2, int a3, int a4, int a5, int a6) { throw new RuntimeException("skeleton method"); }
  public final void clear(@GuardSatisfied Calendar this) { throw new RuntimeException("skeleton method"); }
  public final void clear(@GuardSatisfied Calendar this, int a1) { throw new RuntimeException("skeleton method"); }
  public final boolean isSet(@GuardSatisfied Calendar this, int a1) { throw new RuntimeException("skeleton method"); }
  public String getDisplayName(@GuardSatisfied Calendar this, int a1, int a2, Locale a3) { throw new RuntimeException("skeleton method"); }
  public Map<String, Integer> getDisplayNames(@GuardSatisfied Calendar this, int a1, int a2, Locale a3) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@GuardSatisfied Calendar this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied Calendar this) { throw new RuntimeException("skeleton method"); }
  public boolean before(Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean after(Object a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(@GuardSatisfied Calendar this, @GuardSatisfied Calendar a1) { throw new RuntimeException("skeleton method"); }
  public abstract void add(@GuardSatisfied Calendar this, int a1, int a2);
  public abstract void roll(@GuardSatisfied Calendar this, int a1, boolean a2);
  public void roll(@GuardSatisfied Calendar this, int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void setTimeZone(@GuardSatisfied Calendar this, TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public TimeZone getTimeZone(@GuardSatisfied Calendar this) { throw new RuntimeException("skeleton method"); }
  public void setLenient(@GuardSatisfied Calendar this, boolean a1) { throw new RuntimeException("skeleton method"); }
   public boolean isLenient(@GuardSatisfied Calendar this) { throw new RuntimeException("skeleton method"); }
  public void setFirstDayOfWeek(@GuardSatisfied Calendar this, int a1) { throw new RuntimeException("skeleton method"); }
  public int getFirstDayOfWeek(@GuardSatisfied Calendar this) { throw new RuntimeException("skeleton method"); }
  public void setMinimalDaysInFirstWeek(@GuardSatisfied Calendar this, int a1) { throw new RuntimeException("skeleton method"); }
  public int getMinimalDaysInFirstWeek(@GuardSatisfied Calendar this) { throw new RuntimeException("skeleton method"); }
  public abstract int getMinimum(@GuardSatisfied Calendar this, int a1);
  public abstract int getMaximum(@GuardSatisfied Calendar this, int a1);
  public abstract int getGreatestMinimum(@GuardSatisfied Calendar this, int a1);
  public abstract int getLeastMaximum(@GuardSatisfied Calendar this, int a1);
  public int getActualMinimum(@GuardSatisfied Calendar this, int a1) { throw new RuntimeException("skeleton method"); }
  public int getActualMaximum(@GuardSatisfied Calendar this, int a1) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied Calendar this) { throw new RuntimeException("skeleton method"); }
  private static final String[] FIELD_NAME = {
        "ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH",
        "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR",
        "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET",
        "DST_OFFSET"
  };
  static final String getFieldName(int field)  { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied Calendar this) { throw new RuntimeException("skeleton method"); }
}
