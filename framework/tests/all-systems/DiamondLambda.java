import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.modifiability.qual.Modifiable;

public class DiamondLambda {
  void foo(@Modifiable Map<String, Set<String>> map, String sequence) {
    Set<String> set = map.computeIfAbsent(sequence, __ -> new HashSet<>());
  }
}
