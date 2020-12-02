package wildcards;

@SuppressWarnings("all") // the assignment is not legal in most checkers.
public class SameBoundsCrazy {
    static class Simple<Q> {}

    static class Gen<T, S extends Simple<T>> {}

    void use(Gen<Simple<?>, ? super Simple<Simple<?>>> g) {
        Gen<Simple<?>, Simple<Simple<?>>> s = g;
    }

    static class Gen2<T, S extends T> {}

    <F> void use2(Gen2<F, ? super F> g) {
        Gen2<F, F> f = g;
    }
}
