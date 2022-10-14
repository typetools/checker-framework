package org.checkerframework.framework.util.typeinference8.constraint;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.util.typeinference8.bound.FalseBound;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Represents a constraint between two {@link AbstractType}. One of:
 *
 * <ul>
 *   <li>{@link Kind#QUALIFIER_SUBTYPE} {@code < Q <: R >}: A qualifier Q is a subtype of a
 *       qualifier R.
 *   <li>{@link Kind#QUALIFIER_EQUALITY} {@code < Q = R >}: A qualifier R is the same as a qualifier
 *       R.
 * </ul>
 */
public class QualifierTyping implements ReductionResult {
  enum Kind {

    /** {@code < Q <: R >}: A qualifier Q is a subtype of a qualifier R. */
    QUALIFIER_SUBTYPE,
    /** {@code < Q = R >}: A qualifier R is the same as a qualifier R. */
    QUALIFIER_EQUALITY,
  }

  private final Set<AnnotationMirror> Q;
  private final Set<AnnotationMirror> R;

  /**
   * Kind of constraint. One of: {@link Kind#QUALIFIER_SUBTYPE} or {@link Kind#QUALIFIER_EQUALITY}.
   */
  private final Kind kind;

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

  /**
   * Return one of the abstract types in this constraint.
   *
   * @return one of the abstract types in this constraint
   */
  public AbstractType getS() {
    return S;
  }

  @Override
  public Kind getKind() {
    return kind;
  }

  @Override
  public ReductionResult reduce(Java8InferenceContext context) {
    switch (getKind()) {
      case TYPE_COMPATIBILITY:
        return reduceCompatible();
      case SUBTYPE:
        return reduceSubtyping();
      case CONTAINED:
        return reduceContained();
      case TYPE_EQUALITY:
        return reduceEquality();
      default:
        throw new BugInCF("Unexpected kind: " + getKind());
    }
  }

  /**
   * Returns the result of reducing this constraint, assuming it is a subtyping constraint. See JLS
   * 18.2.3.
   */
  private ReductionResult reduceSubtyping() {
    if (S.isProper() && T.isProper()) {
      ReductionResult isSubtype = ((ProperType) S).isSubType((ProperType) T);
      if (isSubtype == ConstraintSet.TRUE) {
        return ConstraintSet.TRUE;
      } else if (((ProperType) S).isSubTypeUnchecked((ProperType) T) == ConstraintSet.TRUE) {
        return ReductionResult.UNCHECKED_CONVERSION;
      }
      return isSubtype;
    } else if (S.getTypeKind() == TypeKind.NULL) {
      if (T.isVariable()) {
        ((Variable) T)
            .getBounds()
            .addQualifierBound(BoundKind.LOWER, S.getAnnotatedType().getAnnotations());
      } else {
        // TODO: check that null is a subtype.
        // T is an Inference Type
        return ConstraintSet.TRUE;
      }
    } else if (T.getTypeKind() == TypeKind.NULL) {
      return ConstraintSet.FALSE;
    }

    if (S.isVariable() || T.isVariable()) {
      if (S.isVariable()) {
        if (T.getTypeKind() == TypeKind.TYPEVAR && T.isLowerBoundTypeVariable()) {
          ((Variable) S).getBounds().addBound(BoundKind.UPPER, T.getTypeVarLowerBound());
        } else {
          ((Variable) S).getBounds().addBound(BoundKind.UPPER, T);
        }
      }
      if (T.isVariable()) {
        if (TypesUtils.isCapturedTypeVariable(S.getJavaType())) {
          ((Variable) T).getBounds().addBound(BoundKind.LOWER, S.getTypeVarUpperBound());
        }
        ((Variable) T).getBounds().addBound(BoundKind.LOWER, S);
      }
      return ConstraintSet.TRUE;
    }

    switch (T.getTypeKind()) {
      case DECLARED:
        return reduceSubtypeClass();
      case ARRAY:
        return reduceSubtypeArray();
      case WILDCARD:
      case TYPEVAR:
        return reduceSubtypeTypeVariable();
      case INTERSECTION:
        return reduceSubtypingIntersection();
      default:
        return ConstraintSet.FALSE;
    }
  }

  /**
   * Returns the result of reducing this constraint, assuming it is a subtyping constraint where
   * {@code T} is a class type. See JLS 18.2.3.
   */
  private ReductionResult reduceSubtypeClass() {
    if (T.isParameterizedType()) {
      // let A1, ..., An be the type arguments of T. Among the supertypes of S, a
      // corresponding class or interface type is identified, with type arguments B1, ...,
      // Bn. If no such type exists, the constraint reduces to false. Otherwise, the
      // constraint reduces to the following new constraints:
      // for all i (1 <= i <= n), <Bi <= Ai>.

      AbstractType sAsSuper = S.asSuper(T.getJavaType());
      if (sAsSuper == null) {
        return new FalseBound();
      } else if (sAsSuper.isRaw() || T.isRaw()) {
        return ReductionResult.UNCHECKED_CONVERSION;
      }

      List<AbstractType> Bs = sAsSuper.getTypeArguments();
      Iterator<AbstractType> As = T.getTypeArguments().iterator();
      ConstraintSet set = new ConstraintSet();
      for (AbstractType b : Bs) {
        AbstractType a = As.next();
        set.add(new QualifierTyping(b, a, Kind.CONTAINED));
      }

      return set;
    } else {
      // The constraint reduces to true if T is among the supertypes of S, and false otherwise.
      return ((InferenceType) S).isSubType((ProperType) T);
    }
  }

  /**
   * Returns the result of reducing this constraint, assuming it is a subtyping constraint where
   * {@code T} is an array type. See JLS 18.2.3.
   */
  private ReductionResult reduceSubtypeArray() {
    AbstractType msArrayType = S.getMostSpecificArrayType();
    if (msArrayType == null) {
      return ConstraintSet.FALSE;
    }
    if (msArrayType.isPrimitiveArray() && T.isPrimitiveArray()) {
      return ConstraintSet.TRUE;
    } else {
      return new QualifierTyping(
          msArrayType.getComponentType(), T.getComponentType(), Kind.SUBTYPE);
    }
  }

  /**
   * Returns the result of reducing this constraint, assuming it is a subtyping constraint where
   * {@code T} is a type variable. See JLS 18.2.3.
   */
  private ReductionResult reduceSubtypeTypeVariable() {
    if (S.getTypeKind() == TypeKind.INTERSECTION) {
      return ConstraintSet.TRUE;
    } else if (T.getTypeKind() == TypeKind.TYPEVAR && T.isLowerBoundTypeVariable()) {
      return new QualifierTyping(S, T.getTypeVarLowerBound(), Kind.SUBTYPE);
    } else if (T.getTypeKind() == TypeKind.WILDCARD && T.isLowerBoundedWildcard()) {
      return new QualifierTyping(S, T.getWildcardLowerBound(), Kind.SUBTYPE);
    } else {
      return ConstraintSet.FALSE;
    }
  }

  /**
   * Returns the result of reducing this constraint, assuming it is a subtyping constraint where
   * {@code T} is an intersection type. See JLS 18.2.3.
   */
  private ReductionResult reduceSubtypingIntersection() {
    ConstraintSet constraintSet = new ConstraintSet();
    for (AbstractType bound : T.getIntersectionBounds()) {
      constraintSet.add(new QualifierTyping(S, bound, Kind.SUBTYPE));
    }
    return constraintSet;
  }

  /**
   * Returns the result of reducing this constraint, assuming it is a containment constraint. See
   * JLS 18.2.3.
   */
  private ReductionResult reduceContained() {
    if (T.getTypeKind() != TypeKind.WILDCARD) {
      if (S.getTypeKind() != TypeKind.WILDCARD) {
        return new QualifierTyping(S, T, Kind.TYPE_EQUALITY);
      } else {
        return ConstraintSet.FALSE;
      }
    } else if (T.isUnboundWildcard()) {
      return ConstraintSet.TRUE;
    } else if (T.isUpperBoundedWildcard()) {
      AbstractType bound = T.getWildcardUpperBound();
      if (S.getTypeKind() == TypeKind.WILDCARD) {
        if (S.isUnboundWildcard() || S.isUpperBoundedWildcard()) {
          return new QualifierTyping(S.getWildcardUpperBound(), bound, Kind.SUBTYPE);
        } else {
          return new QualifierTyping(S.getWildcardLowerBound(), bound, Kind.TYPE_EQUALITY);
        }
      } else {
        return new QualifierTyping(S, bound, Kind.SUBTYPE);
      }
    } else { // T is lower bounded wildcard
      AbstractType tPrime = T.getWildcardLowerBound();
      if (S.getTypeKind() != TypeKind.WILDCARD) {
        return new QualifierTyping(tPrime, S, Kind.SUBTYPE);
      } else if (S.isLowerBoundedWildcard()) {
        return new QualifierTyping(tPrime, S.getWildcardLowerBound(), Kind.SUBTYPE);
      } else {
        return ConstraintSet.FALSE;
      }
    }
  }

  /**
   * Returns the result of reducing this constraint, assume it is a type compatibility constraint.
   * See JLS 18.2.2
   */
  private ReductionResult reduceCompatible() {
    if (T.isProper() && S.isProper()) {
      // the constraint reduces to true if S is compatible in a loose invocation context
      // with T (5.3), and false otherwise.
      ReductionResult r = ((ProperType) S).isSubTypeUnchecked((ProperType) T);
      if (ConstraintSet.TRUE == r) {
        return ConstraintSet.TRUE;
      }
      return ((ProperType) S).isAssignable((ProperType) T);
    } else if (S.isProper() && S.getTypeKind().isPrimitive()) {
      return new QualifierTyping(((ProperType) S).boxType(), T, Kind.TYPE_COMPATIBILITY);
    } else if (T.isProper() && T.getTypeKind().isPrimitive()) {
      return new QualifierTyping(S, ((ProperType) T).boxType(), Kind.TYPE_EQUALITY);
    } else if (T.isParameterizedType() && !S.isVariable()) {
      // Otherwise, if T is a parameterized type of the form G<T1, ..., Tn>,
      // and there exists no type of the form G<...> that is a supertype of S,
      // but the raw type G is a supertype of S, then the constraint reduces to true.
      AbstractType superS = S.asSuper(T.getJavaType());
      if (superS != null && superS.isRaw()) {
        return ReductionResult.UNCHECKED_CONVERSION;
      }
    } else if (T.getTypeKind() == TypeKind.ARRAY && T.getComponentType().isParameterizedType()) {
      AbstractType superS = S.asSuper(T.getJavaType());
      if (superS != null && superS.getComponentType().isRaw()) {
        return ReductionResult.UNCHECKED_CONVERSION;
      }
    }

    return new QualifierTyping(S, T, Kind.SUBTYPE);
  }

  /**
   * Returns the result of reducing this constraint, assume it is an equality constraint. See JLS
   * 18.2.4
   */
  private ReductionResult reduceEquality() {
    if (S.isProper()) {
      if (T.isProper()) {
        // If S and T are proper types, the constraint reduces to true if S is the same
        // as T (4.3.4), and false otherwise.
        return ConstraintSet.TRUE;
      }
      ProperType sProper = (ProperType) S;
      if (sProper.getTypeKind() == TypeKind.NULL || sProper.getTypeKind().isPrimitive()) {
        // if S or T is the null type, the constraint reduces to false.
        return ConstraintSet.FALSE;
      }
    } else if (T.isProper()) {
      ProperType tProper = (ProperType) T;
      if (tProper.getTypeKind() == TypeKind.NULL || tProper.getTypeKind().isPrimitive()) {
        // if S or T is the null type, the constraint reduces to false.
        return ConstraintSet.FALSE;
      }
    }

    if (S.isVariable() || T.isVariable()) {
      if (S.isVariable()) {
        ((Variable) S).getBounds().addBound(BoundKind.EQUAL, T);
      }
      if (T.isVariable()) {
        ((Variable) T).getBounds().addBound(BoundKind.EQUAL, S);
      }
      return ConstraintSet.TRUE;
    }

    List<AbstractType> sTypeArgs = S.getTypeArguments();
    List<AbstractType> tTypeArgs = T.getTypeArguments();
    if (sTypeArgs != null && tTypeArgs != null && sTypeArgs.size() == tTypeArgs.size()) {
      // Assume if both have type arguments, then S and T are class or interface types with
      // the same erasure
      ConstraintSet constraintSet = new ConstraintSet();
      for (int i = 0; i < tTypeArgs.size(); i++) {
        if (tTypeArgs.get(i) != sTypeArgs.get(i)) {
          constraintSet.add(
              new QualifierTyping(tTypeArgs.get(i), sTypeArgs.get(i), Kind.TYPE_EQUALITY));
        }
      }
      return constraintSet;
    }

    AbstractType sComponentType = S.getComponentType();
    AbstractType tComponentType = T.getComponentType();
    if (sComponentType != null && tComponentType != null) {
      return new QualifierTyping(sComponentType, tComponentType, Kind.TYPE_EQUALITY);
    }

    if (T.getTypeKind() == TypeKind.WILDCARD && S.getTypeKind() == TypeKind.WILDCARD) {
      if (T.isUnboundWildcard() && S.isUnboundWildcard()) {
        return ConstraintSet.TRUE;
      } else if (!S.isLowerBoundedWildcard() && !T.isLowerBoundedWildcard()) {
        return new QualifierTyping(
            S.getWildcardUpperBound(), T.getWildcardUpperBound(), Kind.TYPE_EQUALITY);
      } else if (T.isLowerBoundedWildcard() && S.isLowerBoundedWildcard()) {
        return new QualifierTyping(
            T.getWildcardLowerBound(), S.getWildcardLowerBound(), Kind.TYPE_EQUALITY);
      }
    }
    return ConstraintSet.FALSE;
  }

  @Override
  public String toString() {
    switch (kind) {
      case TYPE_COMPATIBILITY:
        return S + " -> " + T;
      case SUBTYPE:
        return S + " <: " + T;
      case CONTAINED:
        return S + " <= " + T;
      case TYPE_EQUALITY:
        return S + " = " + T;
      default:
        assert false;
        return super.toString();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    QualifierTyping typing = (QualifierTyping) o;

    return S.equals(typing.S) && kind == typing.kind;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + S.hashCode();
    result = 31 * result + kind.hashCode();
    return result;
  }
}
