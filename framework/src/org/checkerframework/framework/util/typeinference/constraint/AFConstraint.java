package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;

import javax.lang.model.type.TypeVariable;
import java.util.Map;
import java.util.Set;

/**
 * AFConstraint represent the initial constraints used to infer type arguments for method invocations and
 * new class invocations.  These constraints are simplified then converted to TUConstraints during
 * type argument inference..
 *
 * Subclasses of AFConstraint represent the following types of constraints found in
 * (http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.7)
 *
 * A 《 F and F 》 A both imply that A is convertible to F.
 * F 《 A and A 》 F both imply that F is convertible to A (this may happen due to wildcard/typevar bounds and recursive types)
 * A = F implies that A is exactly F
 *
 * In the Checker Framework a type, A will be convertible to another type F, if AnnotatedTypes.asSuper will return
 * a non-null value when A is passed as a subtype and F the supertype to the method.
 *
 * In Java type A will be convertible to another type F if there exists a conversion context/method that transforms
 * the one type into the other.
 *
 * A 《 F and F 》 A are represented by class A2F
 * F 《 A and A 》 F are represented by class F2A
 * F = A is represented by class FIsA
 */
public abstract class AFConstraint {
    public final AnnotatedTypeMirror argument;
    public final AnnotatedTypeMirror formalParameter;

    //used to compute hashcodes, this value should be unique for every subclass of AFConstraints
    protected final int hashcodeBase;

    public AFConstraint(final AnnotatedTypeMirror argument, final AnnotatedTypeMirror formalParameter,
                        int hashcodeBase) {
        this.argument = argument;
        this.formalParameter = formalParameter;
        this.hashcodeBase = hashcodeBase;
    }

    /**
     *
     * @param targets The type parameters whose arguments we are trying to solve for
     * @return Returns true if this constraint can't be broken up into other constraints or further simplified
     * In general, if either argument or formal parameter is a use of the type parameters we are inferring over
     * then the constraint cannot be reduced further
     */
    public boolean isIrreducible(final Set<TypeVariable> targets) {
        return TypeArgInferenceUtil.isATarget(argument, targets)
            || TypeArgInferenceUtil.isATarget(formalParameter, targets);
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        } //else

        if (thatObject == null || this.getClass() != thatObject.getClass()) {
            return false;
        }

        final AFConstraint that = (AFConstraint) thatObject;

        return this.argument.equals(that.argument)
            && this.formalParameter.equals(that.formalParameter);
    }

    @Override
    public int hashCode() {
        int result = formalParameter.hashCode();
        result = hashcodeBase * result + argument.hashCode();
        return result;
    }

    /**
     * Once AFConstraints are irreducible it can be converted to a TU constraint, constraints between
     * individual type parameters for which we are inferring an argument (T) and Java types (U).
     * @return A TUConstraint that represents this AFConstraint
     */
    public abstract TUConstraint toTUConstraint();

    /**
     * Given a partial solution to our type argument inference, replace any uses of type parameters that
     * have been solved with their arguments.
     *
     * That is:
     * Let S be a partial solution to our inference (i.e. we have inferred type arguments for some types)
     * Let S be a map {@code (T0 -> A0, T1 -> A1, ..., TN -> AN)} where Ti is a type parameter and Ai is its solved argument.
     * For all uses of Ti in this constraint, replace them with Ai.
     *
     * For the mapping {@code (T0 -> A0)}, the following constraint:
     * {@code ArrayList<T0> << List<T1>}
     *
     * Becomes:
     * {@code ArrayList<A0> << List<T1>}
     *
     * A constraint:
     * {@code T0 << T1}
     *
     * Becomes:
     * {@code A0 << T1}
     *
     *
     * @param substitutions A mapping of target type parameter to the type argument to
     * @return A new constraint that contains no use of the keys in substitutions
     */
    public AFConstraint substitute(final Map<TypeVariable, AnnotatedTypeMirror> substitutions) {
        final AnnotatedTypeMirror newArgument = TypeArgInferenceUtil.substitute(substitutions, argument);
        final AnnotatedTypeMirror newFormalParameter = TypeArgInferenceUtil.substitute(substitutions, formalParameter);
        return construct(newArgument, newFormalParameter);
    }

    /**
     * Used to create a new constraint of the same subclass of AFConstraint.
     */
    protected abstract AFConstraint construct(AnnotatedTypeMirror newArgument, AnnotatedTypeMirror newFormalParameter);
}
