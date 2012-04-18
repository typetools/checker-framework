package java.util;
import checkers.igj.quals.*;

@Immutable
public final class ServiceLoader<S> implements @Immutable Iterable<S> {
  protected ServiceLoader(@ReadOnly ServiceLoader<S> this) {}
  public void reload() { throw new RuntimeException("skeleton method"); }
  public @Immutable Iterator<S> iterator() { throw new RuntimeException("skeleton method"); }
  public static <S> ServiceLoader<S> load(Class<S> a1, ClassLoader a2) { throw new RuntimeException("skeleton method"); }
  public static <S> ServiceLoader<S> load(Class<S> a1) { throw new RuntimeException("skeleton method"); }
  public static <S> ServiceLoader<S> loadInstalled(Class<S> a1) { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
}
