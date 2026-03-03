package org.checkerframework.framework.util.typeinference8.constraint;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.util.typeinference8.types.AbstractQualifier;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.Qualifier;
import org.checkerframework.framework.util.typeinference8.types.QualifierVar;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.BugInCF;

/**
 * Represents a constraint between two {@link AbstractType}. One of:
 *
 * <ul>
 *   <li>{@link Kind#QUALIFIER_SUBTYPE} {@code < Q <: R >}: A qualifier Q is a subtype of a
 *       qualifier R.
 *   <li>{@link Kind#QUALIFIER_EQUALITY} {@code < Q = R >}: A qualifier Q is the same as a qualifier
 *       R.
 * </ul>
 */
public class QualifierTyping implements Constraint {

  /** The qualifier on the left-hand side of the constraint. */
  private final AbstractQualifier Q;

  /** The qualifier on the right-hand side of the constraint. */
  private final AbstractQualifier R;

  /**
   * Kind of constraint. One of: {@link Kind#QUALIFIER_SUBTYPE} or {@link Kind#QUALIFIER_EQUALITY}.
   */
  private final Kind kind;

  /**
   * Creates a qualifier typing constraint.
   *
   * @param Q the qualifiers on the left-hand side of the constraint
   * @param R the qualifiers on the right-hand side of the constraint
   * @param kind the kind of qualifier constraint
   */
  public QualifierTyping(AbstractQualifier Q, AbstractQualifier R, Kind kind) {
    assert Q != null && R != null;
    switch (kind) {
      case QUALIFIER_SUBTYPE:
      case QUALIFIER_EQUALITY:
        break;
      default:
        throw new BugInCF("Unexpected kind: " + kind);
    }
    this.R = R;
    this.Q = Q;
    this.kind = kind;
  }

  @Override
  public Kind getKind() {
    return kind;
  }

  @Override
  public ReductionResult reduce(Java8InferenceContext context) {
    switch (getKind()) {
      case QUALIFIER_EQUALITY:
        return reduceEquality(context);
      case QUALIFIER_SUBTYPE:
        return reduceSubtyping(context);
      default:
        throw new BugInCF("Unexpected kind: " + getKind());
    }
  }

  /**
   * Reduce this constraint.
   *
   * @param context the context
   * @return the result of reducing this constraint
   */
  private ReductionResult reduceSubtyping(Java8InferenceContext context) {
    if (Q instanceof Qualifier && R instanceof Qualifier) {
      AnnotationMirror qAnno = ((Qualifier) Q).getAnnotation();
      AnnotationMirror rAnno = ((Qualifier) R).getAnnotation();
      if (context.typeFactory.getQualifierHierarchy().isSubtypeQualifiersOnly(qAnno, rAnno)) {
        return ConstraintSet.TRUE;
      }
      return ConstraintSet.TRUE_ANNO_FAIL;
    }

    ConstraintSet constraintSet = new ConstraintSet();
    if (Q instanceof QualifierVar) {
      // Q <: R
      QualifierVar var = (QualifierVar) Q;
      constraintSet.addAll(var.addBound(BoundKind.UPPER, R));
    }
    if (R instanceof QualifierVar) {
      // Q <: R
      QualifierVar var = (QualifierVar) R;
      constraintSet.addAll(var.addBound(BoundKind.LOWER, Q));
    }
    return constraintSet;
  }

  /**
   * Reduce this constraint.
   *
   * @param context the context
   * @return the result of reducing this constraint
   */
  private ReductionResult reduceEquality(Java8InferenceContext context) {
    if (Q instanceof Qualifier && R instanceof Qualifier) {
      AnnotationMirror qAnno = ((Qualifier) Q).getAnnotation();
      AnnotationMirror rAnno = ((Qualifier) R).getAnnotation();
      if (context.typeFactory.getQualifierHierarchy().isSubtypeQualifiersOnly(qAnno, rAnno)
          && context.typeFactory.getQualifierHierarchy().isSubtypeQualifiersOnly(rAnno, qAnno)) {
        return ConstraintSet.TRUE;
      }
      return ConstraintSet.TRUE_ANNO_FAIL;
    }
    ConstraintSet constraintSet = new ConstraintSet();
    if (Q instanceof QualifierVar) {
      // Q == R
      QualifierVar var = (QualifierVar) Q;
      constraintSet.addAll(var.addBound(BoundKind.EQUAL, R));
    }
    if (R instanceof QualifierVar) {
      // Q == R
      QualifierVar var = (QualifierVar) R;
      constraintSet.addAll(var.addBound(BoundKind.EQUAL, Q));
    }
    return constraintSet;
  }

  @Override
  public String toString() {
    switch (kind) {
      case QUALIFIER_SUBTYPE:
        return Q + " <: " + R;

      case QUALIFIER_EQUALITY:
        return Q + " = " + R;
      default:
        assert false;
        return super.toString();
    }
  }
}
