class Issue5008<T> {
    Issue5008(L<T> l) {}

    static class B extends Issue5008<Object> {
        B() {
            super(new L<>());
        }
    }

    static class L<T> {}
}
