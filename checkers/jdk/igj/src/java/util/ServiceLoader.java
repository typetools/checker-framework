package java.util;
import checkers.igj.quals.*;

@Immutable
public final class ServiceLoader<S> implements @Immutable java.lang.Iterable<S> {
  protected ServiceLoader() @ReadOnly {}
  public void reload() { throw new RuntimeException("skeleton method"); }
  public @Immutable java.util.Iterator<S> iterator() { throw new RuntimeException("skeleton method"); }
  public static <S> java.util.ServiceLoader<S> load(java.lang.Class<S> a1, java.lang.ClassLoader a2) { throw new RuntimeException("skeleton method"); }
  public static <S> java.util.ServiceLoader<S> load(java.lang.Class<S> a1) { throw new RuntimeException("skeleton method"); }
  public static <S> java.util.ServiceLoader<S> loadInstalled(java.lang.Class<S> a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
