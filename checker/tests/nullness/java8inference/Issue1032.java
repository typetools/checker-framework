// Test case for issue #1032:
// https://github.com/typetools/checker-framework/issues/1032

import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.*;

public class Issue1032 {

  @SuppressWarnings("nullness")
  static @NonNull String castStringToNonNull(@Nullable String arg) {
    return (@NonNull String) arg;
  }

  Stream<@NonNull String> mapStringCast1(Stream<@Nullable String> arg) {
    return arg.map(Issue1032::castStringToNonNull);
  }

  @SuppressWarnings("nullness")
  static <T> @NonNull T castTToNonNull(@Nullable T arg) {
    return (@NonNull T) arg;
  }

  Stream<@NonNull String> mapStringCast2(Stream<@Nullable String> arg) {
    return arg.map(Issue1032::<String>castTToNonNull);
  }

  <T> Stream<@NonNull T> mapTCast(Stream<@Nullable T> arg) {
    // TODO: false postive
    // :: error: (return)
    return arg.map(Issue1032::<T>castTToNonNull);
  }
}
