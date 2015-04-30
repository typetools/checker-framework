// Test case for issue 370: https://code.google.com/p/checker-framework/issues/detail?id=370

import java.util.*;
import org.checkerframework.checker.nullness.qual.Nullable;

class AnnotatedJdkTest {
  // This code should type-check because of the annotated JDK, which contains:
  //   class Arrays {
  //     public static <T> List<T> asList(T... a);
  //   }
  // That JDK annotation should be equivalent to
  //     public static <T extends @Nullable Object> List<T> asList(T... a);
  // because of the CLIMB-to-top defaulting rule.

  @Nullable List<@Nullable String> lns = Arrays.asList("foo", null, "bar");

  // TODO: we need a mechanism to express that asList returns
  // a non-null List when the parameter is non-empty. Or is
  // the result of "Arrays.asList()" non-null?
  // TODO: the description above is not quite right, as the type checkers
  // are not run on the annotated JDK and therefore CLIMB is not applied.
}
