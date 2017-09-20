abstract class Super {
    abstract SuperInner<?> a();

    abstract static class SuperInner<T extends SuperInner<T>> {
        abstract T b();

        abstract Super c();
    }
}

abstract class Sub extends Super {
    // It is significant that this method specializes the
    // return type. If this returns SuperInner, no crash happens.
    @Override
    abstract SubInner<?> a();

    abstract static class SubInner<S extends SubInner<S>> extends Super.SuperInner<S> {
        // Crashes with this overridden method and passes without it.
        @Override
        abstract Super c();
    }
}
