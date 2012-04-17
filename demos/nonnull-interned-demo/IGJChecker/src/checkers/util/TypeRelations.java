package checkers.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;

import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotationRelations;
import checkers.types.AnnotatedTypeMirror.*;

/**
 * Abstracts away a subtyping relation for types
 */
public class TypeRelations {
    private final AnnotationRelations annotationRelations;
    private final ProcessingEnvironment env;
    private final AnnotatedTypeFactory factory;
    
    /** Used internally for raising types to supertypes. */
    protected final AnnotatedTypes atypes;

    public TypeRelations(AnnotatedTypeFactory factory, ProcessingEnvironment env, AnnotationRelations annotationRelations) {
        this.env = env;
        this.annotationRelations = annotationRelations;
        this.factory = factory;
        this.atypes = new AnnotatedTypes(env, factory);
    }

    private boolean compareSame(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
        return bothAnnotated(lhs, rhs) && compareTypes(lhs, rhs);
     }

    private boolean bothAnnotated(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
        if (lhs.getKind() == TypeKind.TYPEVAR && rhs.getKind() != TypeKind.TYPEVAR)
            lhs = ((AnnotatedTypeVariable)lhs).getUpperBound();
        if (lhs.getKind() == TypeKind.WILDCARD && rhs.getKind() != TypeKind.WILDCARD) {
            lhs = ((AnnotatedWildcardType)lhs).getExtendsBound();
            if (lhs == null) return true;
        }
        if (lhs.getKind().isPrimitive() && rhs.getKind() == TypeKind.DECLARED)
            rhs = factory.getUnboxedType((AnnotatedDeclaredType)rhs);
        if (lhs.getKind() == TypeKind.DECLARED && rhs.getKind().isPrimitive())
            rhs = factory.getBoxedType((AnnotatedPrimitiveType)rhs);

        AnnotationMirror la = annotationRelations.getAnnotation(lhs.getAnnotations());
        AnnotationMirror ra = annotationRelations.getAnnotation(rhs.getAnnotations());
        if (la == ra)
            return true;
        else if ((la == null) || (ra == null))
            return false;
        
        boolean result = la.getAnnotationType().asElement().equals(ra.getAnnotationType().asElement());
        result &= env.getElementUtils().getElementValuesWithDefaults(la).equals(
                env.getElementUtils().getElementValuesWithDefaults(ra));
        
        return result;
    }
    
    private boolean compareTypes(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
        if (lhs.getKind() == TypeKind.TYPEVAR && rhs.getKind() != TypeKind.TYPEVAR)
            lhs = ((AnnotatedTypeVariable)lhs).getUpperBound();
        if (lhs.getKind() == TypeKind.WILDCARD && rhs.getKind() != TypeKind.WILDCARD) {
            lhs = ((AnnotatedWildcardType)lhs).getExtendsBound();
            if (lhs == null) return true;
        }
        if (lhs.getKind().isPrimitive() && rhs.getKind() == TypeKind.DECLARED)
            rhs = factory.getUnboxedType((AnnotatedDeclaredType)rhs);
        if (lhs.getKind() == TypeKind.DECLARED && rhs.getKind().isPrimitive())
            rhs = factory.getBoxedType((AnnotatedPrimitiveType)rhs);

        // TODO: this should never be the case?
        assert (!(rhs.getKind() == TypeKind.NULL));
        
        if (lhs.getKind() == TypeKind.DECLARED && rhs.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType lhDecl = (AnnotatedDeclaredType)lhs;
            AnnotatedDeclaredType rhDecl = (AnnotatedDeclaredType)rhs;
            if (!lhDecl.getTypeArguments().isEmpty() && !rhDecl.getTypeArguments().isEmpty()) {
                boolean result = true;
                for (int i = 0; i < lhDecl.getTypeArguments().size(); ++i) {
                    result &= compareSame(lhDecl.getTypeArguments().get(i), rhDecl.getTypeArguments().get(i));
                }
                return result;
            }
        } else if (lhs.getKind() == TypeKind.ARRAY && rhs.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType lhArray = (AnnotatedArrayType)lhs;
            AnnotatedArrayType rhArray = (AnnotatedArrayType)rhs;
            if (lhArray.getKind() == TypeKind.DECLARED || lhs.getKind() == TypeKind.ARRAY) {
                rhArray = (AnnotatedArrayType) atypes.asSuper(rhs, lhArray);
                if (rhArray == null) rhArray = (AnnotatedArrayType) rhs;
            }
            return isSubtype(lhArray.getComponentType(), rhArray.getComponentType());
        }
        return true;
    }

    /**
     * Checks two types of subtyping.
     */
    public boolean isSubtype(AnnotatedTypeMirror lhs, AnnotatedTypeMirror rhs) {
        if (lhs.getKind() == TypeKind.TYPEVAR && rhs.getKind() != TypeKind.TYPEVAR)
            lhs = ((AnnotatedTypeVariable)lhs).getUpperBound();
        if (lhs.getKind() == TypeKind.WILDCARD && rhs.getKind() != TypeKind.WILDCARD) {
            lhs = ((AnnotatedWildcardType)lhs).getExtendsBound();
            if (lhs == null) return true;
        }
        if (lhs.getKind().isPrimitive() && rhs.getKind() == TypeKind.DECLARED)
            rhs = factory.getUnboxedType((AnnotatedDeclaredType)rhs);
        if (lhs.getKind() == TypeKind.DECLARED && rhs.getKind().isPrimitive())
            rhs = factory.getBoxedType((AnnotatedPrimitiveType)rhs);

        AnnotationMirror lhsAnnotation = annotationRelations.getAnnotation(lhs.getAnnotations());
        AnnotationMirror rhsAnnotation = annotationRelations.getAnnotation(rhs.getAnnotations());
        
        boolean result = annotationRelations.isSubtype(lhsAnnotation, rhsAnnotation);
        
        if (lhs.getKind() == TypeKind.DECLARED || lhs.getKind() == TypeKind.ARRAY) {
            AnnotatedTypeMirror rhsTemp = atypes.asSuper(rhs, lhs);
            if (rhsTemp != null) rhsTemp = rhs;
            if (rhs.getKind() == TypeKind.DECLARED || rhs.getKind() == TypeKind.ARRAY)
                result &= compareTypes(lhs, rhs);
        }
            
        return result;
    }
}
