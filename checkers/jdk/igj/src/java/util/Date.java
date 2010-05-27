package java.util;
import checkers.igj.quals.*;

@I
public class Date implements @I java.io.Serializable, @I Cloneable, @I Comparable<@ReadOnly Date> {
    private static final long serialVersionUID = 0L;
  public Date() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Date(long a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Date(int a1, int a2, int a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Date(int a1, int a2, int a3, int a4, int a5) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Date(int a1, int a2, int a3, int a4, int a5, int a6) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public Date(String a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public static long UTC(int a1, int a2, int a3, int a4, int a5, int a6) { throw new RuntimeException("skeleton method"); }
  public static long parse(String a1) { throw new RuntimeException("skeleton method"); }
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
  public boolean before(@ReadOnly Date a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean after(@ReadOnly Date a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int compareTo(@ReadOnly Date a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String toLocaleString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String toGMTString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int getTimezoneOffset() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I("N") Object clone() { throw new RuntimeException("skeleton method"); }
}
