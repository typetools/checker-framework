// Test case for issue #1032:
// https://github.com/typetools/checker-framework/issues/1032
// @skip-test until the issue is fixed.

// @below-java8-jdk-skip-test

import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.*;

class StreamMap {

    @SuppressWarnings("nullness")
    static @NonNull String castStringToNonNull(@Nullable String arg) {
        return (@NonNull String) arg;
    }

    Stream<@NonNull String> mapStringCast1(Stream<@Nullable String> arg) {
        return arg.map(StreamMap::castStringToNonNull);
    }

    @SuppressWarnings("nullness")
    static <T> @NonNull T castTToNonNull(@Nullable T arg) {
        return (@NonNull T) arg;
    }

    Stream<@NonNull String> mapStringCast2(Stream<@Nullable String> arg) {
        return arg.map(StreamMap::<String>castTToNonNull);
    }

    <T> Stream<@NonNull T> mapTCast(Stream<@Nullable T> arg) {
        return arg.map(StreamMap::<T>castTToNonNull);
    }
}
