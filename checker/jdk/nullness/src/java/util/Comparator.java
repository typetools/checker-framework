package java.util;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

// Javadoc says: "a comparator may optionally permit comparison of null
// arguments, while maintaining the requirements for an equivalence relation."
public interface Comparator<T extends @Nullable Object> {
  public abstract int compare(T a1, T a2);
  @Pure public abstract boolean equals(@Nullable Object a1);
}
