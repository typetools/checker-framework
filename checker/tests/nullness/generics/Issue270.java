import org.checkerframework.checker.nullness.qual.*;

// ::error: (bound)
public class Issue270<@Nullable TypeParam extends @NonNull Object> {
  public static void main() {

    // ::error: (type.argument)
    @Nullable Issue270<@Nullable String> strWAtv = null;
    // ::error: (type.argument)
    @Nullable Issue270<@NonNull Integer> intWAtv = null;
  }
}
