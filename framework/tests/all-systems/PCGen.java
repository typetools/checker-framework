import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class PCGen {
  private Map<String, String> activeBonusMap = new HashMap<>();

  void method(String prefix) {
    final Set<String> keys =
        activeBonusMap.keySet().stream()
            .filter(fullyQualifiedBonusType -> fullyQualifiedBonusType.startsWith(prefix))
            .collect(Collectors.toCollection(() -> new TreeSet<>()));
  }
}
