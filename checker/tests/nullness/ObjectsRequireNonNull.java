// Test case for https://tinyurl.com/cfissue/3149 .

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ObjectsRequireNonNull {
    void foo(@Nullable Object nble, @NonNull Object nn) {
        // :: error: (argument.type.incompatible)
        Objects.requireNonNull(null);
        // :: error: (argument.type.incompatible)
        Objects.requireNonNull(nble);
        Objects.requireNonNull("hello");
        Objects.requireNonNull(nn);
    }
}
