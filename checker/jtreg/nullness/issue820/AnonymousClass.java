import org.checkerframework.checker.nullness.qual.NonNull;

public class AnonymousClass {
    @NonNull Object error = null;

    void method() {
        Object effectivelyFinalLocal = new Object();
        Object a =
                new Object() {
                    void foo() {
                        @NonNull Object nonNull = effectivelyFinalLocal;
                    }
                };
    }
}
