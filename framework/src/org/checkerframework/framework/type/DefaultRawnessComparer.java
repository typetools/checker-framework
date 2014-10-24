package org.checkerframework.framework.type;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.type.visitor.VisitHistory;
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.InternalUtils;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;

/**
 * Our
 * //TODO: USING THE SAME LOGIC AS THE OLD TYPE_HIERARCHY.isSubtypeAsTypeArgument but only for
 * //TODO: RAW TypeArguments, we can replace this later if we think there is a more sensible thing to do
 * //TODO: LOOK AT OLD TYPE_HIERARCHY FOR MORE INFORAMTION
 */
public class DefaultRawnessComparer extends AbstractAtmComboVisitor<Boolean, VisitHistory> {
    private DefaultTypeHierarchy typeHierarchy;

    public DefaultRawnessComparer(final DefaultTypeHierarchy typeHierarchy) {
        this.typeHierarchy = typeHierarchy;
    }

    @Override
    protected String defaultErrorMessage(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, VisitHistory visited) {
        return "DefaultRawnessComparer: Unexpected AnnotatedTypeMirror combination.\n"
                + "type1 = " + subtype.getClass().getSimpleName()   + "( " + subtype   + " )\n"
                + "type2 = " + supertype.getClass().getSimpleName() + "( " + supertype + " )\n"
                + "visitHistory = " + visited;
    }


    public boolean isValid(final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype, VisitHistory visited) {
        return AtmCombo.accept(subtype, supertype, visited, this);
    }

    //TODO: GENERAL CASE IF THE OTHERS HAVEN"T OCCURED SUCH AS DECLARED_DECLARED
    protected boolean arePrimaryAnnotationsEqual(final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype) {
        return AnnotationUtils.areSame(subtype.getAnnotations(), supertype.getAnnotations());
    }

    @Override
    public Boolean visitDeclared_Declared(AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        if(checkOrAdd(subtype, supertype, visited)) {
            return true;
        }

        if(!arePrimaryAnnotationsEqual(subtype, supertype)) {
            return false;
        }

        return typeHierarchy.visitTypeArgs(subtype, supertype, visited, subtype.wasRaw(), supertype.wasRaw());
    }


    @Override
    public Boolean visitWildcard_Wildcard(AnnotatedWildcardType subtype, AnnotatedWildcardType supertype, VisitHistory visited) {

        if(checkOrAdd(subtype, supertype, visited)) {
            return true;
        }

        if (supertype.getExtendsBoundField() == null ||
            supertype.getExtendsBoundField().getAnnotations().isEmpty()) {
            // TODO: the LHS extends bound hasn't been unfolded or defaulted.
            // Stop looking, we should be fine.
            // See tests/nullness/generics/WildcardSubtyping.java
            return true;
        }

        AnnotatedTypeMirror subtypeUpper   = subtype.getEffectiveExtendsBound();
        AnnotatedTypeMirror supertypeUpper = supertype.getEffectiveExtendsBound();

        if( subtypeUpper.getKind() == TypeKind.TYPEVAR
         && InternalUtils.isCaptured((TypeVariable) supertypeUpper.getUnderlyingType())) {
            supertypeUpper = ((AnnotatedTypeVariable) supertypeUpper).getEffectiveUpperBound();
        }

        if(checkOrAdd(subtypeUpper, supertypeUpper, visited)) {
            return true;
        }

        return typeHierarchy.isSubtype(subtypeUpper, supertypeUpper);
    }

    @Override
    public Boolean visitArray_Array(AnnotatedArrayType subtype, AnnotatedArrayType supertype, VisitHistory visited) {
        if(!arePrimaryAnnotationsEqual(subtype, supertype)) {
            return false;
        }
        return this.isValid(subtype.getComponentType(), supertype.getComponentType(), visited);
    }

    @Override
    public Boolean visitDeclared_Wildcard(AnnotatedDeclaredType subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitArray_Wildcard(AnnotatedArrayType subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Wildcard(AnnotatedTypeVariable subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitDeclared_Typevar(AnnotatedDeclaredType subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitArray_Typevar(AnnotatedArrayType subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitWildcard_Declared(AnnotatedWildcardType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
       return arePrimaryAnnotationsEqual(subtype.getEffectiveExtendsBound(), supertype);
    }

    public Boolean visitWildcardSupertype(AnnotatedTypeMirror subtype, AnnotatedWildcardType supertype, VisitHistory visited) {

        if(checkOrAdd(subtype, supertype, visited)) {
            return true;
        }

        if( !supertype.getAnnotations().isEmpty()
         && !supertype.getEffectiveAnnotations().equals(subtype.getEffectiveAnnotations())) {
            return false;
        }

        final AnnotatedTypeMirror superExtendsBound = supertype.getExtendsBound();
        if(superExtendsBound == null) {
            return true;
        }

        if(checkOrAdd(subtype, superExtendsBound, visited)) {
            return true;
        }

        return typeHierarchy.isSubtype(subtype, superExtendsBound, visited);
    }

    public Boolean visitTypevarSupertype(AnnotatedTypeMirror subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        if(checkOrAdd(subtype, supertype, visited)) {
            return true;
        }

        final AnnotatedTypeMirror superBound = supertype.getUpperBound();
        if(checkOrAdd(subtype, superBound, visited)) {
            return true;
        }

        return typeHierarchy.isSubtype(subtype, superBound, visited);
    }

    private boolean checkOrAdd(final AnnotatedTypeMirror subtype,final AnnotatedTypeMirror supertype,
                               final VisitHistory visited ) {
        if (visited.contains(subtype, supertype)) {
            return true;
        }

        visited.add(subtype, supertype);
        return false;
    }

}
