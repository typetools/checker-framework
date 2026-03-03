package org.checkerframework.framework.type;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.javacutil.AnnotationMirrorSet;
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
public abstract class QualifierHierarchy {

  /** The associated type factory. This is used only for checking whether types are relevant. */
  protected GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory;

  /**
   * Creates a new QualifierHierarchy.
   *
   * @param atypeFactory the associated type factory
   */
  public QualifierHierarchy(GenericAnnotatedTypeFactory<?, ?, ?, ?> atypeFactory) {
    this.atypeFactory = atypeFactory;
  }

  /**
   * Returns true if this QualifierHierarchy is valid.
   *
   * @return true if this QualifierHierarchy is valid
   */
  public boolean isValid() {
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
  public int getWidth() {
    return getTopAnnotations().size();
  }

  /**
   * Returns the top (ultimate super) type qualifiers in the type system. The size of this set is
   * equal to {@link #getWidth}.
   *
   * @return the top (ultimate super) type qualifiers in the type system
   */
  public abstract AnnotationMirrorSet getTopAnnotations();

  /**
   * Returns true if the given qualifer is one of the top annotations for this qualifer hierarchy.
   *
   * @param qualifier any qualifier from one of the qualifier hierarchies represented by this
   * @return true if the given qualifer is one of the top annotations for this qualifer hierarchy
   */
  public boolean isTop(AnnotationMirror qualifier) {
    return AnnotationUtils.containsSame(getTopAnnotations(), qualifier);
  }

  /**
   * Returns the top qualifier for the given qualifier, that is, the qualifier that is a supertype
   * of {@code qualifier} but no further supertypes exist.
   *
   * @param qualifier any qualifier from one of the qualifier hierarchies represented by this
   * @return the top qualifier of {@code qualifier}'s hierarchy
   */
  public abstract AnnotationMirror getTopAnnotation(AnnotationMirror qualifier);

  /**
   * Returns the bottom type qualifiers in the hierarchy. The size of this set is equal to {@link
   * #getWidth}.
   *
   * @return the bottom type qualifiers in the hierarchy
   */
  public abstract AnnotationMirrorSet getBottomAnnotations();

  /**
   * Returns the bottom for the given qualifier, that is, the qualifier that is a subtype of {@code
   * qualifier} but no further subtypes exist.
   *
   * @param qualifier any qualifier from one of the qualifier hierarchies represented by this
   * @return the bottom qualifier of {@code qualifier}'s hierarchy
   */
  public abstract AnnotationMirror getBottomAnnotation(AnnotationMirror qualifier);

  /**
   * Returns the polymorphic qualifier for the hierarchy containing {@code qualifier}, or {@code
   * null} if there is no polymorphic qualifier in that hierarchy.
   *
   * @param qualifier any qualifier from one of the qualifier hierarchies represented by this
   * @return the polymorphic qualifier for the hierarchy containing {@code qualifier}, or {@code
   *     null} if there is no polymorphic qualifier in that hierarchy
   */
  public abstract @Nullable AnnotationMirror getPolymorphicAnnotation(AnnotationMirror qualifier);

  /**
   * Returns {@code true} if the qualifier is a polymorphic qualifier; otherwise, returns {@code
   * false}.
   *
   * @param qualifier qualifier
   * @return {@code true} if the qualifier is a polymorphic qualifier; otherwise, returns {@code
   *     false}.
   */
  public abstract boolean isPolymorphicQualifier(AnnotationMirror qualifier);

  // **********************************************************************
  // Qualifier Hierarchy Queries
  // **********************************************************************

  /**
   * Returns true if {@code subQualifier} is equal to or a sub-qualifier of {@code superQualifier},
   * according to the type qualifier hierarchy, ignoring Java basetypes.
   *
   * <p>Clients should generally call {@link #isSubtypeShallow}. However, subtypes should generally
   * override this method (if needed).
   *
   * <p>This method behaves the same as {@link #isSubtypeQualifiersOnly(AnnotationMirror,
   * AnnotationMirror)}, which calls this method. This method is for clients inside the framework,
   * and it has {@code protected} access to prevent use by clients outside the framework. This makes
   * it easy to find places where code outside the framework is ignoring Java basetypes -- at calls
   * to {@link #isSubtypeQualifiersOnly}.
   *
   * @param subQualifier possible subqualifier
   * @param superQualifier possible superqualifier
   * @return true iff {@code subQualifier} is a subqualifier of, or equal to, {@code superQualifier}
   */
  protected abstract boolean isSubtypeQualifiers(
      AnnotationMirror subQualifier, AnnotationMirror superQualifier);

  /**
   * Returns true if {@code subQualifier} is equal to or a sub-qualifier of {@code superQualifier},
   * according to the type qualifier hierarchy, ignoring Java basetypes.
   *
   * <p>This method is for clients outside the framework, and should not be used by framework code.
   *
   * @param subQualifier possible subqualifier
   * @param superQualifier possible superqualifier
   * @return true iff {@code subQualifier} is a subqualifier of, or equal to, {@code superQualifier}
   */
  public final boolean isSubtypeQualifiersOnly(
      AnnotationMirror subQualifier, AnnotationMirror superQualifier) {
    return isSubtypeQualifiers(subQualifier, superQualifier);
  }

  /**
   * Returns true if {@code subQualifier} is equal to or a sub-qualifier of {@code superQualifier},
   * according to the type qualifier hierarchy. The types {@code subType} and {@code superType} are
   * not necessarily in a Java subtyping relationship with one another and are only used by this
   * method for special cases when qualifier subtyping depends on the Java basetype.
   *
   * <p>Clients should usually call {@code isSubtypeShallow()} (this method). Rarely, to ignore the
   * Java basetype, a client can call {@link #isSubtypeQualifiersOnly}.
   *
   * <p>Subtypes should override {@link #isSubtypeQualifiers} (not this method), unless qualifier
   * subtyping depends on Java basetypes.
   *
   * @param subQualifier possible subqualifier
   * @param subType the Java basetype associated with {@code subQualifier}
   * @param superQualifier possible superqualifier
   * @param superType the Java basetype associated with {@code superQualifier}
   * @return true iff {@code subQualifier} is a subqualifier of, or equal to, {@code superQualifier}
   */
  @SuppressWarnings({"nullness", "keyfor"}) // AnnotatedTypeFactory hasn't been annotated.
  public boolean isSubtypeShallow(
      AnnotationMirror subQualifier,
      TypeMirror subType,
      AnnotationMirror superQualifier,
      TypeMirror superType) {
    if (!atypeFactory.isRelevant(subType) || !atypeFactory.isRelevant(superType)) {
      // At least one of the types is not relevant.
      return true;
    }
    return isSubtypeQualifiers(subQualifier, superQualifier);
  }

  /**
   * Returns true if {@code subQualifier} is equal to or a sub-qualifier of {@code superQualifier},
   * according to the type qualifier hierarchy. The type {@code typeMirror} is only used by this
   * method for special cases when qualifier subtyping depends on the Java basetype.
   *
   * <p>Clients should usually call {@link #isSubtypeShallow(AnnotationMirror, AnnotationMirror,
   * TypeMirror)} (this method) or {@link #isSubtypeShallow(AnnotationMirror, TypeMirror,
   * AnnotationMirror, TypeMirror)}. Rarely, to ignore the Java basetype, a client can call {@link
   * #isSubtypeQualifiersOnly}.
   *
   * <p>Subtypes should override {@link #isSubtypeQualifiers} (not this method), unless qualifier
   * subtyping depends on Java basetypes.
   *
   * @param subQualifier possible subqualifier
   * @param superQualifier possible superqualifier
   * @param typeMirror the Java basetype associated with both {@code subQualifier} and {@code
   *     superQualifier}
   * @return true iff {@code subQualifier} is a subqualifier of, or equal to, {@code superQualifier}
   */
  public final boolean isSubtypeShallow(
      AnnotationMirror subQualifier, AnnotationMirror superQualifier, TypeMirror typeMirror) {
    return isSubtypeShallow(subQualifier, typeMirror, superQualifier, typeMirror);
  }

  /**
   * Returns true if all qualifiers in {@code subQualifiers} are a subqualifier or equal to the
   * qualifier in the same hierarchy in {@code superQualifiers}. The types {@code subType} and
   * {@code superType} are not necessarily in a Java subtyping relationship with one another and are
   * only used by this method for special cases when qualifier subtyping depends on the Java
   * basetype.
   *
   * <p>Subtypes of {@code QualifierHierarchy} more often override {@link
   * #isSubtypeShallow(AnnotationMirror, TypeMirror, AnnotationMirror, TypeMirror)} than this
   * method.
   *
   * @param subQualifiers a set of qualifiers; exactly one per hierarchy
   * @param subType the type associated with {@code subQualifiers}
   * @param superQualifiers a set of qualifiers; exactly one per hierarchy
   * @param superType the type associated with {@code superQualifiers}
   * @return true iff all qualifiers in {@code subQualifiers} are a subqualifier or equal to the
   *     qualifier in the same hierarchy in {@code superQualifiers}
   */
  public final boolean isSubtypeShallow(
      Collection<? extends AnnotationMirror> subQualifiers,
      TypeMirror subType,
      Collection<? extends AnnotationMirror> superQualifiers,
      TypeMirror superType) {
    assertSameSize(subQualifiers, superQualifiers);
    for (AnnotationMirror subQual : subQualifiers) {
      AnnotationMirror superQual = findAnnotationInSameHierarchy(superQualifiers, subQual);
      if (superQual == null) {
        throw new BugInCF(
            "QualifierHierarchy: missing annotation in hierarchy %s. found: %s",
            subQual, StringsPlume.join(",", superQualifiers));
      }
      if (!isSubtypeShallow(subQual, subType, superQual, superType)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if all qualifiers in {@code subQualifiers} are a subqualifier or equal to the
   * qualifier in the same hierarchy in {@code superQualifiers}. The types {@code subType} and
   * {@code superType} are not necessarily in a Java subtyping relationship with one another and are
   * only used by this method for special cases when qualifier subtyping depends on the Java
   * basetype.
   *
   * <p>Subtypes of {@code QualifierHierarchy} more often override {@link
   * #isSubtypeShallow(AnnotationMirror, TypeMirror, AnnotationMirror, TypeMirror)} than this
   * method.
   *
   * @param subQualifiers a set of qualifiers; exactly one per hierarchy
   * @param superQualifiers a set of qualifiers; exactly one per hierarchy
   * @return true iff all qualifiers in {@code subQualifiers} are a subqualifier or equal to the
   *     qualifier in the same hierarchy in {@code superQualifiers}
   */
  public boolean isSubtypeQualifiersOnly(
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
      if (!isSubtypeQualifiersOnly(subQual, superQual)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if all qualifiers in {@code subQualifiers} are a subqualifier of or equal to the
   * qualifier in the same hierarchy in {@code superQualifiers}. The type {@code typeMirror} is only
   * used by this method for special cases when qualifier subtyping depends on the Java basetype.
   *
   * <p>Subtypes of {@code QualifierHierarchy} more often override {@link
   * #isSubtypeShallow(AnnotationMirror, TypeMirror, AnnotationMirror, TypeMirror)} than this
   * method.
   *
   * @param subQualifiers a set of qualifiers; exactly one per hierarchy
   * @param superQualifiers a set of qualifiers; exactly one per hierarchy
   * @param typeMirror the type associated with both sets of qualifiers
   * @return true iff all qualifiers in {@code subQualifiers} are a subqualifier or equal to the
   *     qualifier in the same hierarchy in {@code superQualifiers}
   */
  public final boolean isSubtypeShallow(
      Collection<? extends AnnotationMirror> subQualifiers,
      Collection<? extends AnnotationMirror> superQualifiers,
      TypeMirror typeMirror) {
    return isSubtypeShallow(subQualifiers, typeMirror, superQualifiers, typeMirror);
  }

  /**
   * Returns the least upper bound (LUB) of the qualifiers {@code qualifier1} and {@code
   * qualifier2}. Returns {@code null} if the qualifiers are not from the same qualifier hierarchy.
   * Ignores Java basetypes.
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
  protected abstract @Nullable AnnotationMirror leastUpperBoundQualifiers(
      AnnotationMirror qualifier1, AnnotationMirror qualifier2);

  /**
   * Returns the least upper bound of all the collections of qualifiers. The result is the lub of
   * the qualifier for the same hierarchy in each set.
   *
   * @param qualifiers a collection of collections of qualifiers. Each inner collection has exactly
   *     one qualifier per hierarchy.
   * @return the least upper bound of the collections of qualifiers
   */
  public Set<? extends AnnotationMirror> leastUpperBoundsQualifiersOnly(
      Collection<? extends Collection<? extends AnnotationMirror>> qualifiers) {
    if (qualifiers.isEmpty()) {
      return AnnotationMirrorSet.emptySet();
    }
    Iterator<? extends Collection<? extends AnnotationMirror>> itor = qualifiers.iterator();
    Set<? extends AnnotationMirror> result = new AnnotationMirrorSet(itor.next());
    while (itor.hasNext()) {
      Collection<? extends AnnotationMirror> annos = itor.next();
      result = leastUpperBoundsQualifiersOnly(result, annos);
    }
    return result;
  }

  /**
   * Returns the least upper bound (LUB) of the qualifiers {@code qualifier1} and {@code
   * qualifier2}. Returns {@code null} if the qualifiers are not from the same qualifier hierarchy.
   * Ignores Java basetypes.
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
  public final @Nullable AnnotationMirror leastUpperBoundQualifiersOnly(
      AnnotationMirror qualifier1, AnnotationMirror qualifier2) {
    return leastUpperBoundQualifiers(qualifier1, qualifier2);
  }

  /**
   * Returns the least upper bound of the two sets of qualifiers. The result is the lub of the
   * qualifier for the same hierarchy in each set.
   *
   * @param qualifiers1 a set of qualifiers; exactly one per hierarchy
   * @param qualifiers2 a set of qualifiers; exactly one per hierarchy
   * @return the least upper bound of the two sets of qualifiers
   */
  public Set<? extends AnnotationMirror> leastUpperBoundsQualifiersOnly(
      Collection<? extends AnnotationMirror> qualifiers1,
      Collection<? extends AnnotationMirror> qualifiers2) {
    assertSameSize(qualifiers1, qualifiers2);
    if (qualifiers1.isEmpty()) {
      throw new BugInCF(
          "QualifierHierarchy.leastUpperBounds: tried to determine LUB with empty sets");
    }

    AnnotationMirrorSet result = new AnnotationMirrorSet();
    for (AnnotationMirror a1 : qualifiers1) {
      for (AnnotationMirror a2 : qualifiers2) {
        AnnotationMirror lub = leastUpperBoundQualifiersOnly(a1, a2);
        if (lub != null) {
          result.add(lub);
        }
      }
    }

    assertSameSize(result, qualifiers1);
    return result;
  }

  /**
   * Returns the least upper bound (LUB) of the qualifiers {@code qualifier1} and {@code
   * qualifier2}. Returns {@code null} if the qualifiers are not from the same qualifier hierarchy.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>leastUpperBound('Nullable', 'NonNull') &rArr; Nullable
   * </ul>
   *
   * @param qualifier1 the first qualifier; may not be in the same hierarchy as {@code qualifier2}
   * @param tm1 the type on which qualifier1 appears
   * @param qualifier2 the second qualifier; may not be in the same hierarchy as {@code qualifier1}
   * @param tm2 the type on which qualifier2 appears
   * @return the least upper bound of the qualifiers, or {@code null} if the qualifiers are from
   *     different hierarchies
   */
  // The fact that null is returned if the qualifiers are not in the same hierarchy is used by the
  // collection version of LUB below.
  @SuppressWarnings({"nullness", "keyfor"}) // AnnotatedTypeFactory hasn't been annotated.
  public @Nullable AnnotationMirror leastUpperBoundShallow(
      AnnotationMirror qualifier1, TypeMirror tm1, AnnotationMirror qualifier2, TypeMirror tm2) {
    boolean tm1IsRelevant = atypeFactory.isRelevant(tm1);
    boolean tm2IsRelevant = atypeFactory.isRelevant(tm2);
    if (tm1IsRelevant == tm2IsRelevant) {
      return leastUpperBoundQualifiers(qualifier1, qualifier2);
    } else if (tm1IsRelevant) {
      return qualifier1;
    } else { // if (tm2IsRelevant) {
      return qualifier2;
    }
  }

  /**
   * Returns the least upper bound of the two sets of qualifiers. The result is the lub of the
   * qualifier for the same hierarchy in each set.
   *
   * @param qualifiers1 a set of qualifiers; exactly one per hierarchy
   * @param tm1 the type on which qualifiers1 appear
   * @param qualifiers2 a set of qualifiers; exactly one per hierarchy
   * @param tm2 the type on which qualifiers2 appear
   * @return the least upper bound of the two sets of qualifiers
   */
  public final Set<? extends AnnotationMirror> leastUpperBoundsShallow(
      Collection<? extends AnnotationMirror> qualifiers1,
      TypeMirror tm1,
      Collection<? extends AnnotationMirror> qualifiers2,
      TypeMirror tm2) {
    assertSameSize(qualifiers1, qualifiers2);
    if (qualifiers1.isEmpty()) {
      throw new BugInCF(
          "QualifierHierarchy.leastUpperBounds: tried to determine LUB with empty sets");
    }

    AnnotationMirrorSet result = new AnnotationMirrorSet();
    for (AnnotationMirror a1 : qualifiers1) {
      for (AnnotationMirror a2 : qualifiers2) {
        AnnotationMirror lub = leastUpperBoundShallow(a1, tm1, a2, tm2);
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
  public int numberOfIterationsBeforeWidening() {
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
  public AnnotationMirror widenedUpperBound(
      AnnotationMirror newQualifier, AnnotationMirror previousQualifier) {
    AnnotationMirror widenedUpperBound = leastUpperBoundQualifiers(newQualifier, previousQualifier);
    if (widenedUpperBound == null) {
      throw new BugInCF(
          "widenedUpperBound(%s, %s): unrelated qualifiers", newQualifier, previousQualifier);
    }
    return widenedUpperBound;
  }

  /**
   * Returns the greatest lower bound for the qualifiers qualifier1 and qualifier2. Returns null if
   * the qualifiers are not from the same qualifier hierarchy.
   *
   * @param qualifier1 first qualifier
   * @param qualifier2 second qualifier
   * @return greatest lower bound of the two annotations, or null if the two annotations are not
   *     from the same hierarchy
   */
  // The fact that null is returned if the qualifiers are not in the same hierarchy is used by the
  // collection version of LUB below.
  public abstract @Nullable AnnotationMirror greatestLowerBoundQualifiers(
      AnnotationMirror qualifier1, AnnotationMirror qualifier2);

  /**
   * Returns the greatest lower bound for the qualifiers qualifier1 and qualifier2. Returns null if
   * the qualifiers are not from the same qualifier hierarchy.
   *
   * @param qualifier1 first qualifier
   * @param qualifier2 second qualifier
   * @return greatest lower bound of the two annotations, or null if the two annotations are not
   *     from the same hierarchy
   */
  // The fact that null is returned if the qualifiers are not in the same hierarchy is used by the
  // collection version of LUB below.
  public final @Nullable AnnotationMirror greatestLowerBoundQualifiersOnly(
      AnnotationMirror qualifier1, AnnotationMirror qualifier2) {
    return greatestLowerBoundQualifiers(qualifier1, qualifier2);
  }

  /**
   * Returns the greatest lower bound for the qualifiers qualifier1 and qualifier2. Returns null if
   * the qualifiers are not from the same qualifier hierarchy.
   *
   * @param qualifier1 first qualifier
   * @param tm1 the type that is annotated by qualifier1
   * @param qualifier2 second qualifier
   * @param tm2 the type that is annotated by qualifier2
   * @return greatest lower bound of the two annotations, or null if the two annotations are not
   *     from the same hierarchy
   */
  @SuppressWarnings({"nullness", "keyfor"}) // AnnotatedTypeFactory hasn't been annotated.
  public @Nullable AnnotationMirror greatestLowerBoundShallow(
      AnnotationMirror qualifier1, TypeMirror tm1, AnnotationMirror qualifier2, TypeMirror tm2) {
    boolean tm1IsRelevant = atypeFactory.isRelevant(tm1);
    boolean tm2IsRelevant = atypeFactory.isRelevant(tm2);
    if (tm1IsRelevant == tm2IsRelevant) {
      return greatestLowerBoundQualifiers(qualifier1, qualifier2);
    } else if (tm1IsRelevant) {
      return qualifier1;
    } else { // if (tm2IsRelevant) {
      return qualifier2;
    }
  }

  /**
   * Returns the greatest lower bound of the two sets of qualifiers. The result is the lub of the
   * qualifier for the same hierarchy in each set.
   *
   * @param qualifiers1 a set of qualifiers; exactly one per hierarchy
   * @param qualifiers2 a set of qualifiers; exactly one per hierarchy
   * @return the greatest lower bound of the two sets of qualifiers
   */
  public Set<? extends AnnotationMirror> greatestLowerBoundsQualifiersOnly(
      Collection<? extends AnnotationMirror> qualifiers1,
      Collection<? extends AnnotationMirror> qualifiers2) {
    assertSameSize(qualifiers1, qualifiers2);
    if (qualifiers1.isEmpty()) {
      throw new BugInCF(
          "QualifierHierarchy.greatestLowerBounds: tried to determine GLB with empty sets");
    }

    AnnotationMirrorSet result = new AnnotationMirrorSet();
    for (AnnotationMirror a1 : qualifiers1) {
      for (AnnotationMirror a2 : qualifiers2) {
        AnnotationMirror glb = greatestLowerBoundQualifiersOnly(a1, a2);
        if (glb != null) {
          result.add(glb);
        }
      }
    }

    assertSameSize(qualifiers1, qualifiers2, result);
    return result;
  }

  /**
   * Returns the greatest lower bound of the two sets of qualifiers. The result is the lub of the
   * qualifier for the same hierarchy in each set.
   *
   * @param qualifiers1 a set of qualifiers; exactly one per hierarchy
   * @param tm1 the type that is annotated by qualifier1
   * @param qualifiers2 a set of qualifiers; exactly one per hierarchy
   * @param tm2 the type that is annotated by qualifier2
   * @return the greatest lower bound of the two sets of qualifiers
   */
  public final Set<? extends AnnotationMirror> greatestLowerBoundsShallow(
      Collection<? extends AnnotationMirror> qualifiers1,
      TypeMirror tm1,
      Collection<? extends AnnotationMirror> qualifiers2,
      TypeMirror tm2) {
    assertSameSize(qualifiers1, qualifiers2);
    if (qualifiers1.isEmpty()) {
      throw new BugInCF(
          "QualifierHierarchy.greatestLowerBounds: tried to determine GLB with empty sets");
    }

    AnnotationMirrorSet result = new AnnotationMirrorSet();
    for (AnnotationMirror a1 : qualifiers1) {
      for (AnnotationMirror a2 : qualifiers2) {
        AnnotationMirror glb = greatestLowerBoundShallow(a1, tm1, a2, tm2);
        if (glb != null) {
          result.add(glb);
        }
      }
    }

    assertSameSize(qualifiers1, qualifiers2, result);
    return result;
  }

  /**
   * Returns the greatest lower bound the all the collections of qualifiers. The result is the glb
   * of the qualifier for the same hierarchy in each set.
   *
   * @param qualifiers a collection of collections of qualifiers. Each inner collection has exactly
   *     one qualifier per hierarchy.
   * @return the greatest lower bound of the collections of qualifiers
   */
  public Set<? extends AnnotationMirror> greatestLowerBoundsQualifiersOnly(
      Collection<? extends Collection<? extends AnnotationMirror>> qualifiers) {
    if (qualifiers.isEmpty()) {
      return AnnotationMirrorSet.emptySet();
    }
    Iterator<? extends Collection<? extends AnnotationMirror>> itor = qualifiers.iterator();
    Set<? extends AnnotationMirror> result = new AnnotationMirrorSet(itor.next());
    while (itor.hasNext()) {
      Collection<? extends AnnotationMirror> annos = itor.next();
      result = greatestLowerBoundsQualifiersOnly(result, annos);
    }
    return result;
  }

  /**
   * Returns true if and only if {@link AnnotatedTypeMirror#getPrimaryAnnotations()} can return a
   * set with fewer qualifiers than the width of the QualifierHierarchy.
   *
   * @param type the type to test
   * @return true if and only if {@link AnnotatedTypeMirror#getPrimaryAnnotations()} can return a
   *     set with fewer qualifiers than the width of the QualifierHierarchy
   */
  public static boolean canHaveEmptyAnnotationSet(AnnotatedTypeMirror type) {
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
   * @param qualifiers the set of annotations to search
   * @param qualifier annotation that is in the same hierarchy as the returned annotation
   * @return annotation in the same hierarchy as qualifier, or null if one is not found
   */
  public @Nullable AnnotationMirror findAnnotationInSameHierarchy(
      Collection<? extends AnnotationMirror> qualifiers, AnnotationMirror qualifier) {
    AnnotationMirror top = this.getTopAnnotation(qualifier);
    return findAnnotationInHierarchy(qualifiers, top);
  }

  /**
   * Returns the annotation in {@code qualifiers} that is in the hierarchy for which {@code top} is
   * top.
   *
   * @param qualifiers the set of annotations to search
   * @param top the top annotation in the hierarchy to which the returned annotation belongs
   * @return annotation in the same hierarchy as annotationMirror, or null if one is not found
   */
  public @Nullable AnnotationMirror findAnnotationInHierarchy(
      Collection<? extends AnnotationMirror> qualifiers, AnnotationMirror top) {
    for (AnnotationMirror anno : qualifiers) {
      if (isSubtypeQualifiers(anno, top)) {
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
  public <T> boolean updateMappingToMutableSet(
      Map<T, AnnotationMirrorSet> map, T key, AnnotationMirror qualifier) {
    // https://github.com/typetools/checker-framework/issues/2000
    @SuppressWarnings("nullness:argument")
    boolean mapContainsKey = map.containsKey(key);
    if (mapContainsKey) {
      @SuppressWarnings("nullness:assignment") // key is a key for map.
      @NonNull AnnotationMirrorSet prevs = map.get(key);
      AnnotationMirror old = findAnnotationInSameHierarchy(prevs, qualifier);
      if (old != null) {
        return false;
      }
      prevs.add(qualifier);
      map.put(key, prevs);
    } else {
      AnnotationMirrorSet set = new AnnotationMirrorSet();
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
  public static void assertSameSize(Collection<?> c1, Collection<?> c2) {
    if (c1.size() != c2.size()) {
      throw new BugInCF(
          "inconsistent sizes (%d, %d):%n  [%s]%n  [%s]",
          c1.size(), c2.size(), StringsPlume.join(",", c1), StringsPlume.join(",", c2));
    }
  }

  /**
   * Throws an exception if the result and the inputs do not all have the same size.
   *
   * @param c1 the first collection
   * @param c2 the second collection
   * @param result the result collection
   */
  public static void assertSameSize(
      @MustCallUnknown Collection<? extends @MustCallUnknown Object> c1,
      @MustCallUnknown Collection<? extends @MustCallUnknown Object> c2,
      @MustCallUnknown Collection<? extends @MustCallUnknown Object> result) {
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
