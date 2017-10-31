import org.checkerframework.checker.nullness.qual.*;

// ::error: (bound.type.incompatible)
class Issue270<@Nullable TypeParam extends @NonNull Object> {
    public static void main() {

        // ::error: (type.argument.type.incompatible)
        @Nullable Issue270<@Nullable String> strWAtv = null;
        // ::error: (type.argument.type.incompatible)
        @Nullable Issue270<@NonNull Integer> intWAtv = null;
    }
}
