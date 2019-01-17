import org.checkerframework.checker.nullness.qual.*;

// public class ExtendsSubtypingIssue extends @NonNull Object {
//    public static void f(@Nullable ExtendsSubtypingIssue a) {
//    }
// }

public @NonNull class ExtendsSubtypingIssue {
    public static void f(@Nullable ExtendsSubtypingIssue a) {}
}
