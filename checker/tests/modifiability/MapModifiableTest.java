import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.modifiability.qual.MaybeModifiable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

// Tests for Map mutating and read-only methods with modifiability qualifiers.
public class MapModifiableTest {

  List<String> d;
  @Modifiable List<String> dm;
  @Unmodifiable List<String> du;

  void testBasicMutatingOperations() {
    @Modifiable Map<String, Integer> mod = new HashMap<>();
    mod.put("a", 1);
    mod.putIfAbsent("b", 2);
    mod.putAll(Map.of("c", 3));
    mod.remove("a");
    mod.remove("b", 2);
    mod.replace("c", 4);
    mod.replace("c", 4, 5);
    mod.replaceAll((k, v) -> v + 1);
    mod.compute("d", (k, v) -> (v == null) ? 1 : v + 1);
    mod.computeIfAbsent("e", k -> 7);
    mod.computeIfPresent("e", (k, v) -> v + 1);
    mod.merge("f", 1, (oldV, newV) -> oldV + newV);
    mod.clear();

    @Unmodifiable Map<String, Integer> unmod = Map.of("x", 1);
    // :: error: [method.invocation]
    unmod.put("y", 2);
    // :: error: [method.invocation]
    unmod.putIfAbsent("z", 3);
    // :: error: [method.invocation]
    unmod.putAll(Map.of("a", 1));
    // :: error: [method.invocation]
    unmod.remove("x");
    // :: error: [method.invocation]
    unmod.remove("x", 1);
    // :: error: [method.invocation]
    unmod.replace("x", 2);
    // :: error: [method.invocation]
    unmod.replace("x", 2, 3);
    // :: error: [method.invocation]
    unmod.replaceAll((k, v) -> v + 1);
    // :: error: [method.invocation]
    unmod.compute("g", (k, v) -> 1);
    // :: error: [method.invocation]
    unmod.computeIfAbsent("h", k -> 2);
    // :: error: [method.invocation]
    unmod.computeIfPresent("h", (k, v) -> v + 1);
    // :: error: [method.invocation]
    unmod.merge("i", 1, (oldV, newV) -> oldV + newV);
    // :: error: [method.invocation]
    unmod.clear();
  }

  void testReadOnlyOperations() {
    @Unmodifiable Map<String, Integer> map = Map.of("a", 1);
    map.get("a");
    map.getOrDefault("b", 0);
    map.containsKey("a");
    map.containsValue(1);
    map.isEmpty();
    map.size();
    map.hashCode();
    map.equals(Map.of("a", 1));
    @Unmodifiable Set<@Unmodifiable Entry<String, Integer>> entries = map.entrySet();
    Collection<Integer> vals = map.values();
    Set<String> keys = map.keySet();
    map.forEach((k, v) -> {});
  }

  void testFactoryMethodsAndAssignments() {
    // Map.of and Map.copyOf are unmodifiable.
    Map<String, Integer> inferred = Map.of("k", 1);
    @Unmodifiable Map<String, Integer> explicitUnmod = Map.of("k", 1);

    // :: error: [assignment]
    @Modifiable Map<String, Integer> cannotBeMod1 = Map.of("m1", 1);

    Map<String, Integer> src = new HashMap<>();
    src.put("s", 1);
    Map<String, Integer> copy = Map.copyOf(src);
    // :: error: [assignment]
    @Modifiable Map<String, Integer> cannotBeMod2 = Map.copyOf(src);
  }

  void testEntryModifiabilityLocals(@Modifiable Entry<String, Integer> modEntry) {
    modEntry.setValue(2);

    @Unmodifiable Entry<String, Integer> immEntry = Map.entry("k", 1);
    // :: error: [method.invocation]
    immEntry.setValue(5);
  }

  void testAnnotatedLocalsFromParams(
      @MaybeModifiable Map<String, Integer> any,
      @Modifiable Map<String, Integer> modSrc,
      @Unmodifiable Map<String, Integer> unmodSrc) {

    modSrc.put("p", 1);
    // :: error: [method.invocation]
    unmodSrc.put("p", 1);
    // depending on defaults, this may be an error:
    // :: error: [method.invocation]
    any.put("q", 2);
  }

  // this is the desired behavior.
  void foo10(Map<String, @Modifiable List<String>> m) {
    List<String> d1 = m.getOrDefault("hello", d);
    // :: error: [assignment]
    @Modifiable List<String> d2 = m.getOrDefault("hello", d);
    // :: error: [assignment]
    @Unmodifiable List<String> d3 = m.getOrDefault("hello", d);
    List<String> d4 = m.getOrDefault("hello", dm);
    @Modifiable List<String> d5 = m.getOrDefault("hello", dm);
    // :: error: [assignment]
    @Unmodifiable List<String> d6 = m.getOrDefault("hello", dm);
    List<String> d7 = m.getOrDefault("hello", du);
    // :: error: [assignment]
    @Modifiable List<String> d8 = m.getOrDefault("hello", du);
    // :: error: [assignment]
    @Unmodifiable List<String> d9 = m.getOrDefault("hello", du);
  }

  void foo11(Map<String, @Unmodifiable List<String>> um) {
    List<String> d1 = um.getOrDefault("hello", d);
    // :: error: [assignment]
    @Modifiable List<String> d2 = um.getOrDefault("hello", d);
    // :: error: [assignment]
    @Unmodifiable List<String> d3 = um.getOrDefault("hello", d);
    List<String> d4 = um.getOrDefault("hello", dm);
    // :: error: [assignment]
    @Modifiable List<String> d5 = um.getOrDefault("hello", dm);
    // :: error: [assignment]
    @Unmodifiable List<String> d6 = um.getOrDefault("hello", dm);
    List<String> d7 = um.getOrDefault("hello", du);
    // :: error: [assignment] :: error: [assignment] :: error: [assignment]
    @Modifiable List<String> d8 = um.getOrDefault("hello", du);
    @Unmodifiable List<String> d9 = um.getOrDefault("hello", du);
  }
}
