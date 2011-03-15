package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class GregorianCalendar extends Calendar {
    private static final long serialVersionUID = 0L;
  public final static int BC = 0;
  public final static int AD = 1;
  public GregorianCalendar() { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(Locale a1) { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(TimeZone a1, Locale a2) { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(int a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(int a1, int a2, int a3, int a4, int a5) { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(int a1, int a2, int a3, int a4, int a5, int a6) { throw new RuntimeException("skeleton method"); }
  public void setGregorianChange(Date a1) { throw new RuntimeException("skeleton method"); }
  public final Date getGregorianChange() { throw new RuntimeException("skeleton method"); }
  public boolean isLeapYear(int a1) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public void add(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void roll(int a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public void roll(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int getMinimum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getMaximum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getGreatestMinimum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getLeastMaximum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getActualMinimum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getActualMaximum(int a1) { throw new RuntimeException("skeleton method"); }
  public TimeZone getTimeZone() { throw new RuntimeException("skeleton method"); }
  public void setTimeZone(TimeZone a1) { throw new RuntimeException("skeleton method"); }
  public Object clone() { throw new RuntimeException("skeleton method"); }
}
