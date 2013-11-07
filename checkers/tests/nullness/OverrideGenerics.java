import checkers.quals.*;
import checkers.nullness.quals.*;

class Super<S extends @Nullable Object> {
    public void m(S p) {}
}

class Impl1<T extends @NonNull Object>
    extends Super<T> {
    public void m(T p) {}
}

class Impl2<T>
    extends Super<T> {
    public void m(T p) {}
}
