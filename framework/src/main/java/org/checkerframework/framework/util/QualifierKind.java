package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.Set;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Represents a kind of qualifier, which is an annotation class. If two qualifiers use the same
 * annotation class, then they have the same qualifier kind. Two qualifiers can have the same "kind"
 * of qualifier but not be the same qualifier; an example is {@code @IndexFor("a")} and
 * {@code @IndexFor("b")}.
 *
 * <p>A {@code QualifierKind} holds information about the relationship between itself and other
 * {@link QualifierKind}s.
 *
 * <p>Exactly one qualifier kind is created for each annotation class.
 *
 * <p>The set of all QualifierKinds for a checker is like an enum. One QualifierKind is like an enum
 * constant in that they are immutable after initialization and only a finite number exist per type
 * system.
 */
@AnnotatedFor("nullness")
public @Interned interface QualifierKind extends Comparable<QualifierKind> {

  /**
   * Returns the canonical name of the annotation class of this.
   *
   * @return the canonical name of the annotation class of this
   */
  @Interned @CanonicalName String getName();

  /**
   * Returns the annotation class for this.
   *
   * @return the annotation class for this
   */
  Class<? extends Annotation> getAnnotationClass();

  /**
   * Returns the top qualifier kind of the hierarchy to which this qualifier kind belongs.
   *
   * @return the top qualifier kind of the hierarchy to which this qualifier kind belongs
   */
  QualifierKind getTop();

  /**
   * Returns true if this is the top qualifier of its hierarchy.
   *
   * @return true if this is the top qualifier of its hierarchy
   */
  boolean isTop();

  /**
   * Returns the bottom qualifier kind of the hierarchy to which this qualifier kind belongs.
   *
   * @return the bottom qualifier kind of the hierarchy to which this qualifier kind belongs
   */
  QualifierKind getBottom();

  /**
   * Returns true if this is the bottom qualifier of its hierarchy.
   *
   * @return true if this is the bottom qualifier of its hierarchy
   */
  boolean isBottom();

  /**
   * Returns the polymorphic qualifier kind of the hierarchy to which this qualifier kind belongs,
   * or null if one does not exist.
   *
   * @return the polymorphic qualifier kind of the hierarchy to which this qualifier kind belongs,
   *     or null if one does not exist
   */
  @Nullable QualifierKind getPolymorphic();

  /**
   * Returns true if this is polymorphic.
   *
   * @return true if this is polymorphic
   */
  @Pure
  boolean isPoly();

  /**
   * Returns true if the annotation class this qualifier kind represents has annotation
   * elements/arguments.
   *
   * @return true if the annotation class this qualifier kind represents has elements/arguments
   */
  boolean hasElements();

  /**
   * All the qualifier kinds that are a strict super qualifier of this qualifier. Does not include
   * this qualifier kind itself.
   *
   * @return all the qualifier kinds that are a strict super qualifier of this qualifier
   */
  Set<? extends QualifierKind> getStrictSuperTypes();

  /**
   * Returns true if this and {@code other} are in the same hierarchy.
   *
   * @param other a qualifier kind
   * @return true if this and {@code other} are in the same hierarchy
   */
  boolean isInSameHierarchyAs(QualifierKind other);

  /**
   * Returns true if this qualifier kind is a subtype of or equal to {@code superQualKind}.
   *
   * @param superQualKind other qualifier kind
   * @return true if this qualifier kind is a subtype of or equal to {@code superQualKind}
   */
  boolean isSubtypeOf(QualifierKind superQualKind);

  @Override
  default int compareTo(QualifierKind o) {
    return this.getName().compareTo(o.getName());
  }
}
