class Issue421<IE> {
    abstract static class C<CE> {
        abstract X<? extends CE> getX();
    }

    interface X<T> {}

    abstract static class R<RE> {
        abstract boolean d(X<? extends RE> id);
    }

    private void f(C<IE> c, R<IE> r) {
        X<? extends IE> x = c.getX();
        boolean bval = r.d(x);
    }
}
