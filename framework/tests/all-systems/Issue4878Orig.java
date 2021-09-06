import java.util.List;

interface M4878Orig {}

class A4878Orig<T extends M4878Orig, F> {
    void p(T v) {}
}

abstract class S<T extends M4878Orig> {
    abstract List<A4878Orig<T, ?>> p();
}

class Issue4878Orig {

    @SuppressWarnings("unchecked")
    <T extends M4878Orig> void f(S<?> s, T v) {
        s.p().forEach(
                        field -> {
                            Object o = field;
                            A4878Orig<T, ?> typedField = (A4878Orig<T, ?>) field;
                            typedField.p(v);
                        });
    }
}
