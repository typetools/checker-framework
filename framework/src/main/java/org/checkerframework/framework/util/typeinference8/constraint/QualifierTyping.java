package org.checkerframework.framework.util.typeinference8.constraint;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
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

  /** The qualifiers on the left hand side of the constraint. */
  private final Set<AnnotationMirror> Q;

  /** The qualifiers on the right hand side of the constraint. */
  private final Set<AnnotationMirror> R;

  /**
   * Kind of constraint. One of: {@link Kind#QUALIFIER_SUBTYPE} or {@link Kind#QUALIFIER_EQUALITY}.
   */
  private final Kind kind;

  /**
   * Creates a qualifier typing constraint.
   *
   * @param Q the qualifiers on the left hand side of the constraint
   * @param R the qualifiers on the right hand side of the constraint
   * @param kind the kind of qualifier constraint
   */
  public QualifierTyping(Set<AnnotationMirror> Q, Set<AnnotationMirror> R, Kind kind) {
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
        return reduceSubtyping(context);
      case QUALIFIER_SUBTYPE:
        return reduceEquality(context);
      default:
        throw new BugInCF("Unexpected kind: " + getKind());
    }
  }

  /**
   * Reduce this constraint
   *
   * @param context the context
   * @return the result of reducing this constraint
   */
  private ReductionResult reduceSubtyping(Java8InferenceContext context) {
    if (context.typeFactory.getQualifierHierarchy().isSubtypeQualifiersOnly(Q, R)
        && context.typeFactory.getQualifierHierarchy().isSubtypeQualifiersOnly(R, Q)) {
      return ConstraintSet.TRUE;
    }
    return ConstraintSet.TRUE_ANNO_FAIL;
  }

  /**
   * Reduce this constraint
   *
   * @param context the context
   * @return the result of reducing this constraint
   */
  private ReductionResult reduceEquality(Java8InferenceContext context) {
    if (context.typeFactory.getQualifierHierarchy().isSubtypeQualifiersOnly(Q, R)) {
      return ConstraintSet.TRUE;
    }
    return ConstraintSet.TRUE_ANNO_FAIL;
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
