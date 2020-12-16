import org.checkerframework.checker.nullness.qual.*;

public class Issue313 {
    class A<@NonNull T extends @Nullable Object> {}

    <@NonNull X extends @Nullable Object> void m() {
        new A<X>();
    }
}
