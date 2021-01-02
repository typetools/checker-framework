// Test case for issue 496:
// https://github.com/typetools/checker-framework/issues/496

import java.util.Optional;

public class Issue496 {

    public static class Entity<T> {
        public final T value;
        public final Class<T> cls;

        public Entity(T value, Class<T> cls) {
            this.value = value;
            this.cls = cls;
        }
    }

    public static <T> Optional<Entity<T>> testCase(Class<T> targetClass) {
        return Optional.<T>empty().map((T val) -> new Entity<T>(val, targetClass));
    }
}
