import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue4523 {

    interface InterfaceA<T extends InterfaceA<T>> extends InterfaceB<T> {}

    interface InterfaceB<T extends InterfaceB<T>> {
        @Nullable T g();
    }

    void f(InterfaceA<?> x) {
        InterfaceA<?> y = x.g() != null ? x.g() : x;
    }
}
