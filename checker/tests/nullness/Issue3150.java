// Test case for https://tinyurl.com/cfissue/3150 .

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3150 {
    void foo(@Nullable Object nble, @NonNull Object nn) {
        // :: error: (type.argument.type.incompatible)
        requireNonNull1(null);
        // :: error: (type.argument.type.incompatible)
        requireNonNull1(nble);
        requireNonNull1("hello");
        requireNonNull1(nn);
        // :: error: (argument.type.incompatible)
        requireNonNull2(null);
        // :: error: (argument.type.incompatible)
        requireNonNull2(nble);
        requireNonNull1("hello");
        requireNonNull1(nn);
    }

    public static <T extends @NonNull Object> T requireNonNull1(T obj) {
        return obj;
    }

    public static <T> @NonNull T requireNonNull2(@NonNull T obj) {
        return obj;
    }
}
