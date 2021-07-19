package inference.guava;

import java.util.Map;

public class Bug12 {

  private static Map<? extends Enum, LockGraphNode> getOrCreateNodes(
      Map<? extends Enum, LockGraphNode> existing, Map<? extends Enum, LockGraphNode> created) {
    return firstNonNull(existing, created);
  }

  public static <T> T firstNonNull(T first, T second) {
    throw new RuntimeException();
  }

  private static class LockGraphNode {}
}
