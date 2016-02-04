package java.lang;

import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.dataflow.qual.SideEffectFree;

// Strings are immutable, hence they have type @GuardedBy({}).
// Only static methods involving a mutable parameter (i.e. a parameter other than a primitive or String) are annotated below.
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
  public static String format(String a1, @GuardSatisfied Object... a2) { throw new RuntimeException("skeleton method"); }
  public static String format(java.util. @GuardSatisfied Locale a1, String a2, @GuardSatisfied Object... a3) { throw new RuntimeException("skeleton method"); }
  public static String valueOf(@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  int compareTo(Comparable<T> this,T a1);
}
