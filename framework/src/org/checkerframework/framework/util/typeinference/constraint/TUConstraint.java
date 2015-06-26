package org.checkerframework.framework.util.typeinference.constraint;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

/**
 * Subclasses of TUConstraint represent constraints between type parameter whose type arguments
 * are being inferred and the types used to do that inference.  These constraints are used by
 * the TASolver to infer arguments.
 *
 * TU constraints come in the classic form of subtype, supertype, and equality constraints.
 * {@code T <: U}  - implies T is a subtype of U, it is represented by TSubU
 * {@code T >: U}  - implies T is a supertype of U, it is represented by TSuperU
 * {@code T = U}   - implies T is equal to U, it is represented by TIsU
 *
 * Note, it is important that the type parameter is represented by an AnnotatedTypeVariable
 * because if a use of the type parameter has a primary annotation, then the two types
 * represented in by a TUConstraint are NOT constrained in the hierarchy of that annotation.
 * e.g.
 * <pre>{@code
 *   <T> void method(List<@NonNull T> t1, T t2)
 *   method(new ArrayList<@NonNull String>(), null);
 * }</pre>
 *
 *   The above method call would eventually be reduced to
 *   constraints:   {@code [@NonNull String == @NonNull T, @Nullable null <: T]}
 *
 *   In this example, if we did not ignore the first constraint then the type argument
 *   would be exactly @NonNull String and the second argument would be invalid.  However,
 *   the correct inference would be @Nullable String and both arguments would be valid.
 */
public abstract class TUConstraint {
    //An AnnotatedTypeVariable representing a target type parameter for which we are inferring a type argument
    //this is the T in the TUConstraints
    public final AnnotatedTypeVariable typeVariable;

    //A type used to infer an argument for the typeVariable T, this would be the U in the TUConstraints
    public final AnnotatedTypeMirror relatedType;
    public final int hashcodeBase;

    public TUConstraint(final AnnotatedTypeVariable typeVariable, final AnnotatedTypeMirror relatedType, int hashcodeBase) {
        this.typeVariable = typeVariable;
        this.relatedType = relatedType;
        this.hashcodeBase = hashcodeBase;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        } //else

        if (thatObject == null || this.getClass() != thatObject.getClass()) {
            return false;
        }

        final TUConstraint that = (TUConstraint) thatObject;

        return this.typeVariable.equals(that.typeVariable)
            && this.relatedType.equals(that.relatedType);
    }

    @Override
    public int hashCode() {
        int result = typeVariable.hashCode();
        result = hashcodeBase * result + relatedType.hashCode();
        return result;
    }
}
