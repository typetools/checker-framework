// Test case for issue 370: https://github.com/typetools/checker-framework/issues/370

import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AnnotatedJdkTest {
    // This code should type-check because of the annotated JDK, which contains:
    //   class Arrays {
    //     public static <T> List<T> asList(T... a);
    //   }
    // That JDK annotation should be equivalent to
    //     public static <T extends @Nullable Object> List<T> asList(T... a);
    // because of the CLIMB-to-top defaulting rule.

    List<@Nullable String> lns = Arrays.asList("foo", null, "bar");
}
