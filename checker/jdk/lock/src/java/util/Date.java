package java.util;
import org.checkerframework.checker.lock.qual.*;

public class Date implements java.io.Serializable, Cloneable, Comparable<Date> {
  private static final long serialVersionUID = 0;
  public Date() { throw new RuntimeException("skeleton method"); }
  public Date(long a1) { throw new RuntimeException("skeleton method"); }
  public Date(int a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public Date(int a1, int a2, int a3, int a4, int a5) { throw new RuntimeException("skeleton method"); }
  public Date(int a1, int a2, int a3, int a4, int a5, int a6) { throw new RuntimeException("skeleton method"); }
  public Date(String a1) { throw new RuntimeException("skeleton method"); }
  public static long UTC(int a1, int a2, int a3, int a4, int a5, int a6) { throw new RuntimeException("skeleton method"); }
  public static long parse(String a1) { throw new RuntimeException("skeleton method"); }
  public int getYear(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
  public void setYear(@GuardSatisfied Date this, int a1) { throw new RuntimeException("skeleton method"); }
  public int getMonth(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
  public void setMonth(@GuardSatisfied Date this, int a1) { throw new RuntimeException("skeleton method"); }
  public int getDate(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
  public void setDate(@GuardSatisfied Date this, int a1) { throw new RuntimeException("skeleton method"); }
  public int getDay(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
  public int getHours(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
  public void setHours(@GuardSatisfied Date this, int a1) { throw new RuntimeException("skeleton method"); }
  public int getMinutes(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
  public void setMinutes(@GuardSatisfied Date this, int a1) { throw new RuntimeException("skeleton method"); }
  public int getSeconds(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
  public void setSeconds(@GuardSatisfied Date this, int a1) { throw new RuntimeException("skeleton method"); }
  public long getTime(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
  public void setTime(@GuardSatisfied Date this, long a1) { throw new RuntimeException("skeleton method"); }
  public boolean before(@GuardSatisfied Date this, Date a1) { throw new RuntimeException("skeleton method"); }
  public boolean after(@GuardSatisfied Date this, Date a1) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@GuardSatisfied Date this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(@GuardSatisfied Date this, @GuardSatisfied Date a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
  public String toLocaleString() { throw new RuntimeException("skeleton method"); }
  public String toGMTString() { throw new RuntimeException("skeleton method"); }
  public int getTimezoneOffset(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
}
