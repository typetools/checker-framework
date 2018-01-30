package java.util;
import org.checkerframework.checker.lock.qual.*;

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
  public void setGregorianChange(@GuardSatisfied GregorianCalendar this, Date a1) { throw new RuntimeException("skeleton method"); }
  public final Date getGregorianChange() { throw new RuntimeException("skeleton method"); }
   public boolean isLeapYear(@GuardSatisfied GregorianCalendar this,int a1) { throw new RuntimeException("skeleton method"); }
   public boolean equals(@GuardSatisfied GregorianCalendar this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied GregorianCalendar this) { throw new RuntimeException("skeleton method"); }
  public void add(@GuardSatisfied GregorianCalendar this, int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void roll(@GuardSatisfied GregorianCalendar this, int a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public void roll(@GuardSatisfied GregorianCalendar this, int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int getMinimum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getMaximum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getGreatestMinimum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getLeastMaximum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getActualMinimum(int a1) { throw new RuntimeException("skeleton method"); }
  public int getActualMaximum(int a1) { throw new RuntimeException("skeleton method"); }
  public TimeZone getTimeZone() { throw new RuntimeException("skeleton method"); }
  public void setTimeZone(@GuardSatisfied GregorianCalendar this, TimeZone a1) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied GregorianCalendar this) { throw new RuntimeException("skeleton method"); }
}
