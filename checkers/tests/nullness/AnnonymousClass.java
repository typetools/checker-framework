import checkers.nullness.quals.*;

class AnonymousClass {

    class Bound<X extends @NonNull Object> {}

    void test() {
        //:: error: (generic.argument.invalid)
        new Bound<@Nullable String>() {

        };
    }
}