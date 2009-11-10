package java.util;
import checkers.igj.quals.*;

@I
public class Date implements @I java.io.Serializable, @I java.lang.Cloneable, @I java.lang.Comparable<@ReadOnly java.util.Date> {
    private static final long serialVersionUID = 0L;
  public Date() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Date(long a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Date(int a1, int a2, int a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Date(int a1, int a2, int a3, int a4, int a5) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Date(int a1, int a2, int a3, int a4, int a5, int a6) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Date(java.lang.String a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public static long UTC(int a1, int a2, int a3, int a4, int a5, int a6) { throw new RuntimeException("skeleton method"); }
  public static long parse(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public int getYear() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setYear(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int getMonth() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setMonth(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int getDate() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setDate(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int getDay() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getHours() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setHours(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int getMinutes() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setMinutes(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public int getSeconds() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setSeconds(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public long getTime() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void setTime(long a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public boolean before(@ReadOnly java.util.Date a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean after(@ReadOnly java.util.Date a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int compareTo(@ReadOnly java.util.Date a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.String toLocaleString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.String toGMTString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getTimezoneOffset() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
