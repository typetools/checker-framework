import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.modifiability.qual.*;

public class PolyShrinkableTest {

  void testMapKeySet() {
    // Modifiable map: keySet() returns @Shrinkable. Cannot add or replace.
    @Modifiable Map<String, String> modMap = new java.util.HashMap<>();
    @Shrinkable Set<String> modKeySet = modMap.keySet();
    // :: error: [assignment]
    @Growable Set<String> modKeySetG = modMap.keySet(); // Cannot add

    // Unmodifiable map: keySet() returns @Unmodifiable. Cannot shrink, add, or replace.
    @Unmodifiable Map<String, String> unmodMap = Map.of("a", "b");
    @Unmodifiable Set<String> unmodKeySet = unmodMap.keySet();
    // :: error: [assignment]
    @Shrinkable Set<String> unmodKeySetS = unmodMap.keySet(); // Cannot shrink
    // :: error: [assignment]
    @Growable Set<String> unmodKeySetG = unmodMap.keySet(); // Cannot add
  }

  void testMapValues() {
    // Modifiable map: values() returns @Shrinkable. Cannot add.
    @Modifiable Map<String, String> modMap = new java.util.HashMap<>();
    @Shrinkable Collection<String> modVals = modMap.values();
    // :: error: [assignment]
    @Growable Collection<String> modValsG = modMap.values(); // Cannot add

    // Unmodifiable map: values() returns @Unmodifiable. Cannot shrink and add.
    @Unmodifiable Map<String, String> unmodMap = Map.of("a", "b");
    @Unmodifiable Collection<String> unmodVals = unmodMap.values();
    // :: error: [assignment]
    @Shrinkable Collection<String> unmodValsS = unmodMap.values(); // Cannot shrink
    // :: error: [assignment]
    @Growable Collection<String> unmodValsG = unmodMap.values(); // Cannot add
  }

  void testMapEntrySet() {
    // entrySet(): Set is @PolyShrinkable (cannot add), Entry is @PolyModifiable.

    // Modifiable map: Set is @Shrinkable, Entry is @Modifiable.
    @Modifiable Map<String, String> modMap = new java.util.HashMap<>();
    @Shrinkable Set<Map.@Modifiable Entry<String, String>> modEntries = modMap.entrySet();
    // :: error: [assignment]
    @Growable Set<Map.@Modifiable Entry<String, String>> modEntriesG = modMap.entrySet(); // Set cannot add

    // Unmodifiable map: Set is @Unmodifiable, Entry is @Unmodifiable.
    @Unmodifiable Map<String, String> unmodMap = Map.of("a", "b");
    @Unmodifiable Set<Map.@Unmodifiable Entry<String, String>> unmodEntries = unmodMap.entrySet();
    // :: error: [assignment]
    @Shrinkable Set<Map.@Unmodifiable Entry<String, String>> unmodEntriesS = unmodMap.entrySet();
    // :: error: [assignment]
    @Growable Set<Map.@Unmodifiable Entry<String, String>> unmodEntriesG = unmodMap.entrySet();
  }
}
