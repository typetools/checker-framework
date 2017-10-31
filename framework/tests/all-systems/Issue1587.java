abstract class Issue1587 {
    static class MyObject {}

    interface Six<T extends Six<T, R>, R> {
        T d();

        Iterable<R> q();
    }

    abstract Six<?, MyObject> e(Object entity);

    public void method(MyObject one) {
        g(e(one).d().q());
    }

    abstract Iterable<MyObject> g(Iterable<MyObject> r);
}
