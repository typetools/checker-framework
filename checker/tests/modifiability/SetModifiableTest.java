import java.util.HashSet;
import java.util.Set;
import java.util.Spliterator;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

// Tests for Set mutating and read-only methods with modifiability qualifiers.
public class SetModifiableTest {

  void testMutatingMethods() {
    @Modifiable Set<String> mod = new HashSet<>();
    mod.add("a");
    mod.addAll(Set.of("b", "c"));
    mod.remove("a");
    mod.removeAll(Set.of("b"));
    mod.retainAll(Set.of("c"));
    mod.clear();

    @Unmodifiable Set<String> unmod = Set.of("x", "y");
    // :: error: [method.invocation]
    unmod.add("z");
    // :: error: [method.invocation]
    unmod.addAll(Set.of("z"));
    // :: error: [method.invocation]
    unmod.remove("x");
    // :: error: [method.invocation]
    unmod.removeAll(Set.of("x"));
    // :: error: [method.invocation]
    unmod.retainAll(Set.of("y"));
    // :: error: [method.invocation]
    unmod.clear();
  }

  void testReadOnlyMethods() {
    Set<String> set = Set.of("a", "b");
    set.contains("a");
    set.containsAll(Set.of("a"));
    set.equals(Set.of("a", "b"));
    set.hashCode();
    set.isEmpty();
    set.size();
    Spliterator<String> sp = set.spliterator();
    Object[] arr = set.toArray();
    String[] strs = set.toArray(new String[0]);
    for (String s : set) {}
  }

  void testFactoryAssignments() {
    Set<String> inferred = Set.of("k");
    @Unmodifiable Set<String> explicitUnmod = Set.of("k");

    // :: error: [assignment]
    @Modifiable Set<String> cannotBeMod1 = Set.of("m1");

    Set<String> src = new HashSet<>();
    src.add("s");
    Set<String> copy = Set.copyOf(src);
    // :: error: [assignment]
    @Modifiable Set<String> cannotBeMod2 = Set.copyOf(src);
  }
}
