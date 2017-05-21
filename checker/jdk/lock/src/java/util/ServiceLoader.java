package java.util;
import org.checkerframework.checker.lock.qual.*;

public final class ServiceLoader<S> implements Iterable<S> {
  protected ServiceLoader() {}
  public void reload() { throw new RuntimeException("skeleton method"); }
  public Iterator<S> iterator() { throw new RuntimeException("skeleton method"); }
  public static <S> ServiceLoader<S> load(Class<S> a1, ClassLoader a2) { throw new RuntimeException("skeleton method"); }
  public static <S> ServiceLoader<S> load(Class<S> a1) { throw new RuntimeException("skeleton method"); }
  public static <S> ServiceLoader<S> loadInstalled(Class<S> a1) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied ServiceLoader<S> this) { throw new RuntimeException("skeleton method"); }
}
