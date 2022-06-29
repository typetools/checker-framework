package org.checkerframework.framework.type;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.plumelib.util.StringsPlume;

/**
 * Represents multiple type qualifier hierarchies. {@link #getWidth} gives the number of hierarchies
 * that this object represents. Each hierarchy has its own top and bottom, and subtyping
 * relationships exist only within each hierarchy.
 *
 * <p>Note the distinction in terminology between a qualifier hierarchy, which has one top and one
 * bottom, and a {@code QualifierHierarchy}, which represents multiple qualifier hierarchies.
 *
 * <p>All type annotations need to be type qualifiers recognized within this hierarchy.
 *
 * <p>This assumes that every annotated type in a program is annotated with exactly one qualifier
 * from each hierarchy.
 */
@AnnotatedFor("nullness")
public interface QualifierHierarchy {

  /**
   * Determine whether this is valid.
   *
   * @return whether this is valid
   */
  default boolean isValid() {
    return true;
  }

  // **********************************************************************
  // Getter methods about this hierarchy
  // **********************************************************************

  /**
   * Returns the width of this hierarchy, i.e. the expected number of annotations on any valid type.
   *
   * @return the width of this QualifierHierarchy
   */
  default int getWidth() {
    return getTopAnnotations().size();
  }

  /**
   * Returns the top (ultimate super) type qualifiers in the type system. The size of this set is
   * equal to {@link #getWidth}.
   *
   * @return the top (ultimate super) type qualifiers in the type system
   */
  Set<? extends AnnotationMirror> getTopAnnotations();

  /**
   * Return the top qualifier for the given qualifier, that is, the qualifier that is a supertype of
   * {@code qualifier} but no further supertypes exist.
   *
   * @param qualifier any qualifier from one of the qualifier hierarchies represented by this
   * @return the top qualifier of {@code qualifier}'s hierarchy
   */
  AnnotationMirror getTopAnnotation(AnnotationMirror qualifier);

  /**
   * Returns the bottom type qualifiers in the hierarchy. The size of this set is equal to {@link
   * #getWidth}.
   *
   * @return the bottom type qualifiers in the hierarchy
   */
  Set<? extends AnnotationMirror> getBottomAnnotations();

  /**
   * Return the bottom for the given qualifier, that is, the qualifier that is a subtype of {@code
   * qualifier} but no further subtypes exist.
   *
   * @param qualifier any qualifier from one of the qualifier hierarchies represented by this
   * @return the bottom qualifier of {@code qualifier}'s hierarchy
   */
  AnnotationMirror getBottomAnnotation(AnnotationMirror qualifier);

  /**
   * Returns the polymorphic qualifier for the hierarchy containing {@code qualifier}, or {@code
   * null} if there is no polymorphic qualifier in that hierarchy.
   *
   * @param qualifier any qualifier from one of the qualifier hierarchies represented by this
   * @return the polymorphic qualifier for the hierarchy containing {@code qualifier}, or {@code
   *     null} if there is no polymorphic qualifier in that hierarchy
   */
  @Nullable AnnotationMirror getPolymorphicAnnotation(AnnotationMirror qualifier);

  /**
   * Returns {@code true} if the qualifier is a polymorphic qualifier; otherwise, returns {@code
   * false}.
   *
   * @param qualifier qualifier
   * @return {@code true} if the qualifier is a polymorphic qualifier; otherwise, returns {@code
   *     false}.
   */
  boolean isPolymorphicQualifier(AnnotationMirror qualifier);

  // **********************************************************************
  // Qualifier Hierarchy Queries
  // **********************************************************************

  /**
   * Tests whether {@code subQualifier} is equal to or a sub-qualifier of {@code superQualifier},
   * according to the type qualifier hierarchy.
   *
   * @param subQualifier possible subqualifier of {@code superQualifier}
   * @param superQualifier possible superqualifier of {@code subQualifier}
   * @return true iff {@code subQualifier} is a subqualifier of, or equal to, {@code superQualifier}
   */
  boolean isSubtype(AnnotationMirror subQualifier, AnnotationMirror superQualifier);

  /**
   * Tests whether all qualifiers in {@code subQualifiers} are a subqualifier or equal to the
   * qualifier in the same hierarchy in {@code superQualifiers}.
   *
   * @param subQualifiers set of qualifiers; exactly one per hierarchy
   * @param superQualifiers set of qualifiers; exactly one per hierarchy
   * @return true iff all qualifiers in {@code subQualifiers} are a subqualifier or equal to the
   *     qualifier in the same hierarchy in {@code superQualifiers}
   */
  default boolean isSubtype(
      Collection<? extends AnnotationMirror> subQualifiers,
      Collection<? extends AnnotationMirror> superQualifiers) {
    assertSameSize(subQualifiers, superQualifiers);
    for (AnnotationMirror subQual : subQualifiers) {
      AnnotationMirror superQual = findAnnotationInSameHierarchy(superQualifiers, subQual);
      if (superQual == null) {
        throw new BugInCF(
            "QualifierHierarchy: missing annotation in hierarchy %s. found: %s",
            subQual, StringsPlume.join(",", superQualifiers));
      }
      if (!isSubtype(subQual, superQual)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the least upper bound (LUB) of the qualifiers {@code qualifier1} and {@code
   * qualifier2}. Returns {@code null} if the qualifiers are not from the same qualifier hierarchy.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>For NonNull, leastUpperBound('Nullable', 'NonNull') &rArr; Nullable
   * </ul>
   *
   * @param qualifier1 the first qualifier; may not be in the same hierarchy as {@code qualifier2}
   * @param qualifier2 the second qualifier; may not be in the same hierarchy as {@code qualifier1}
   * @return the least upper bound of the qualifiers, or {@code null} if the qualifiers are from
   *     different hierarchies
   */
  // The fact that null is returned if the qualifiers are not in the same hierarchy is used by the
  // collection version of LUB below.
  @Nullable AnnotationMirror leastUpperBound(
      AnnotationMirror qualifier1, AnnotationMirror qualifier2);

  /**
   * Returns the least upper bound of the two sets of qualifiers. The result is the lub of the
   * qualifier for the same hierarchy in each set.
   *
   * @param qualifiers1 set of qualifiers; exactly one per hierarchy
   * @param qualifiers2 set of qualifiers; exactly one per hierarchy
   * @return the least upper bound of the two sets of qualifiers
   */
  default Set<? extends AnnotationMirror> leastUpperBounds(
      Collection<? extends AnnotationMirror> qualifiers1,
      Collection<? extends AnnotationMirror> qualifiers2) {
    assertSameSize(qualifiers1, qualifiers2);
    if (qualifiers1.isEmpty()) {
      throw new BugInCF(
          "QualifierHierarchy.leastUpperBounds: tried to determine LUB with empty sets");
    }

    Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
    for (AnnotationMirror a1 : qualifiers1) {
      for (AnnotationMirror a2 : qualifiers2) {
        AnnotationMirror lub = leastUpperBound(a1, a2);
        if (lub != null) {
          result.add(lub);
        }
      }
    }

    assertSameSize(result, qualifiers1);
    return result;
  }

  /**
   * Returns the number of iterations dataflow should perform before {@link
   * #widenedUpperBound(AnnotationMirror, AnnotationMirror)} is called or -1 if it should never be
   * called.
   *
   * @return the number of iterations dataflow should perform before {@link
   *     #widenedUpperBound(AnnotationMirror, AnnotationMirror)} is called or -1 if it should never
   *     be called.
   */
  default int numberOfIterationsBeforeWidening() {
    return -1;
  }

  /**
   * If the qualifier hierarchy has an infinite ascending chain, then the dataflow analysis might
   * never reach a fixed point. To prevent this, implement this method such that it returns an upper
   * bound for the two qualifiers that is a strict super type of the least upper bound. If this
   * method is implemented, also override {@link #numberOfIterationsBeforeWidening()} to return a
   * positive number.
   *
   * <p>{@code newQualifier} is newest qualifier dataflow computed for some expression and {@code
   * previousQualifier} is the qualifier dataflow computed on the last iteration.
   *
   * <p>If the qualifier hierarchy has no infinite ascending chain, returns the least upper bound of
   * the two annotations.
   *
   * @param newQualifier new qualifier dataflow computed for some expression; must be in the same
   *     hierarchy as {@code previousQualifier}
   * @param previousQualifier the previous qualifier dataflow computed on the last iteration; must
   *     be in the same hierarchy as {@code previousQualifier}
   * @return an upper bound that is higher than the least upper bound of newQualifier and
   *     previousQualifier (or the lub if the qualifier hierarchy does not require this)
   */
  default AnnotationMirror widenedUpperBound(
      AnnotationMirror newQualifier, AnnotationMirror previousQualifier) {
    AnnotationMirror widenUpperBound = leastUpperBound(newQualifier, previousQualifier);
    if (widenUpperBound == null) {
      throw new BugInCF(
          "Passed two unrelated qualifiers to QualifierHierarchy#widenedUpperBound. %s %s.",
          newQualifier, previousQualifier);
    }
    return widenUpperBound;
  }

  /**
   * Returns the greatest lower bound for the qualifiers qualifier1 and qualifier2. Returns null if
   * the qualifiers are not from the same qualifier hierarchy.
   *
   * @param qualifier1 first qualifier
   * @param qualifier2 second qualifier
   * @return greatest lower bound of the two annotations or null if the two annotations are not from
   *     the same hierarchy
   */
  // The fact that null is returned if the qualifiers are not in the same hierarchy is used by the
  // collection version of LUB below.
  @Nullable AnnotationMirror greatestLowerBound(
      AnnotationMirror qualifier1, AnnotationMirror qualifier2);

  /**
   * Returns the greatest lower bound of the two sets of qualifiers. The result is the lub of the
   * qualifier for the same hierarchy in each set.
   *
   * @param qualifiers1 set of qualifiers; exactly one per hierarchy
   * @param qualifiers2 set of qualifiers; exactly one per hierarchy
   * @return the greatest lower bound of the two sets of qualifiers
   */
  default Set<? extends AnnotationMirror> greatestLowerBounds(
      Collection<? extends AnnotationMirror> qualifiers1,
      Collection<? extends AnnotationMirror> qualifiers2) {
    assertSameSize(qualifiers1, qualifiers2);
    if (qualifiers1.isEmpty()) {
      throw new BugInCF(
          "QualifierHierarchy.greatestLowerBounds: tried to determine GLB with empty sets");
    }

    Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
    for (AnnotationMirror a1 : qualifiers1) {
      for (AnnotationMirror a2 : qualifiers2) {
        AnnotationMirror glb = greatestLowerBound(a1, a2);
        if (glb != null) {
          result.add(glb);
        }
      }
    }

    assertSameSize(qualifiers1, qualifiers2, result);
    return result;
  }

  /**
   * Returns true if and only if {@link AnnotatedTypeMirror#getAnnotations()} can return a set with
   * fewer qualifiers than the width of the QualifierHierarchy.
   *
   * @param type the type to test
   * @return true if and only if {@link AnnotatedTypeMirror#getAnnotations()} can return a set with
   *     fewer qualifiers than the width of the QualifierHierarchy
   */
  static boolean canHaveEmptyAnnotationSet(AnnotatedTypeMirror type) {
    return type.getKind() == TypeKind.TYPEVAR
        || type.getKind() == TypeKind.WILDCARD
        ||
        // TODO: or should the union/intersection be the LUB of the alternatives?
        type.getKind() == TypeKind.UNION
        || type.getKind() == TypeKind.INTERSECTION;
  }

  /**
   * Returns the annotation in {@code qualifiers} that is in the same hierarchy as {@code
   * qualifier}.
   *
   * <p>The default implementation calls {@link #getTopAnnotation(AnnotationMirror)} and then calls
   * {@link #findAnnotationInHierarchy(Collection, AnnotationMirror)}. So, if {@code qualifier} is a
   * top qualifier, then call {@link #findAnnotationInHierarchy(Collection, AnnotationMirror)}
   * directly is faster.
   *
   * @param qualifiers set of annotations to search
   * @param qualifier annotation that is in the same hierarchy as the returned annotation
   * @return annotation in the same hierarchy as qualifier, or null if one is not found
   */
  default @Nullable AnnotationMirror findAnnotationInSameHierarchy(
      Collection<? extends AnnotationMirror> qualifiers, AnnotationMirror qualifier) {
    AnnotationMirror top = this.getTopAnnotation(qualifier);
    return findAnnotationInHierarchy(qualifiers, top);
  }

  /**
   * Returns the annotation in {@code qualifiers} that is in the hierarchy for which {@code top} is
   * top.
   *
   * @param qualifiers set of annotations to search
   * @param top the top annotation in the hierarchy to which the returned annotation belongs
   * @return annotation in the same hierarchy as annotationMirror, or null if one is not found
   */
  default @Nullable AnnotationMirror findAnnotationInHierarchy(
      Collection<? extends AnnotationMirror> qualifiers, AnnotationMirror top) {
    for (AnnotationMirror anno : qualifiers) {
      if (isSubtype(anno, top)) {
        return anno;
      }
    }
    return null;
  }

  /**
   * Update a mapping from {@code key} to a set of AnnotationMirrors. If {@code key} is not already
   * in the map, then put it in the map with a value of a new set containing {@code qualifier}. If
   * the map contains {@code key}, then add {@code qualifier} to the set to which {@code key} maps.
   * If that set contains a qualifier in the same hierarchy as {@code qualifier}, then don't add it
   * and return false.
   *
   * @param map the mapping to modify
   * @param key the key to update or add
   * @param qualifier the value to update or add
   * @param <T> type of the map's keys
   * @return true if the update was done; false if there was a qualifier hierarchy collision
   */
  default <T> boolean updateMappingToMutableSet(
      Map<T, Set<AnnotationMirror>> map, T key, AnnotationMirror qualifier) {
    // https://github.com/typetools/checker-framework/issues/2000
    @SuppressWarnings("nullness:argument")
    boolean mapContainsKey = map.containsKey(key);
    if (mapContainsKey) {
      @SuppressWarnings("nullness:assignment") // key is a key for map.
      @NonNull Set<AnnotationMirror> prevs = map.get(key);
      AnnotationMirror old = findAnnotationInSameHierarchy(prevs, qualifier);
      if (old != null) {
        return false;
      }
      prevs.add(qualifier);
      map.put(key, prevs);
    } else {
      Set<AnnotationMirror> set = AnnotationUtils.createAnnotationSet();
      set.add(qualifier);
      map.put(key, set);
    }
    return true;
  }

  /**
   * Throws an exception if the given collections do not have the same size.
   *
   * @param c1 the first collection
   * @param c2 the second collection
   */
  static void assertSameSize(Collection<?> c1, Collection<?> c2) {
    if (c1.size() != c2.size()) {
      throw new BugInCF(
          "inconsistent sizes (%d, %d):%n  [%s]%n  [%s]",
          c1.size(), c2.size(), StringsPlume.join(",", c1), StringsPlume.join(",", c2));
    }
  }

  /**
   * Throws an exception if the result does not have the same size as the inputs (which are assumed
   * to have the same size as one another).
   *
   * @param c1 the first collection
   * @param c2 the second collection
   * @param result the result collection
   */
  static void assertSameSize(Collection<?> c1, Collection<?> c2, Collection<?> result) {
    if (c1.size() != result.size() || c2.size() != result.size()) {
      throw new BugInCF(
          "inconsistent sizes (%d, %d, %d):%n  %s%n  %s%n  %s",
          c1.size(),
          c2.size(),
          result.size(),
          StringsPlume.join(",", c1),
          StringsPlume.join(",", c2),
          StringsPlume.join(",", result));
    }
  }
}
