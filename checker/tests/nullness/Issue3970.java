import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3970 {

  public interface InterfaceA<T extends InterfaceA<T>> extends InterfaceB<T> {}

  public interface InterfaceB<T extends InterfaceB<T>> {
    int f();

    @Nullable T g();
  }

  void t(InterfaceA<?> a) {
    if (a.f() == 1) {
      // :: error: (assignment.type.incompatible)
      InterfaceA<?> a2 = a.g();
    }
  }
}
