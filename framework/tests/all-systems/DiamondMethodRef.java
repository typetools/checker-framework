import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiamondMethodRef {
  @SuppressWarnings("lock:methodref.receiver") // True positive.
  void method(CharacterDisplay display) {
    display.getClassSet().stream()
        .map(
            pcClass ->
                IntStream.range(0, display.getLevel(pcClass))
                    .map(i -> 0)
                    .boxed()
                    .collect(
                        Collectors.toMap(
                            Function.identity(), hitDie -> 1, Integer::sum, LinkedHashMap::new)))
        .mapToInt(hdMap -> hdMap.entrySet().stream().mapToInt(Map.Entry::getValue).sum());
  }

  public static class CharacterDisplay {
    public Set<PCClass> getClassSet() {
      throw new RuntimeException();
    }

    public final int getLevel(PCClass pcc) {
      return 0;
    }

    public HitDie getLevelHitDie(PCClass pcClass, final int classLevel) {
      throw new RuntimeException();
    }
  }

  static class PCClass {}

  static class HitDie {
    int getDie() {
      return 0;
    }
  }
}
