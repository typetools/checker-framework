package checkers.util;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedNullType;

/**
 * Abstracts away a subtyping relation, relegating the comparisons
 * to a method that ignores the generic / array structure.
 */
public abstract class SubtypeRelation {

    private int typeArgumentsCount(AnnotatedTypeMirror type) {
        if (type instanceof AnnotatedDeclaredType)
            return ((AnnotatedDeclaredType)type).getTypeArguments().size();
        else if (type instanceof AnnotatedArrayType)
            return 1;
        return 0;
    }

    /**
     * Checks two types for subtyping ignoring type parameters.
     */
    abstract protected boolean isSubtypeIgnoringTypeParameters(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs);

    /**
     * Checks two types of subtyping.
     */
    public boolean isSubtype(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
        if (!isSubtypeIgnoringTypeParameters(lhs, rhs)) return false;

        // TODO: this should never be the case?
        if (rhs instanceof AnnotatedNullType)
            return true;

        // Count type args.
        int lhArgs = typeArgumentsCount(lhs);
        int rhArgs = typeArgumentsCount(rhs);

        if ((lhArgs > 0 && rhArgs == 0) || (rhArgs > 0 && lhArgs == 0))
            return true;

        boolean result = true;

        if (lhArgs > 0 && rhArgs > 0) {
            if (lhs instanceof AnnotatedDeclaredType && rhs instanceof AnnotatedDeclaredType) {
                AnnotatedDeclaredType lhDecl = (AnnotatedDeclaredType)lhs;
                AnnotatedDeclaredType rhDecl = (AnnotatedDeclaredType)rhs;
                //assert lhArgs == rhArgs : "differing numbers of type arguments: " + lhArgs + ", " + rhArgs + " -- " + lhDecl + ", " + rhDecl; // BUG: this is an invalid assertion
                if (lhArgs == rhArgs)
                    for (int i = 0; i < lhArgs; i++) {
                        result &= isSubtypeIgnoringTypeParameters(lhDecl.getTypeArguments().get(i), rhDecl.getTypeArguments().get(i));
                }
            } else if (rhs instanceof AnnotatedArrayType && rhs instanceof AnnotatedArrayType) {
                AnnotatedArrayType lhArray = (AnnotatedArrayType)lhs;
                AnnotatedArrayType rhArray = (AnnotatedArrayType)rhs;
                result = isSubtypeIgnoringTypeParameters(lhArray.getComponentType(), rhArray.getComponentType());
            } else throw new AssertionError("must be either both generic or both array types");
        }

        return result;
    }

}
