package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
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
  public int getYear() { throw new RuntimeException("skeleton method"); }
  public void setYear(int a1) { throw new RuntimeException("skeleton method"); }
  public int getMonth() { throw new RuntimeException("skeleton method"); }
  public void setMonth(int a1) { throw new RuntimeException("skeleton method"); }
  public int getDate() { throw new RuntimeException("skeleton method"); }
  public void setDate(int a1) { throw new RuntimeException("skeleton method"); }
  public int getDay() { throw new RuntimeException("skeleton method"); }
  public int getHours() { throw new RuntimeException("skeleton method"); }
  public void setHours(int a1) { throw new RuntimeException("skeleton method"); }
  public int getMinutes() { throw new RuntimeException("skeleton method"); }
  public void setMinutes(int a1) { throw new RuntimeException("skeleton method"); }
  public int getSeconds() { throw new RuntimeException("skeleton method"); }
  public void setSeconds(int a1) { throw new RuntimeException("skeleton method"); }
  public long getTime() { throw new RuntimeException("skeleton method"); }
  public void setTime(long a1) { throw new RuntimeException("skeleton method"); }
  public boolean before(Date a1) { throw new RuntimeException("skeleton method"); }
  public boolean after(Date a1) { throw new RuntimeException("skeleton method"); }
   public boolean equals(@GuardSatisfied Date this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int compareTo(@GuardSatisfied Date this,@GuardSatisfied Date a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
  public String toLocaleString() { throw new RuntimeException("skeleton method"); }
  public String toGMTString() { throw new RuntimeException("skeleton method"); }
  public int getTimezoneOffset() { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied Date this) { throw new RuntimeException("skeleton method"); }
}
