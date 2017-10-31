import org.checkerframework.checker.fenum.qual.Fenum;

class CastsFenum {
    @Fenum("A") Object fa;

    void m(Object p, @Fenum("A") Object a) {
        fa = (Object) a;
        // :: error: (assignment.type.incompatible)
        fa = (Object) p;

        // TODO: How can we test the behavior for
        // instanceof? It should be the same as for casts.
        // if (p instanceof Object) {
    }
}
