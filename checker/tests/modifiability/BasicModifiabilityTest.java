import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.MaybeModifiable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;
import org.checkerframework.checker.modifiability.qual.UnmodifiableParam;

// this checkes the correctness of annotation in List.java and ONLY the initialization of ArrayList
public class BasicModifiabilityTest {

  void testBasicModifiable() {
    // Modifiable collections should allow mutation
    List<String> modifiableList = new ArrayList<>();
    modifiableList.add("test");
    modifiableList.remove(0);

    // Unmodifiable collections should not allow mutation
    @Unmodifiable List<String> unmodifiableList = List.of("test1", "test2");
    // :: error: [method.invocation]
    unmodifiableList.add("test3");
    // :: error: [method.invocation]
    unmodifiableList.remove(0);
    // :: error: [method.invocation]
    unmodifiableList.remove("test1");
  }

  void testUnmodifiableFactoryMethods() {
    // These should be inferred as @Unmodifiable
    List<String> list1 = List.of("a", "b");
    // :: error: [method.invocation]
    list1.add("c");

    // Explicit unmodifiable type + unmodifiable factory
    @Unmodifiable List<String> list1u = List.of("x", "y");

    // :: error: [assignment]
    @Modifiable List<String> cannotBeMod1 = List.of("m1", "m2");

    List<String> list2 = List.copyOf(new ArrayList<>());
    // :: error: [method.invocation]
    list2.add("c");
    // :: error: [method.invocation]
    list2.remove(0);

    List<String> src = new ArrayList<>();
    src.add("s");

    @Unmodifiable List<String> list2u = List.copyOf(src);
    // :: error: [assignment]
    @Modifiable List<String> cannotBeMod2 = List.copyOf(src);
  }

  void testModifiableFactoryMethods() {
    // This should be inferred as @Modifiable
    List<String> modifiableList = new ArrayList<>();
    modifiableList.add("test");
    modifiableList.remove(0);
  }

  void testToArrayModifiability(
      @Modifiable List<String> modList,
      @Unmodifiable List<String> unmodList,
      @MaybeModifiable List<String> anyList) {

    modList.toArray();
    unmodList.toArray();
    anyList.toArray();

    String[] fromMod = modList.toArray(new String[0]);
    String[] fromUnmod = unmodList.toArray(new String[0]);
    String[] fromAny = anyList.toArray(new String[0]);
  }

  void testIteratorPolymorphic(
      @Modifiable List<String> modList, @Unmodifiable List<String> unmodList) {

    @MaybeModifiable Iterator<String> itMod = modList.iterator();

    // And the iterator from an unmodifiable list is unmodifiable:
    @Unmodifiable Iterator<String> itUnmod = unmodList.iterator();

    // Trying to treat the unmodifiable iterator as modifiable should be rejected.
    // :: error: [assignment]
    @Modifiable Iterator<String> badIt = unmodList.iterator();
  }

  void testMutatingBulkOperations(
      @Modifiable @IteratorPolyMod List<String> mod, @Unmodifiable List<String> unmod) {
    List<String> other = new ArrayList<>();
    other.add("x");

    mod.addAll(other);
    mod.removeAll(other);
    mod.retainAll(other);

    // :: error: [method.invocation]
    unmod.addAll(other);
    // :: error: [method.invocation]
    unmod.removeAll(other);
    // :: error: [method.invocation]
    unmod.retainAll(other);
  }

  void testUnmodifiableParamMutation(@UnmodifiableParam List<String> unmodParam, String value) {
    // :: error: [method.invocation]
    unmodParam.add(value);
    // :: error: [method.invocation]
    unmodParam.remove(value);
    // :: error: [method.invocation]
    unmodParam.set(0, value);
  }

  void testReplaceAll(
      @IteratorPolyMod @Modifiable List<String> mod, @Unmodifiable List<String> unmod) {
    mod.replaceAll(s -> s + "!");

    // :: error: [method.invocation]
    unmod.replaceAll(s -> s + "!");
  }

  void testSubList(@Modifiable List<String> mod, @Unmodifiable List<String> unmod) {
    List<String> sub = mod.subList(0, mod.size());
    sub.addAll(List.of("test")); //  OK because sub is modifiable

    // :: error: [assignment]
    @Modifiable List<String> sub2 = unmod.subList(0, unmod.size());
  }
}
