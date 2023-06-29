import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.NonNull;

public class KeyForTypeVar<T extends @NonNull Object> {
  <@KeyForBottom E extends @KeyFor("#1") T> T method(Map<T, String> m, E key) {
    @NonNull String s = m.get(key);
    throw new RuntimeException();
  }
}
