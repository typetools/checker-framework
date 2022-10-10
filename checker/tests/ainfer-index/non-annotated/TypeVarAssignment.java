public class TypeVarAssignment<T, S extends T> {
    T t;

    void foo(S s) {
        t = s;
    }
}
