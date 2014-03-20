package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;


public final class Boolean implements java.io.Serializable, Comparable<Boolean> {
  private static final long serialVersionUID = 0;
  public final static Boolean TRUE = true;
  public final static Boolean FALSE = false;
  public final static Class<Boolean> TYPE = null;
  public Boolean(boolean a1) { throw new RuntimeException("skeleton method"); }
  public Boolean(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean parseBoolean(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean booleanValue() { throw new RuntimeException("skeleton method"); }
  @Pure public static Boolean valueOf(boolean a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static Boolean valueOf(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static String toString(boolean a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean getBoolean(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int compareTo(Boolean a1) { throw new RuntimeException("skeleton method"); }
}
