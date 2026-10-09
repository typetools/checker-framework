package org.checkerframework.common.value;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * The dataflow analysis for the Value Checker.
 *
 * <p>It differs from the default analysis only in that it converts the {@code IntRangeFrom*}
 * annotations, which are aliases that the Index Checker introduces, into the {@code IntRange}
 * annotations that the rest of the Value Checker reasons about. Without this conversion, whether an
 * abstract value holds an alias or an {@code IntRange} would depend on whether the value happens to
 * have been merged at a control-flow join point.
 */
public class ValueAnalysis extends CFAnalysis {

  /** The type factory, with a more specific type than the superclass field. */
  private final ValueAnnotatedTypeFactory valueAtypeFactory;

  /**
   * Creates a new {@code ValueAnalysis}.
   *
   * @param checker the checker
   * @param factory the type factory
   */
  public ValueAnalysis(BaseTypeChecker checker, ValueAnnotatedTypeFactory factory) {
    super(checker, factory);
    this.valueAtypeFactory = factory;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This conversion bounds an {@code IntRangeFrom*} alias by the maximum value of {@code
   * underlyingType}, whereas {@code ValueQualifierHierarchy.leastUpperBoundQualifiers} bounds it by
   * {@code Long.MAX_VALUE}. The bound here is the more precise of the two.
   */
  @Override
  public @Nullable CFValue createAbstractValue(
      AnnotationMirrorSet annotations, TypeMirror underlyingType) {
    return super.createAbstractValue(
        valueAtypeFactory.convertSpecialIntRangeToStandardIntRange(annotations, underlyingType),
        underlyingType);
  }
}
