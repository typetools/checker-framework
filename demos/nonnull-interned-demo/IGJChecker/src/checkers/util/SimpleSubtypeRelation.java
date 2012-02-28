package checkers.util;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.*;

import checkers.types.*;
import static checkers.types.AnnotatedTypeMirror.*;

/**
 * Abstracts away the straightforward subtyping relation, like that of the
 * Interned and NonNull checkers.
 */
public @Deprecated class SimpleSubtypeRelation {
    
    /** The qualifier for the subtype. */
    protected final AnnotationMirror subtype;
    
    /** The qualifier for the supertype. */
    protected final AnnotationMirror supertype;

    /** Used internally for raising types to supertypes. */
    protected final AnnotatedTypes atypes;

    /**
     * Creates a new SimpleSubtypeRelation for comparing types in type systems
     * that contain two qualified types where one is a subtype of the other.
     * 
     * @param subtype the subtype qualifier
     * @param supertype the supertype qualifier, or null if the supertype is the
     *        qualified type
     * @param atypes the {@link AnnotatedTypes} instance for the checker's
     *        context
     */
    public SimpleSubtypeRelation(AnnotationMirror subtype, AnnotationMirror supertype, AnnotatedTypes atypes) {
        this.subtype = subtype;
        this.supertype = supertype;
        this.atypes = atypes;
    }

    private int typeArgumentsCount(AnnotatedTypeMirror type) {
        if (type instanceof AnnotatedDeclaredType)
            return ((AnnotatedDeclaredType)type).getTypeArguments().size();
        else if (type instanceof AnnotatedArrayType)
            return 1;
        return 0;
    }

    private boolean compareSame(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
       return bothAnnotated(lhs, rhs) && compareTypes(lhs, rhs);
    }
    
    private boolean bothAnnotated(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
        // TODO: look at wildcard bounds!
        if (lhs.getKind() == TypeKind.TYPEVAR || rhs.getKind() == TypeKind.TYPEVAR)
            return true;
        if (lhs.getKind() == TypeKind.WILDCARD || rhs.getKind() == TypeKind.WILDCARD)
            return true;
        if (lhs.getKind() == TypeKind.NULL || rhs.getKind() == TypeKind.NULL)
            return true;
        return lhs.hasAnnotation(this.subtype) == rhs.hasAnnotation(this.subtype);
    }

    private boolean compareTypes(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
        
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
                        result &= compareSame(lhDecl.getTypeArguments().get(i), rhDecl.getTypeArguments().get(i));
                }
            } else if (lhs instanceof AnnotatedArrayType && rhs instanceof AnnotatedArrayType) {
                AnnotatedArrayType lhArray = (AnnotatedArrayType)lhs;
                AnnotatedArrayType rhArray = (AnnotatedArrayType)rhs;
                result = compareSame(lhArray.getComponentType(), rhArray.getComponentType());
            } else throw new AssertionError("must be either both generic or both array types");
        }

        return result;
    }
        public boolean isSubtype(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
        if (lhs == null || rhs == null) return false;
              
        // Try to raise the type to its supertype.
        AnnotatedTypeMirror valueBaseType = atypes.asSuper(rhs, lhs);
        if (valueBaseType == null)
            valueBaseType = rhs;
        
        if ((lhs.getKind().isPrimitive() && !valueBaseType.getKind().isPrimitive())
        	|| (!lhs.getKind().isPrimitive() && valueBaseType.getKind().isPrimitive()))
            return true;
        
        // Treat null as a subtype of everything.
        if (valueBaseType.getKind() == TypeKind.NULL 
                && !valueBaseType.hasAnnotation(this.supertype)) 
            return true;
        
	// Check type variable bounds.
        if (lhs instanceof AnnotatedTypeVariable) {
            AnnotatedTypeVariable tv = (AnnotatedTypeVariable)lhs;
            if (tv.getUpperBound().hasAnnotation(this.subtype) 
                    && !valueBaseType.hasAnnotation(this.subtype))
                return false;
        }
        
        // T is a subtype of !T.
        if (lhs.hasAnnotation(this.subtype) 
                && !valueBaseType.hasAnnotation(this.subtype))
            return false;
        
        // Examine inner types of array/generic types.
        return compareTypes(lhs, valueBaseType);
    }
    
}
