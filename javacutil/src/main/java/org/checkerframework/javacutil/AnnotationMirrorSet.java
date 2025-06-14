package org.checkerframework.javacutil;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.plumelib.util.DeepCopyable;

/**
 * The Set interface defines many methods with respect to the equals method. This implementation of
 * Set violates those specifications, but fulfills the same property using {@link
 * AnnotationUtils#areSame} rather than equals.
 *
 * <p>For example, the specification for the contains(Object o) method says: "returns true if and
 * only if this collection contains at least one element e such that (o == null ? e == null :
 * o.equals(e))." The specification for {@link AnnotationMirrorSet#contains} is "returns true if and
 * only if this collection contains at least one element e such that (o == null ? e == null :
 * AnnotationUtils.areSame(o, e))".
 *
 * <p>AnnotationMirror is an interface and not all implementing classes provide a correct equals
 * method; therefore, the existing implementations of Set cannot be used.
 */
// TODO: Could extend AbstractSet to eliminate the need to implement a few methods.
public class AnnotationMirrorSet
    implements NavigableSet<@KeyFor("this") AnnotationMirror>, DeepCopyable<AnnotationMirrorSet> {

  /** Backing set. */
  // Not final because makeUnmodifiable() can reassign it.
  private NavigableSet<@KeyFor("this") AnnotationMirror> shadowSet =
      new TreeSet<>(AnnotationUtils::compareAnnotationMirrors);

  /** The canonical unmodifiable empty set. */
  private static AnnotationMirrorSet emptySet = unmodifiableSet(Collections.emptySet());

  // Constructors and factory methods

  /** Default constructor. */
  public AnnotationMirrorSet() {}

  // TODO: Should this be an unmodifiable set?
  /**
   * Creates a new {@link AnnotationMirrorSet} that contains {@code value}.
   *
   * @param value the AnnotationMirror to put in the set
   */
  public AnnotationMirrorSet(AnnotationMirror value) {
    this.add(value);
  }

  /**
   * Returns a new {@link AnnotationMirrorSet} that contains the given annotation mirrors.
   *
   * @param annos the AnnotationMirrors to put in the set
   */
  public AnnotationMirrorSet(Collection<? extends AnnotationMirror> annos) {
    this.addAll(annos);
  }

  @SuppressWarnings("keyfor:argument") // transferring keys from one map to another
  @Override
  public AnnotationMirrorSet deepCopy() {
    AnnotationMirrorSet result = new AnnotationMirrorSet();
    result.shadowSet.addAll(shadowSet);
    return result;
  }

  /**
   * Make this set unmodifiable.
   *
   * @return this set
   */
  public @This AnnotationMirrorSet makeUnmodifiable() {
    shadowSet = Collections.unmodifiableNavigableSet(shadowSet);
    return this;
  }

  /**
   * Returns a new unmodifiable {@link AnnotationMirrorSet} that contains {@code value}.
   *
   * @param value the AnnotationMirror to put in the set
   * @return a new unmodifiable {@link AnnotationMirrorSet} that contains only {@code value}
   */
  public static AnnotationMirrorSet singleton(AnnotationMirror value) {
    // The implementation could be more efficient if Collections.singleton returned a
    // NavigableSet.
    AnnotationMirrorSet result = new AnnotationMirrorSet();
    result.add(value);
    result.makeUnmodifiable();
    return result;
  }

  /**
   * Returns an unmodifiable AnnotationMirrorSet with the given elements.
   *
   * @param annos the annotation mirrors that will constitute the new unmodifiable set
   * @return an unmodifiable AnnotationMirrorSet with the given elements
   */
  public static AnnotationMirrorSet unmodifiableSet(Collection<? extends AnnotationMirror> annos) {
    AnnotationMirrorSet result = new AnnotationMirrorSet(annos);
    result.makeUnmodifiable();
    return result;
  }

  /**
   * Returns an empty set.
   *
   * @return an empty set
   */
  public static AnnotationMirrorSet emptySet() {
    return emptySet;
  }

  // Set methods

  @Override
  // @SuppressWarnings("collectionownership:override.receiver")
  public int size() {
    return shadowSet.size();
  }

  @Override
  public boolean isEmpty() {
    return shadowSet.isEmpty();
  }

  @Override
  public boolean contains(
      @UnknownInitialization(AnnotationMirrorSet.class) AnnotationMirrorSet this,
      @Nullable Object o) {
    return o instanceof AnnotationMirror
        && AnnotationUtils.containsSame(shadowSet, (AnnotationMirror) o);
  }

  @Override
  public Iterator<@KeyFor("this") AnnotationMirror> iterator() {
    return shadowSet.iterator();
  }

  @Override
  public Object[] toArray() {
    return shadowSet.toArray();
  }

  @SuppressWarnings("nullness:toarray.nullable.elements.not.newarray") // delegation
  @Override
  public <@KeyForBottom T> @Nullable T[] toArray(@PolyNull T[] a) {
    return shadowSet.toArray(a);
  }

  @SuppressWarnings("keyfor:argument") // delegation
  // @SuppressWarnings("collectionownership:override.receiver")
  @Override
  public boolean add(
      @UnknownInitialization(AnnotationMirrorSet.class) AnnotationMirrorSet this,
      AnnotationMirror annotationMirror) {
    if (contains(annotationMirror)) {
      return false;
    }
    shadowSet.add(annotationMirror);
    return true;
  }

  @Override
  public boolean remove(@Nullable Object o) {
    if (o instanceof AnnotationMirror) {
      AnnotationMirror found = AnnotationUtils.getSame(shadowSet, (AnnotationMirror) o);
      return found != null && shadowSet.remove(found);
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object o : c) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean addAll(
      @UnknownInitialization(AnnotationMirrorSet.class) AnnotationMirrorSet this,
      Collection<? extends AnnotationMirror> c) {
    boolean result = true;
    for (AnnotationMirror a : c) {
      if (!add(a)) {
        result = false;
      }
    }
    return result;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    AnnotationMirrorSet newSet = new AnnotationMirrorSet();
    for (Object o : c) {
      if (contains(o)) {
        assert o != null
            : "@AssumeAssertion(nullness): after contains, the argument should have"
                + " the element type of the set";
        newSet.add((AnnotationMirror) o);
      }
    }
    if (newSet.size() != shadowSet.size()) {
      shadowSet = newSet;
      return true;
    }
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean result = true;
    for (Object a : c) {
      if (!remove(a)) {
        result = false;
      }
    }
    return result;
  }

  @Override
  public void clear() {
    shadowSet.clear();
  }

  @Override
  public String toString() {
    return shadowSet.toString();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AnnotationMirrorSet)) {
      return false;
    }
    AnnotationMirrorSet s = (AnnotationMirrorSet) o;
    if (this.size() != s.size()) {
      return false;
    }
    return containsAll(s);
  }

  @Override
  public int hashCode() {
    int result = 0;
    Iterator<AnnotationMirror> i = iterator();
    while (i.hasNext()) {
      AnnotationMirror am = i.next();
      if (am != null) {
        result += am.hashCode();
      }
    }
    return result;
  }

  // NavigableSet methods

  @SuppressWarnings({
    "interning:override.return", // looks like a bug (in interning checker)
    "signature:override.return", // "
    "nullness:return", // wildcard types
    "keyfor:return" // comparator wildcard
  })
  @Override
  public Comparator<? super AnnotationMirror> comparator() {
    return shadowSet.comparator();
  }

  @Override
  public @KeyFor("this") AnnotationMirror first() {
    return shadowSet.first();
  }

  @Override
  public @KeyFor("this") AnnotationMirror last() {
    return shadowSet.last();
  }

  @SuppressWarnings("keyfor:argument") // delegation
  @Override
  public @Nullable @KeyFor("this") AnnotationMirror lower(AnnotationMirror e) {
    return shadowSet.lower(e);
  }

  @SuppressWarnings("keyfor:argument") // delegation
  @Override
  public @Nullable @KeyFor("this") AnnotationMirror floor(AnnotationMirror e) {
    return shadowSet.floor(e);
  }

  @SuppressWarnings("keyfor:argument") // delegation
  @Override
  public @Nullable @KeyFor("this") AnnotationMirror ceiling(AnnotationMirror e) {
    return shadowSet.ceiling(e);
  }

  @SuppressWarnings("keyfor:argument") // delegation
  @Override
  public @Nullable @KeyFor("this") AnnotationMirror higher(AnnotationMirror e) {
    return shadowSet.higher(e);
  }

  @Override
  public @Nullable @KeyFor("this") AnnotationMirror pollFirst() {
    return shadowSet.pollFirst();
  }

  @Override
  public @Nullable @KeyFor("this") AnnotationMirror pollLast() {
    return shadowSet.pollLast();
  }

  @Override
  public AnnotationMirrorSet descendingSet() {
    throw new Error("Not yet implemented.");
  }

  @Override
  public Iterator<@KeyFor("this") AnnotationMirror> descendingIterator() {
    throw new Error("Not yet implemented.");
  }

  @Override
  public AnnotationMirrorSet subSet(
      AnnotationMirror fromElement,
      boolean fromInclusive,
      AnnotationMirror toElement,
      boolean toInclusive) {
    throw new Error("Not yet implemented.");
  }

  @Override
  public AnnotationMirrorSet headSet(AnnotationMirror toElement, boolean inclusive) {
    throw new Error("Not yet implemented.");
  }

  @Override
  public AnnotationMirrorSet tailSet(AnnotationMirror fromElement, boolean inclusive) {
    throw new Error("Not yet implemented.");
  }

  @Override
  public AnnotationMirrorSet subSet(AnnotationMirror fromElement, AnnotationMirror toElement) {
    throw new Error("Not yet implemented.");
  }

  @Override
  public AnnotationMirrorSet headSet(AnnotationMirror toElement) {
    throw new Error("Not yet implemented.");
  }

  @Override
  public AnnotationMirrorSet tailSet(AnnotationMirror fromElement) {
    throw new Error("Not yet implemented.");
  }
}
