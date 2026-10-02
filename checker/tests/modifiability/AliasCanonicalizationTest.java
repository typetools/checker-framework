import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.MaybeGrowable;
import org.checkerframework.checker.modifiability.qual.MaybeReplaceable;
import org.checkerframework.checker.modifiability.qual.MaybeShrinkable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;
import org.checkerframework.checker.modifiability.qual.Unshrinkable;

class AliasCanonicalizationTest {

  void modifiableIterator(@Modifiable Iterator<String> iterator) {
    @MaybeGrowable @Shrinkable @MaybeReplaceable Iterator<String> canonical = iterator;
    @Shrinkable Iterator<String> shrinkable = iterator;
    iterator.remove();
  }

  void unmodifiableIterator(@Unmodifiable Iterator<String> iterator) {
    @MaybeGrowable @Unshrinkable @MaybeReplaceable Iterator<String> canonical = iterator;
    // :: error: [method.invocation]
    iterator.remove();
  }

  void modifiableSet(@Modifiable Set<String> set) {
    @Growable @Shrinkable @MaybeReplaceable Set<String> canonical = set;
    set.add("a");
    set.remove("a");
  }

  void modifiableQueue(@Modifiable Queue<String> queue) {
    @Growable @Shrinkable @MaybeReplaceable Queue<String> canonical = queue;
    queue.add("a");
    queue.remove();
  }

  void modifiableCollection(@Modifiable @IteratorPolyMod Collection<String> collection) {
    @Growable @Shrinkable @IteratorPolyMod @MaybeReplaceable Collection<String> canonical = collection;
    collection.add("a");
    collection.remove("a");
  }

  void modifiableEntry(Map.@Modifiable Entry<String, String> entry) {
    Map.@MaybeGrowable @MaybeShrinkable @Replaceable Entry<String, String> canonical = entry;
    entry.setValue("value");
  }

  void explicitIterator(@Growable @Shrinkable @Replaceable Iterator<String> iterator) {
    @Growable @Shrinkable @Replaceable Iterator<String> explicit = iterator;
    @Replaceable Iterator<String> replaceable = iterator;
    @Growable Iterator<String> growable = iterator;
  }

  void explicitReplaceOnStructurallyUnreplaceableTypes(
      @Replaceable Collection<String> collection,
      @Replaceable Set<String> set,
      @Replaceable Queue<String> queue) {
    @Replaceable Collection<String> explicitCollection = collection;
    @Replaceable Set<String> explicitSet = set;
    @Replaceable Queue<String> explicitQueue = queue;
  }

  void modifiableListIterator(@Modifiable ListIterator<String> iterator) {
    @Growable @Shrinkable @Replaceable ListIterator<String> canonical = iterator;
    iterator.add("a");
    iterator.set("b");
    iterator.remove();
  }
}
