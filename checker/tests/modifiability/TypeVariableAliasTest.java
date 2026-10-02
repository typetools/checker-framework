import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.MaybeGrowable;
import org.checkerframework.checker.modifiability.qual.MaybeReplaceable;
import org.checkerframework.checker.modifiability.qual.MaybeShrinkable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;
import org.checkerframework.checker.modifiability.qual.Unreplaceable;

/**
 * A type variable may be instantiated by any subtype of its upper bound, including a subtype that
 * also implements an unrelated interface. Unless the upper bound is a subtype of a type that has a
 * capability, such as {@code List} for replacement or {@code Collection} for growth,
 * {@code @Modifiable} and {@code @Unmodifiable} on the type variable make no claim about that
 * capability.
 */
public class TypeVariableAliasTest {

  static <T> void takeModifiable(@Modifiable T t) {}

  static <T> void takeUnmodifiable(@Unmodifiable T t) {}

  static <T extends Collection<String>> void takeModifiableCollection(@Modifiable T t) {}

  static <T extends List<String>> void takeModifiableList(@Modifiable T t) {}

  static <T extends List<String>> void takeUnmodifiableList(@Unmodifiable T t) {}

  void unboundedTypeVariable(
      @Modifiable Set<String> modifiableSet,
      @Unmodifiable Set<String> unmodifiableSet,
      Map.@Modifiable Entry<String, String> modifiableEntry,
      @Modifiable List<String> modifiableList) {
    takeModifiable(modifiableSet);
    takeUnmodifiable(unmodifiableSet);
    takeModifiable(modifiableEntry);
    takeModifiable(modifiableList);
  }

  void collectionBound(@Modifiable Set<String> modifiableSet) {
    takeModifiableCollection(modifiableSet);
  }

  <T> void unboundedMeaning(@Modifiable T t) {
    @MaybeGrowable @MaybeShrinkable @MaybeReplaceable T canonical = t;
    // :: error: [assignment]
    @Growable T growable = t;
  }

  <T extends Collection<String>> void collectionBoundMeaning(@Modifiable T t) {
    // `Collection` has no subtype that cannot grow or shrink, but `Set` cannot be replaced into.
    @Growable @Shrinkable @MaybeReplaceable T canonical = t;
    // :: error: [assignment]
    @Replaceable T replaceable = t;
  }

  // No subtype of `List` lacks any of these capabilities, so the aliases keep their full meaning.
  <T extends List<String>> void listBoundMeaning(@Modifiable T t, @Unmodifiable T u) {
    @Growable @Shrinkable @Replaceable T canonical = t;
    @Unreplaceable T unreplaceable = u;
  }

  void listBound(
      @Modifiable ArrayList<String> modifiable, @Unmodifiable List<String> unmodifiable) {
    takeModifiableList(modifiable);
    takeUnmodifiableList(unmodifiable);
    // :: error: [argument]
    takeModifiableList(unmodifiable);
  }

  static <T extends AbstractCollection<String>> void takeModifiableAbstractCollection(
      @Modifiable T t) {}

  static <T extends Collection<String> & Serializable> void takeModifiableSerializableCollection(
      @Modifiable T t) {}

  // `HashSet` is a subtype of both bounds, though `Set` is a subtype of neither.
  void unrelatedBound(@Modifiable HashSet<String> modifiableSet) {
    takeModifiableAbstractCollection(modifiableSet);
    takeModifiableSerializableCollection(modifiableSet);
  }

  <T extends AbstractCollection<String>> void abstractCollectionBoundMeaning(@Modifiable T t) {
    @Growable @Shrinkable @MaybeReplaceable T canonical = t;
    // :: error: [assignment]
    @Replaceable T replaceable = t;
  }

  <T extends Collection<String> & Serializable> void intersectionBoundMeaning(@Modifiable T t) {
    @Growable @Shrinkable @MaybeReplaceable T canonical = t;
    // :: error: [assignment]
    @Replaceable T replaceable = t;
  }
}
