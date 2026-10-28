package org.checkerframework.common.accumulation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * AccumulationValue holds additional information about accumulated facts ("values", not to be
 * confused with "Value" in the name of this class) that cannot be stored in the accumulation type,
 * because they are not a refinement of that type. This situation occurs for type variables and
 * wildcards, for which calling {@link AccumulationTransfer#accumulate(Node, TransferResult,
 * String...)} would otherwise have no effect (since the types are invariant: {@code T} is not a
 * supertype of {@code Accumulator(a) T} unless both bounds of T are supertypes of Accumulator(a)).
 * This enables an accumulation checker (or, typically, a client of that accumulation checker) to
 * resolve accumulated facts even on types that are type variables. For example, the Resource Leak
 * Checker uses this facility to check that calls to close() on variables whose type is a type
 * variable have actually occurred, such as in this example:
 *
 * <pre><code>
 *   public static &lt;T extends java.io.Closeable&gt; void close(
 *       &#064;Owning @MustCall("close") T value) throws Exception {
 *     value.close();
 *   }
 * </code></pre>
 */
public class AccumulationValue extends CFAbstractValue<AccumulationValue> {

  /**
   * If the underlying type is a type variable or a wildcard and the annotations include an
   * accumulator annotation, then this is the set of accumulated values, which may be empty.
   * Otherwise, it is null.
   */
  private @Nullable Set<String> accumulatedValues = null;

  /**
   * Creates a new AccumulationValue.
   *
   * @param analysis the analysis class this value belongs to
   * @param annotations the annotations in this abstract value
   * @param underlyingType the underlying (Java) type in this abstract value
   */
  protected AccumulationValue(
      CFAbstractAnalysis<AccumulationValue, ?, ?> analysis,
      AnnotationMirrorSet annotations,
      TypeMirror underlyingType) {
    super(analysis, annotations, underlyingType);
    if (underlyingType.getKind() == TypeKind.TYPEVAR
        || underlyingType.getKind() == TypeKind.WILDCARD) {
      AccumulationAnnotatedTypeFactory typeFactory =
          (AccumulationAnnotatedTypeFactory) analysis.getTypeFactory();
      AnnotationMirror accumulator = null;
      for (AnnotationMirror anm : annotations) {
        if (typeFactory.isAccumulatorAnnotation(anm)) {
          accumulator = anm;
          break;
        }
      }
      if (accumulator != null) {
        accumulatedValues = new HashSet<>(typeFactory.getAccumulatedValues(accumulator));
      }
    }
  }

  /**
   * Creates a new AccumulationValue with the given accumulated values.
   *
   * @param analysis the analysis class this value belongs to
   * @param annotations the annotations in this abstract value
   * @param underlyingType the underlying (Java) type in this abstract value
   * @param accumulatedValues the accumulated values, or null
   */
  private AccumulationValue(
      CFAbstractAnalysis<AccumulationValue, ?, ?> analysis,
      AnnotationMirrorSet annotations,
      TypeMirror underlyingType,
      @Nullable Set<String> accumulatedValues) {
    super(analysis, annotations, underlyingType);
    this.accumulatedValues = accumulatedValues;
  }

  /**
   * If the underlying type is a type variable or a wildcard and the annotations include an
   * accumulator annotation, then this is the set of accumulated values, which may be empty.
   * Otherwise, it is null.
   *
   * @return the set (this is not a copy of the set, but an alias)
   */
  public @Nullable Set<String> getAccumulatedValues() {
    return accumulatedValues;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return super.equals(obj)
        && obj instanceof AccumulationValue other
        && Objects.equals(accumulatedValues, other.accumulatedValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), accumulatedValues);
  }

  @Override
  protected AccumulationValue upperBound(
      @Nullable AccumulationValue other, TypeMirror upperBoundTypeMirror, boolean shouldWiden) {
    AccumulationValue lub = super.upperBound(other, upperBoundTypeMirror, shouldWiden);

    if (other == null || other.accumulatedValues == null || this.accumulatedValues == null) {
      return lub;
    }
    // A bottom value represents every value having been accumulated, so it is the identity for
    // lub.  Any accumulatedValues it has (from mostSpecific) must not shrink the other's set.
    AnnotationMirror bottom = ((AccumulationAnnotatedTypeFactory) analysis.getTypeFactory()).bottom;
    boolean thisIsBottom = this.getAnnotations().contains(bottom);
    boolean otherIsBottom = other.getAnnotations().contains(bottom);
    if (thisIsBottom != otherIsBottom) {
      lub.accumulatedValues =
          new HashSet<>(thisIsBottom ? other.accumulatedValues : this.accumulatedValues);
      return lub;
    }
    // Lub the accumulatedValues by intersecting the lists as if they were sets.
    Set<String> lubAccumulatedValues = new HashSet<>(this.accumulatedValues);
    lubAccumulatedValues.retainAll(other.accumulatedValues);
    lub.accumulatedValues = lubAccumulatedValues;
    return lub;
  }

  @Override
  public @Nullable AccumulationValue mostSpecific(
      AccumulationValue other, AccumulationValue backup) {
    if (other == null) {
      return this;
    }
    AccumulationValue mostSpecific = super.mostSpecific(other, backup);
    if (mostSpecific != null) {
      return mostSpecific.withAccumulatedValues(this.accumulatedValues, other.accumulatedValues);
    }

    // mostSpecific is null if the two types are not comparable.  This is normally
    // because one of this or other is a type variable with no annotations, but the
    // other annotations are non-empty.  In this case, add the accumulatedValues to the
    // value with no annotations and return it as most specific.
    if (other.getAnnotations().isEmpty()) {
      return other.withAccumulatedValues(this.accumulatedValues, null);
    } else if (this.getAnnotations().isEmpty()) {
      return this.withAccumulatedValues(other.accumulatedValues, null);
    }
    return null;
  }

  /**
   * Returns a value that is like this one, but whose {@link #accumulatedValues} also contains
   * {@code newAccumulatedValues1} and {@code newAccumulatedValues2}. Does not side-effect this
   * value, which might be shared by multiple stores.
   *
   * @param newAccumulatedValues1 accumulated values to add, or null
   * @param newAccumulatedValues2 more accumulated values to add, or null
   * @return a value like this one whose accumulatedValues also contains {@code
   *     newAccumulatedValues1} and {@code newAccumulatedValues2}; it is {@code this} if this
   *     value's accumulatedValues already contains both
   */
  private AccumulationValue withAccumulatedValues(
      @Nullable Set<String> newAccumulatedValues1, @Nullable Set<String> newAccumulatedValues2) {
    Set<String> resultAccumulatedValues =
        unionIfLarger(accumulatedValues, newAccumulatedValues1, newAccumulatedValues2);
    if (resultAccumulatedValues == null) {
      return this;
    }
    return new AccumulationValue(
        analysis, getAnnotations(), getUnderlyingType(), resultAccumulatedValues);
  }

  @Override
  public String toString() {
    String superToString = super.toString();
    // remove last '}'
    superToString = superToString.substring(0, superToString.length() - 1);
    return superToString
        + ", "
        + "["
        + (accumulatedValues == null
            ? "null"
            : String.join(", ", accumulatedValues.toArray(new String[0])))
        + "] }";
  }
}
