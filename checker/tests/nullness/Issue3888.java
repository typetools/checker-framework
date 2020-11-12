import org.checkerframework.checker.nullness.qual.PolyNull;

abstract class Issue3888 {

    interface L<X> {}

    interface E {}

    public interface F<V, Y> {
        Y a(V v);
    }

    abstract <T> void f(F<T, Boolean> f);

    void c(F<E, @PolyNull L> o) {
        f((E vm) -> o.a(vm) == null);
    }
}
