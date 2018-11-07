package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ServiceLoader<S> implements Iterable<S> {
  protected ServiceLoader() {}
  public void reload() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree
  public Iterator<S> iterator() { throw new RuntimeException("skeleton method"); }
  public static <S> ServiceLoader<S> load(Class<S> a1, @Nullable ClassLoader a2) { throw new RuntimeException("skeleton method"); }
  public static <S> ServiceLoader<S> load(Class<S> a1) { throw new RuntimeException("skeleton method"); }
  public static <S> ServiceLoader<S> loadInstalled(Class<S> a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
}
