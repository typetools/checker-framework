import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Issue1630 {
    static @Nullable String toString(Object o) {
        return null;
    }

    @SuppressWarnings("nullness") // Issue 979
    public static List<String> f(List<Integer> xs) {
        return xs != null
                ? xs.stream()
                        .map(Issue1630::toString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                : Collections.emptyList();
    }
}
