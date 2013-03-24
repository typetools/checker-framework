import checkers.nullness.quals.*;

class Pair<S extends @Nullable Object, T extends @Nullable Object> {
    static <S extends @Nullable Object, T extends @Nullable Object>
    Pair<S, T> of(S p1, T p2) { return new Pair<S, T>(); }
}

class Test1<X extends @Nullable Object> {
    Pair<@Nullable X, @Nullable X> test1(@Nullable X p) {
        return Pair.<@Nullable X, @Nullable X>of(p, (X) null);
    }
}

/** TODO: infer method type arguments from assignment context.
class Test2<X extends @Nullable Object> {
    Pair<@Nullable X, @Nullable X> test1(@Nullable X p) {
        return Pair.of(p, (X) null);
    }
}
*/