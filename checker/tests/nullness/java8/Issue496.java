// Test case for issue 496:
// https://github.com/typetools/checker-framework/issues/496

import java.util.Optional;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class Issue496 {

  public static class Entity<T> {
    public final T value;
    public final Class<T> cls;

    // Optional.map is @SideEffectFree, so the mapper below must be too, and calling this
    // constructor is side-effect-free only if it says so.
    @SideEffectFree
    public Entity(T value, Class<T> cls) {
      this.value = value;
      this.cls = cls;
    }
  }

  public static <T> Optional<Entity<T>> testCase(Class<T> targetClass) {
    return Optional.<T>empty().map((T val) -> new Entity<T>(val, targetClass));
  }
}
