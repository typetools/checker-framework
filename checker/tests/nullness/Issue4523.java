import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public class Issue4523 {

  interface InterfaceA<T extends InterfaceA<T>> extends InterfaceB<T> {}

  interface InterfaceB<T extends InterfaceB<T>> {
    @Pure
    @Nullable T g();
  }

  void f(InterfaceA<?> x) {
    InterfaceA<?> y = x.g() != null ? x.g() : x;
  }
}
