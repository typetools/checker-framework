import java.util.Collection;
import java.util.NavigableMap;

public abstract class Issue3792<T> {
    static class Instant {}

    void method(
            NavigableMap<Instant, Collection<T>> contents,
            Instant minTimestamp,
            Instant limitTimestamp) {
        contents.subMap(minTimestamp, true, limitTimestamp, false).entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(v -> of(v, e.getKey())));
    }

    abstract Object of(T v, Instant key);
}
