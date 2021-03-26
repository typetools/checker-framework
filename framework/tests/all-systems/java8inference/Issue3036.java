import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Issue3036 {

  public Set<MyInnerClass> getDsData() {
    throw new RuntimeException();
  }

  public static class MyInnerClass {
    public int getKeyTag() {
      return 5;
    }

    public String getDigest() {
      return "";
    }
  }

  private void write(Stream<MyInnerClass> stream) {
    Function<MyInnerClass, ImmutableMap<String, ? extends Serializable>> mapper =
        dsData1 ->
            ImmutableMap.of(
                "keyTag", dsData1.getKeyTag(),
                "digest", dsData1.getDigest());

    List<Map<String, ?>> dsData =
        getDsData().stream()
            .map(
                dsData1 ->
                    ImmutableMap.of(
                        "keyTag", dsData1.getKeyTag(),
                        "digest", dsData1.getDigest()))
            .collect(Collectors.toList());
  }

  public static class ImmutableMap<K, V> extends HashMap<K, V> {
    public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2) {
      throw new RuntimeException();
    }
  }
}
