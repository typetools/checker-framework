import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresKeyForIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

@SuppressWarnings("contracts.conditional.postcondition")
public final class NoSuchMethodVisibility {

  @Pure
  private @Nullable Map<String, Integer> privateMap() {
    return null;
  }

  @EnsuresKeyForIf(expression = "#1", map = "privateMap()", result = true)
  public boolean isMapped(final String name) {
    return true;
  }

  @Pure
  public @Nullable Map<String, Integer> publicMap() {
    return null;
  }

  @EnsuresKeyForIf(expression = "#1", map = "publicMap()", result = true)
  public boolean isMapped2(final String name) {
    return true;
  }
}
