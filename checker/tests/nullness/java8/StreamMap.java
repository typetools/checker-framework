// @below-java8-jdk-skip-test

import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.*;

class StreamMap {

    @SuppressWarnings("nullness")
    static @NonNull String castToNonNull(@Nullable String arg) {
        return (@NonNull String) arg;
    }

    Stream<@NonNull String> mapCast(Stream<@Nullable String> arg) {
        return arg.map(StreamMap::castToNonNull);
    }
}
