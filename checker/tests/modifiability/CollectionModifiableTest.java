import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.MaybeModifiable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

// Tests for Collection mutating and read-only methods with modifiability qualifiers.
public class CollectionModifiableTest {

  void testMutatingMethods() {
    @Modifiable Collection<String> mod = new ArrayList<>();
    mod.add("a");
    mod.addAll(List.of("b", "c"));
    mod.clear();
    mod.remove("a");
    mod.removeAll(List.of("b"));
    mod.retainAll(List.of("c"));
    mod.removeIf(s -> s.isEmpty());

    @Unmodifiable Collection<String> unmod = List.of("x", "y");
    // :: error: [method.invocation]
    unmod.add("z");
    // :: error: [method.invocation]
    unmod.addAll(List.of("z"));
    // :: error: [method.invocation]
    unmod.clear();
    // :: error: [method.invocation]
    unmod.remove("x");
    // :: error: [method.invocation]
    unmod.removeAll(List.of("x"));
    // :: error: [method.invocation]
    unmod.retainAll(List.of("y"));
    // :: error: [method.invocation]
    unmod.removeIf(s -> true);
  }

  void testReadOnlyMethods() {
    Collection<String> c = List.of("a", "b");
    boolean has = c.contains("a");
    boolean all = c.containsAll(List.of("a"));
    boolean eq = c.equals(List.of("a", "b"));
    int h = c.hashCode();
    boolean empty = c.isEmpty();
    int sz = c.size();
    Spliterator<String> sp = c.spliterator();
    Stream<String> st = c.stream();
    Stream<String> pst = c.parallelStream();
    Object[] arr = c.toArray();
    String[] strs = c.toArray(new String[0]);
    // iterate to exercise iterator() (non-mutating usage)
    for (String s : c) {}
  }

  void testFactoryAssignments() {
    // List.of returns an unmodifiable collection view
    Collection<String> inferred = List.of("k");
    @Unmodifiable Collection<String> explicitUnmod = List.of("k");

    // :: error: [assignment]
    @Modifiable Collection<String> cannotBeMod1 = List.of("m1");

    List<String> src = new ArrayList<>();
    src.add("s");
    Collection<String> copy = List.copyOf(src);
    // :: error: [assignment]
    @Modifiable Collection<String> cannotBeMod2 = List.copyOf(src);
  }

  void testAnnotatedLocalsFromParams(
      @Modifiable @IteratorPolyMod Collection<String> mod,
      @Unmodifiable Collection<String> unmod,
      @MaybeModifiable Collection<String> unknown) {
    mod.add("p");
    // :: error: [method.invocation]
    unmod.add("p");
    // depending on defaults, this may be an error:
    // :: error: [method.invocation]
    unknown.add("q");
  }
}
