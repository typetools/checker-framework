package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;


// It is permitted to write a subclass that extends ThreadLocal<@NonNull MyType>,
// but in such a case:
//   * the subclass must override initialValue to return a non-null value
//   * the subclass needs to suppress a warning:
//     @SuppressWarnings("nullness:type.argument.type.incompatible") // initialValue returns non-null
public class ThreadLocal<@Nullable T> {
  public ThreadLocal() { throw new RuntimeException("skeleton method"); }
  public T get() { throw new RuntimeException("skeleton method"); }
  public void set(T a1) { throw new RuntimeException("skeleton method"); }
  public void remove() { throw new RuntimeException("skeleton method"); }
}
