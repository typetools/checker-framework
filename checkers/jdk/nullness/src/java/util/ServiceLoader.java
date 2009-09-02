package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class ServiceLoader<S> implements java.lang.Iterable<S> {
  public void reload() { throw new RuntimeException("skeleton method"); }
  public java.util.Iterator<S> iterator() { throw new RuntimeException("skeleton method"); }
  public static <S> java.util.ServiceLoader<S> load(java.lang.Class<S> a1, @Nullable java.lang.ClassLoader a2) { throw new RuntimeException("skeleton method"); }
  public static <S> java.util.ServiceLoader<S> load(java.lang.Class<S> a1) { throw new RuntimeException("skeleton method"); }
  public static <S> java.util.ServiceLoader<S> loadInstalled(java.lang.Class<S> a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
