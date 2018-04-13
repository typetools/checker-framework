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
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Our //TODO: USING THE SAME LOGIC AS THE OLD TYPE_HIERARCHY.isSubtypeAsTypeArgument but only for
 * //TODO: RAW TypeArguments, we can replace this later if we think there is a more sensible thing
 * to do //TODO: LOOK AT OLD TYPE_HIERARCHY FOR MORE INFORAMTION
 */
public class DefaultRawnessComparer extends AbstractAtmComboVisitor<Boolean, SubtypeVisitHistory> {
    private final DefaultTypeHierarchy typeHierarchy;
    private AnnotationMirror currentTop;
    protected final SubtypeVisitHistory visitHistory;

    public DefaultRawnessComparer(final DefaultTypeHierarchy typeHierarchy) {
        this.typeHierarchy = typeHierarchy;
        this.visitHistory = new SubtypeVisitHistory();
    }

    @Override
    protected String defaultErrorMessage(
            AnnotatedTypeMirror subtype,
            AnnotatedTypeMirror supertype,
            SubtypeVisitHistory visited) {
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

    public boolean isValidInHierarchy(
            final AnnotatedTypeMirror subtype,
            final AnnotatedTypeMirror supertype,
            AnnotationMirror top) {
        AnnotationMirror prevTop = currentTop;
        this.currentTop = top;
        boolean result = AtmCombo.accept(subtype, supertype, visitHistory, this);
        this.currentTop = prevTop;
        return result;
    }

    // TODO: GENERAL CASE IF THE OTHERS HAVEN"T OCCURED SUCH AS DECLARED_DECLARED
    protected boolean arePrimaryAnnotationsEqual(
            final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype) {
        if (currentTop != null) {
            return AnnotationUtils.areSame(
                    subtype.getAnnotationInHierarchy(currentTop),
                    supertype.getAnnotationInHierarchy(currentTop));
        } else {
            return AnnotationUtils.areSame(subtype.getAnnotations(), supertype.getAnnotations());
        }
    }

    @Override
    public Boolean visitDeclared_Declared(
            AnnotatedDeclaredType subtype,
            AnnotatedDeclaredType supertype,
            SubtypeVisitHistory visited) {
        if (visited.contains(subtype, supertype, currentTop)) {
            return true;
        }

        if (!arePrimaryAnnotationsEqual(subtype, supertype)) {
            return false;
        }

        Boolean result =
                typeHierarchy.visitTypeArgs(
                        subtype, supertype, subtype.wasRaw(), supertype.wasRaw());
        visited.add(subtype, supertype, currentTop, result);
        return result;
    }

    @Override
    public Boolean visitDeclared_Null(
            AnnotatedDeclaredType subtype,
            AnnotatedNullType supertype,
            SubtypeVisitHistory visited) {
        if (visited.contains(subtype, supertype, currentTop)) {
            return true;
        }

        if (!arePrimaryAnnotationsEqual(subtype, supertype)) {
            return false;
        }

        Boolean result = !subtype.wasRaw();
        visited.add(subtype, supertype, currentTop, result);
        return result;
    }

    @Override
    public Boolean visitWildcard_Wildcard(
            AnnotatedWildcardType subtype,
            AnnotatedWildcardType supertype,
            SubtypeVisitHistory visited) {

        if (visited.contains(subtype, supertype, currentTop)) {
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

        if (visited.contains(subtypeUpper, supertypeUpper, currentTop)) {
            return true;
        }

        Boolean result = typeHierarchy.isSubtype(subtypeUpper, supertypeUpper, currentTop);

        visited.add(subtype, supertype, currentTop, result);
        visited.add(subtypeUpper, supertypeUpper, currentTop, result);
        return result;
    }

    @Override
    public Boolean visitArray_Array(
            AnnotatedArrayType subtype, AnnotatedArrayType supertype, SubtypeVisitHistory visited) {
        if (!arePrimaryAnnotationsEqual(subtype, supertype)) {
            return false;
        }
        return this.isValidInHierarchy(
                subtype.getComponentType(), supertype.getComponentType(), currentTop);
    }

    @Override
    public Boolean visitNull_Declared(
            AnnotatedNullType subtype,
            AnnotatedDeclaredType supertype,
            SubtypeVisitHistory visited) {
        if (visited.contains(subtype, supertype, currentTop)) {
            return true;
        }

        if (!arePrimaryAnnotationsEqual(subtype, supertype)) {
            return false;
        }

        Boolean result = !supertype.wasRaw();
        visited.add(subtype, supertype, currentTop, result);
        return result;
    }

    @Override
    public Boolean visitNull_Typevar(
            AnnotatedNullType subtype,
            AnnotatedTypeVariable supertype,
            SubtypeVisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitArray_Declared(
            AnnotatedArrayType subtype,
            AnnotatedDeclaredType supertype,
            SubtypeVisitHistory visited) {
        return arePrimaryAnnotationsEqual(subtype, supertype);
    }

    @Override
    public Boolean visitPrimitive_Primitive(
            AnnotatedPrimitiveType subtype,
            AnnotatedPrimitiveType supertype,
            SubtypeVisitHistory visited) {
        return arePrimaryAnnotationsEqual(subtype, supertype);
    }

    @Override
    public Boolean visitDeclared_Wildcard(
            AnnotatedDeclaredType subtype,
            AnnotatedWildcardType supertype,
            SubtypeVisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitArray_Wildcard(
            AnnotatedArrayType subtype,
            AnnotatedWildcardType supertype,
            SubtypeVisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Wildcard(
            AnnotatedTypeVariable subtype,
            AnnotatedWildcardType supertype,
            SubtypeVisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitDeclared_Typevar(
            AnnotatedDeclaredType subtype,
            AnnotatedTypeVariable supertype,
            SubtypeVisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitArray_Typevar(
            AnnotatedArrayType subtype,
            AnnotatedTypeVariable supertype,
            SubtypeVisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Declared(
            AnnotatedTypeVariable subtype,
            AnnotatedDeclaredType supertype,
            SubtypeVisitHistory visited) {
        return visitTypeVarSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Null(
            AnnotatedTypeVariable subtype,
            AnnotatedNullType supertype,
            SubtypeVisitHistory visited) {
        return visitTypeVarSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Typevar(
            AnnotatedTypeVariable subtype,
            AnnotatedTypeVariable supertype,
            SubtypeVisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    // See FromApac5Tests - iterator for when this would occur
    @Override
    public Boolean visitWildcard_Array(
            AnnotatedWildcardType subtype,
            AnnotatedArrayType supertype,
            SubtypeVisitHistory visited) {
        return arePrimaryAnnotationsEqual(subtype.getExtendsBound(), supertype);
    }

    @Override
    public Boolean visitWildcard_Declared(
            AnnotatedWildcardType subtype,
            AnnotatedDeclaredType supertype,
            SubtypeVisitHistory visited) {
        return arePrimaryAnnotationsEqual(subtype.getExtendsBound(), supertype);
    }

    @Override
    public Boolean visitWildcard_Typevar(
            AnnotatedWildcardType subtype,
            AnnotatedTypeVariable supertype,
            SubtypeVisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    public Boolean visitWildcardSupertype(
            AnnotatedTypeMirror subtype,
            AnnotatedWildcardType supertype,
            SubtypeVisitHistory visited) {
        if (visited.contains(subtype, supertype, currentTop)) {
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

        if (visited.contains(subtype, superExtendsBound, currentTop)) {
            return true;
        }

        Boolean result = this.visit(subtype, superExtendsBound, visited);
        visited.add(subtype, supertype, currentTop, result);
        visited.add(subtype, superExtendsBound, currentTop, result);
        return result;
    }

    public Boolean visitTypevarSupertype(
            AnnotatedTypeMirror subtype,
            AnnotatedTypeVariable supertype,
            SubtypeVisitHistory visited) {
        if (visited.contains(subtype, supertype, currentTop)) {
            return true;
        }

        final AnnotatedTypeMirror supertypeUb = supertype.getUpperBound();
        if (visited.contains(subtype, supertypeUb, currentTop)) {
            return true;
        }

        Boolean result = this.visit(subtype, supertypeUb, visited);
        visited.add(subtype, supertype, currentTop, result);
        visited.add(subtype, supertypeUb, currentTop, result);
        return result;
    }

    public Boolean visitTypeVarSubtype(
            AnnotatedTypeVariable subtype,
            AnnotatedTypeMirror supertype,
            SubtypeVisitHistory visited) {
        if (visited.contains(subtype, supertype, currentTop)) {
            return true;
        }

        final AnnotatedTypeMirror subtypeUb = subtype.getUpperBound();
        if (visited.contains(subtypeUb, supertype, currentTop)) {
            return true;
        }

        // Needed to prevent infinite recursion e.g. in
        // tests/all-systems/java8inference/Issue1815.java
        visited.add(subtype, supertype, currentTop, true);

        Boolean result = this.visit(subtypeUb, supertype, visited);
        visited.add(subtype, supertype, currentTop, result);
        visited.add(subtypeUb, supertype, currentTop, result);
        return result;
    }
}
