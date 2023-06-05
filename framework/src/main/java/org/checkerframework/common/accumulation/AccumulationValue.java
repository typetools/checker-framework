package org.checkerframework.common.accumulation;

import java.util.HashSet;
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
 * String...)} would otherwise have no effect (since the types are invariant: T is not a supertype
 * of Accumulator(a) T unless both bounds of T are supertypes of Accumulator(a)). This enables an
 * accumulation checker (or, typically, a client of that accumulation checker) to resolve
 * accumulated facts even on types that are type variables. For example, the Resource Leak Checker
 * uses this facility to check that calls to close() on variables whose type is a type variable have
 * actually occurred, such as in this example:
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
   * If the underlying type is a type variable or a wildcard, then this is a set of accumulated
   * values. Otherwise, it is null.
   */
  private @Nullable Set<String> accumulatedValues = null;

  /**
   * Creates a new CFAbstractValue.
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
   * If the underlying type is a type variable or a wildcard, then this is a set of accumulated
   * values. Otherwise, it is null.
   *
   * @return the set (this is not a copy of the set, but an alias)
   */
  public Set<String> getAccumulatedValues() {
    return accumulatedValues;
  }

  @Override
  public AccumulationValue leastUpperBound(AccumulationValue other) {
    AccumulationValue lub = super.leastUpperBound(other);
    if (other == null || other.accumulatedValues == null || this.accumulatedValues == null) {
      return lub;
    }
    // Lub the accumulatedValues by intersecting the lists as if they were sets.
    lub.accumulatedValues = new HashSet<>(this.accumulatedValues.size());
    lub.accumulatedValues.addAll(this.accumulatedValues);
    lub.accumulatedValues.retainAll(other.accumulatedValues);
    if (lub.accumulatedValues.isEmpty()) {
      lub.accumulatedValues = null;
    }
    return lub;
  }

  @Override
  public AccumulationValue mostSpecific(AccumulationValue other, AccumulationValue backup) {
    if (other == null) {
      return this;
    }
    AccumulationValue mostSpecific = super.mostSpecific(other, backup);
    if (mostSpecific != null) {
      mostSpecific.addAccumulatedValues(this.accumulatedValues);
      mostSpecific.addAccumulatedValues(other.accumulatedValues);
      return mostSpecific;
    }

    // mostSpecific is null if the two types are not comparable.  This is normally
    // because one of this or other is a type variable and annotations is empty, but the
    // other annotations are not empty.  In this case, copy the accumulatedValues to the
    // value with no annotations and return it as most specific.
    if (other.getAnnotations().isEmpty()) {
      mostSpecific =
          new AccumulationValue(
              analysis, AnnotationMirrorSet.emptySet(), other.getUnderlyingType());
      mostSpecific.addAccumulatedValues(this.accumulatedValues);
      mostSpecific.addAccumulatedValues(other.accumulatedValues);
      return mostSpecific;
    } else if (this.getAnnotations().isEmpty()) {
      mostSpecific =
          new AccumulationValue(
              analysis, AnnotationMirrorSet.emptySet(), other.getUnderlyingType());
      mostSpecific.addAccumulatedValues(this.accumulatedValues);
      mostSpecific.addAccumulatedValues(other.accumulatedValues);
      return mostSpecific;
    }
    return null;
  }

  /**
   * Merges its argument into the {@link #accumulatedValues} field of this.
   *
   * @param newAccumulatedValues a new list of accumulated values
   */
  private void addAccumulatedValues(Set<String> newAccumulatedValues) {
    if (newAccumulatedValues == null || newAccumulatedValues.isEmpty()) {
      return;
    }
    if (accumulatedValues == null) {
      accumulatedValues = new HashSet<>(newAccumulatedValues.size());
    }
    accumulatedValues.addAll(newAccumulatedValues);
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
