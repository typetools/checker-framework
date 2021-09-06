import java.util.List;

class Issue4878 {
    <Q> void f(S4878<?> s, Q v) {
        s.p().forEach(param -> {});
    }
}

class A4878<L, E> {}

abstract class S4878<T> {
    abstract List<A4878<T, ?>> p();
}
