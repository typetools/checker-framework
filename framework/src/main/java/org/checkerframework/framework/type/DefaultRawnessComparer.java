package org.checkerframework.framework.type;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.type.visitor.VisitHistory;
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Our //TODO: USING THE SAME LOGIC AS THE OLD TYPE_HIERARCHY.isSubtypeAsTypeArgument but only for
 * //TODO: RAW TypeArguments, we can replace this later if we think there is a more sensible thing
 * to do //TODO: LOOK AT OLD TYPE_HIERARCHY FOR MORE INFORAMTION
 */
public class DefaultRawnessComparer extends AbstractAtmComboVisitor<Boolean, VisitHistory> {
    private final DefaultTypeHierarchy typeHierarchy;
    private AnnotationMirror currentTop;

    public DefaultRawnessComparer(final DefaultTypeHierarchy typeHierarchy) {
        this.typeHierarchy = typeHierarchy;
    }

    @Override
    protected String defaultErrorMessage(
            AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, VisitHistory visited) {
        return "DefaultRawnessComparer: Unexpected AnnotatedTypeMirror combination.\n"
                + "type1 = "
                + subtype.getClass().getSimpleName()
                + "( "
                + subtype
                + " )\n"
                + "type2 = "
                + supertype.getClass().getSimpleName()
                + "( "
                + supertype
                + " )\n"
                + "visitHistory = "
                + visited;
    }

    public boolean isValid(
            final AnnotatedTypeMirror subtype,
            final AnnotatedTypeMirror supertype,
            VisitHistory visited) {
        return AtmCombo.accept(subtype, supertype, visited, this);
    }

    public boolean isValidInHierarchy(
            final AnnotatedTypeMirror subtype,
            final AnnotatedTypeMirror supertype,
            AnnotationMirror top,
            VisitHistory visited) {
        this.currentTop = top;
        boolean result = AtmCombo.accept(subtype, supertype, visited, this);
        this.currentTop = null;
        return result;
    }

    // TODO: GENERAL CASE IF THE OTHERS HAVEN"T OCCURED SUCH AS DECLARED_DECLARED
    protected boolean arePrimaryAnnotationsEqual(
            final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype) {
        if (currentTop != null) {
            return AnnotationUtils.areSame(
                    subtype.getAnnotationInHierarchy(currentTop),
                    supertype.getAnnotationInHierarchy(currentTop));
        } // else

        return AnnotationUtils.areSame(subtype.getAnnotations(), supertype.getAnnotations());
    }

    @Override
    public Boolean visitDeclared_Declared(
            AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        if (checkOrAdd(subtype, supertype, visited)) {
            return true;
        }

        if (!arePrimaryAnnotationsEqual(subtype, supertype)) {
            return false;
        }

        return typeHierarchy.visitTypeArgs(
                subtype, supertype, visited, subtype.wasRaw(), supertype.wasRaw());
    }

    @Override
    public Boolean visitWildcard_Wildcard(
            AnnotatedWildcardType subtype, AnnotatedWildcardType supertype, VisitHistory visited) {

        if (checkOrAdd(subtype, supertype, visited)) {
            return true;
        }

        if (supertype.getExtendsBoundField() == null
                || supertype.getExtendsBoundField().getAnnotations().isEmpty()) {
            // TODO: the LHS extends bound hasn't been unfolded or defaulted.
            // Stop looking, we should be fine.
            // See tests/nullness/generics/WildcardSubtyping.java
            return true;
        }

        AnnotatedTypeMirror subtypeUpper = subtype.getExtendsBound();
        AnnotatedTypeMirror supertypeUpper = supertype.getExtendsBound();

        if (supertypeUpper.getKind() == TypeKind.TYPEVAR
                && TypesUtils.isCaptured((TypeVariable) supertypeUpper.getUnderlyingType())) {
            supertypeUpper = ((AnnotatedTypeVariable) supertypeUpper).getUpperBound();
        }

        if (checkOrAdd(subtypeUpper, supertypeUpper, visited)) {
            return true;
        }

        if (currentTop == null) {
            return typeHierarchy.isSubtype(subtypeUpper, supertypeUpper);
        }

        return typeHierarchy.isSubtype(subtypeUpper, supertypeUpper, currentTop);
    }

    @Override
    public Boolean visitArray_Array(
            AnnotatedArrayType subtype, AnnotatedArrayType supertype, VisitHistory visited) {
        if (!arePrimaryAnnotationsEqual(subtype, supertype)) {
            return false;
        }
        return this.isValid(subtype.getComponentType(), supertype.getComponentType(), visited);
    }

    @Override
    public Boolean visitNull_Declared(
            AnnotatedNullType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        if (checkOrAdd(subtype, supertype, visited)) {
            return true;
        }

        if (!arePrimaryAnnotationsEqual(subtype, supertype)) {
            return false;
        }

        return !supertype.wasRaw();
    }

    @Override
    public Boolean visitNull_Typevar(
            AnnotatedNullType subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitArray_Declared(
            AnnotatedArrayType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        return arePrimaryAnnotationsEqual(subtype, supertype);
    }

    @Override
    public Boolean visitPrimitive_Primitive(
            AnnotatedPrimitiveType subtype,
            AnnotatedPrimitiveType supertype,
            VisitHistory visited) {
        return arePrimaryAnnotationsEqual(subtype, supertype);
    }

    @Override
    public Boolean visitDeclared_Wildcard(
            AnnotatedDeclaredType subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitArray_Wildcard(
            AnnotatedArrayType subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Wildcard(
            AnnotatedTypeVariable subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitDeclared_Typevar(
            AnnotatedDeclaredType subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitArray_Typevar(
            AnnotatedArrayType subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Declared(
            AnnotatedTypeVariable subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        return visitTypeVarSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Null(
            AnnotatedTypeVariable subtype, AnnotatedNullType supertype, VisitHistory visited) {
        return visitTypeVarSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Typevar(
            AnnotatedTypeVariable subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    // See FromApac5Tests - iterator for when this would occur
    @Override
    public Boolean visitWildcard_Array(
            AnnotatedWildcardType subtype, AnnotatedArrayType supertype, VisitHistory visited) {
        return arePrimaryAnnotationsEqual(subtype.getExtendsBound(), supertype);
    }

    @Override
    public Boolean visitWildcard_Declared(
            AnnotatedWildcardType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        return arePrimaryAnnotationsEqual(subtype.getExtendsBound(), supertype);
    }

    @Override
    public Boolean visitWildcard_Typevar(
            AnnotatedWildcardType subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    public Boolean visitWildcardSupertype(
            AnnotatedTypeMirror subtype, AnnotatedWildcardType supertype, VisitHistory visited) {

        if (checkOrAdd(subtype, supertype, visited)) {
            return true;
        }

        if (!supertype.getAnnotations().isEmpty()
                && !supertype.getEffectiveAnnotations().equals(subtype.getEffectiveAnnotations())) {
            return false;
        }

        final AnnotatedTypeMirror superExtendsBound = supertype.getExtendsBound();
        if (superExtendsBound == null) {
            return true;
        }

        if (checkOrAdd(subtype, superExtendsBound, visited)) {
            return true;
        }

        return this.visit(subtype, superExtendsBound, visited);
    }

    public Boolean visitTypevarSupertype(
            AnnotatedTypeMirror subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        if (checkOrAdd(subtype, supertype, visited)) {
            return true;
        }

        final AnnotatedTypeMirror supertypeUb = supertype.getUpperBound();
        if (checkOrAdd(subtype, supertypeUb, visited)) {
            return true;
        }

        return this.visit(subtype, supertypeUb, visited);
    }

    public Boolean visitTypeVarSubtype(
            AnnotatedTypeVariable subtype, AnnotatedTypeMirror supertype, VisitHistory visited) {
        if (checkOrAdd(subtype, supertype, visited)) {
            return true;
        }

        final AnnotatedTypeMirror subtypeUb = subtype.getUpperBound();
        if (checkOrAdd(subtypeUb, supertype, visited)) {
            return true;
        }

        return this.visit(subtype, subtypeUb, visited);
    }

    private boolean checkOrAdd(
            final AnnotatedTypeMirror subtype,
            final AnnotatedTypeMirror supertype,
            final VisitHistory visited) {
        if (visited.contains(subtype, supertype)) {
            return true;
        }

        visited.add(subtype, supertype);
        return false;
    }
}
