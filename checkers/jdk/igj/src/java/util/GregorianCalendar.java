package java.util;
import checkers.igj.quals.*;

@I
public class GregorianCalendar extends @I Calendar{
    private static final long serialVersionUID = 0L;
  public final static int BC = 0;
  public final static int AD = 1;
  public GregorianCalendar() @AssignsFields  { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(@ReadOnly TimeZone a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(@ReadOnly Locale a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(@ReadOnly TimeZone a1, @ReadOnly Locale a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(int a1, int a2, int a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(int a1, int a2, int a3, int a4, int a5) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public GregorianCalendar(int a1, int a2, int a3, int a4, int a5, int a6) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void setGregorianChange(@ReadOnly Date a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public final Date getGregorianChange() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean isLeapYear(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void add(int a1, int a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void roll(int a1, boolean a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void roll(int a1, int a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int getMinimum(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getMaximum(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getGreatestMinimum(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getLeastMaximum(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getActualMinimum(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getActualMaximum(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @ReadOnly TimeZone getTimeZone() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setTimeZone(@ReadOnly TimeZone a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
