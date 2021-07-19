package org.checkerframework.framework.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Helper class to compute the least upper bound of two AnnotatedTypeMirrors.
 *
 * <p>This class should only be used by {@link AnnotatedTypes#leastUpperBound(AnnotatedTypeFactory,
 * AnnotatedTypeMirror, AnnotatedTypeMirror)}.
 */
class AtmLubVisitor extends AbstractAtmComboVisitor<Void, AnnotatedTypeMirror> {

    private final AnnotatedTypeFactory atypeFactory;
    private final QualifierHierarchy qualifierHierarchy;
    /**
     * List of {@link AnnotatedTypeVariable} or {@link AnnotatedWildcardType} that have been
     * visited. Call {@link #visited(AnnotatedTypeMirror)} to check if the type have been visited,
     * so that reference equality is used rather than {@link #equals(Object)}.
     */
    private final List<AnnotatedTypeMirror> visited = new ArrayList<>();

    AtmLubVisitor(AnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
        this.qualifierHierarchy = atypeFactory.getQualifierHierarchy();
    }

    /**
     * Returns an ATM that is the least upper bound of type1 and type2 and whose Java type is
     * lubJavaType. lubJavaType must be a super type or convertible to the Java types of type1 and
     * type2.
     */
    AnnotatedTypeMirror lub(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, TypeMirror lubJavaType) {
        AnnotatedTypeMirror lub = AnnotatedTypeMirror.createType(lubJavaType, atypeFactory, false);

        if (type1.getKind() == TypeKind.NULL) {
            return lubWithNull((AnnotatedNullType) type1, type2, lub);
        }
        if (type2.getKind() == TypeKind.NULL) {
            return lubWithNull((AnnotatedNullType) type2, type1, lub);
        }

        AnnotatedTypeMirror type1AsLub = AnnotatedTypes.asSuper(atypeFactory, type1, lub);
        AnnotatedTypeMirror type2AsLub = AnnotatedTypes.asSuper(atypeFactory, type2, lub);

        visit(type1AsLub, type2AsLub, lub);
        visited.clear();
        return lub;
    }

    private AnnotatedTypeMirror lubWithNull(
            AnnotatedNullType nullType, AnnotatedTypeMirror otherType, AnnotatedTypeMirror lub) {
        AnnotatedTypeMirror otherAsLub;
        if (otherType.getKind() == TypeKind.NULL) {
            otherAsLub = otherType.deepCopy();
        } else {
            otherAsLub = AnnotatedTypes.asSuper(atypeFactory, otherType, lub);
        }

        lub = otherAsLub.deepCopy();

        if (otherAsLub.getKind() != TypeKind.TYPEVAR && otherAsLub.getKind() != TypeKind.WILDCARD) {
            for (AnnotationMirror nullAnno : nullType.getAnnotations()) {
                AnnotationMirror otherAnno = otherAsLub.getAnnotationInHierarchy(nullAnno);
                AnnotationMirror lubAnno = qualifierHierarchy.leastUpperBound(nullAnno, otherAnno);
                lub.replaceAnnotation(lubAnno);
            }
            return lub;
        }

        // LUB(@N null, T), where T's upper bound is @U and T's lower bound is @L
        // if @L <: @U <: @N then LUB(@N null, T) = @N T
        // if @L <: @N <:@U && @N != @L  then LUB(@N null, T) = @U T
        // if @N <: @L <: @U             then LUB(@N null, T) =    T
        Set<AnnotationMirror> lowerBounds =
                AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualifierHierarchy, otherAsLub);
        for (AnnotationMirror lowerBound : lowerBounds) {
            AnnotationMirror nullAnno = nullType.getAnnotationInHierarchy(lowerBound);
            AnnotationMirror upperBound = otherAsLub.getEffectiveAnnotationInHierarchy(lowerBound);
            if (qualifierHierarchy.isSubtype(upperBound, nullAnno)) {
                // @L <: @U <: @N
                lub.replaceAnnotation(nullAnno);
            } else if (qualifierHierarchy.isSubtype(lowerBound, nullAnno)
                    && !qualifierHierarchy.isSubtype(nullAnno, lowerBound)) {
                // @L <: @N <:@U && @N != @L
                lub.replaceAnnotation(upperBound);
            } // else @N <: @L <: @U
        }
        return lub;
    }

    /**
     * Replaces the primary annotations of lub with the lub of the primary annotations of type1 and
     * type2.
     */
    private void lubPrimaryAnnotations(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotatedTypeMirror lub) {
        Set<? extends AnnotationMirror> lubSet;
        if (type1.getAnnotations().isEmpty()) {
            lubSet = type2.getAnnotations();
        } else if (type2.getAnnotations().isEmpty()) {
            lubSet = type1.getAnnotations();
        } else {
            lubSet =
                    qualifierHierarchy.leastUpperBounds(
                            type1.getAnnotations(), type2.getAnnotations());
        }
        lub.replaceAnnotations(lubSet);
    }

    /** Casts lub to the type of type and issues an error if type and lub are not the same kind. */
    private <T extends AnnotatedTypeMirror> T castLub(T type, AnnotatedTypeMirror lub) {
        if (type.getKind() != lub.getKind()) {
            throw new BugInCF(
                    "AtmLubVisitor: unexpected type. Found: %s Required %s",
                    lub.getKind(), type.getKind());
        }
        @SuppressWarnings("unchecked")
        T castedLub = (T) lub;
        return castedLub;
    }

    @Override
    public Void visitNull_Null(
            AnnotatedNullType type1, AnnotatedNullType type2, AnnotatedTypeMirror lub) {
        // Called to issue warning
        castLub(type1, lub);
        lubPrimaryAnnotations(type1, type2, lub);
        return null;
    }

    @Override
    public Void visitArray_Array(
            AnnotatedArrayType type1, AnnotatedArrayType type2, AnnotatedTypeMirror lub) {
        AnnotatedArrayType lubArray = castLub(type1, lub);
        lubPrimaryAnnotations(type1, type2, lubArray);

        visit(type1.getComponentType(), type2.getComponentType(), lubArray.getComponentType());
        return null;
    }

    @Override
    public Void visitDeclared_Declared(
            AnnotatedDeclaredType type1, AnnotatedDeclaredType type2, AnnotatedTypeMirror lub) {
        AnnotatedDeclaredType castedLub = castLub(type1, lub);

        lubPrimaryAnnotations(type1, type2, lub);

        if (lub.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType enclosingLub = ((AnnotatedDeclaredType) lub).getEnclosingType();
            AnnotatedDeclaredType enclosing1 = type1.getEnclosingType();
            AnnotatedDeclaredType enclosing2 = type2.getEnclosingType();
            if (enclosingLub != null && enclosing1 != null && enclosing2 != null) {
                visitDeclared_Declared(enclosing1, enclosing2, enclosingLub);
            }
        }

        for (int i = 0; i < type1.getTypeArguments().size(); i++) {
            AnnotatedTypeMirror type1TypeArg = type1.getTypeArguments().get(i);
            AnnotatedTypeMirror type2TypeArg = type2.getTypeArguments().get(i);
            AnnotatedTypeMirror lubTypeArg = castedLub.getTypeArguments().get(i);
            lubTypeArgument(type1TypeArg, type2TypeArg, lubTypeArg);
        }
        return null;
    }

    private void lubTypeArgument(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotatedTypeMirror lub) {
        // In lub(), asSuper is called on type1 and type2, but asSuper does not recur into type
        // arguments, so call asSuper on the type arguments so that they have the same underlying
        // type.
        final AnnotatedTypeMirror type1AsLub = AnnotatedTypes.asSuper(atypeFactory, type1, lub);
        final AnnotatedTypeMirror type2AsLub = AnnotatedTypes.asSuper(atypeFactory, type2, lub);

        // If the type argument is a wildcard or captured wildcard, then the lub computation is
        // slightly different.  The primary annotation on the lower bound is the glb of lower bounds
        // of the type types.  This is because the lub of Gen<@A ? extends @A Object> and Gen<@B ?
        // extends @A Object> is Gen<@B ? extends @A Object>.  If visit(type1AsLub, type2AsLub, lub)
        // was called instead of the below code, then the lub would be Gen<@A ? extends @A Object>.
        // (Note the lub of Gen<@A ? super @A Object> and Gen<@A ? super @B Object> does not exist,
        // but Gen<@A ? super @B Object> is returned.)
        if (lub.getKind() == TypeKind.WILDCARD) {
            if (visited(lub)) {
                return;
            }
            AnnotatedWildcardType type1Wildcard = (AnnotatedWildcardType) type1AsLub;
            AnnotatedWildcardType type2Wildcard = (AnnotatedWildcardType) type2AsLub;
            AnnotatedWildcardType lubWildcard = (AnnotatedWildcardType) lub;
            if (type1Wildcard.isUninferredTypeArgument()
                    || type2Wildcard.isUninferredTypeArgument()) {
                lubWildcard.setUninferredTypeArgument();
            }
            lubWildcard(
                    type1Wildcard.getSuperBound(),
                    type1Wildcard.getExtendsBound(),
                    type2Wildcard.getSuperBound(),
                    type2Wildcard.getExtendsBound(),
                    lubWildcard.getSuperBound(),
                    lubWildcard.getExtendsBound());
        } else if (lub.getKind() == TypeKind.TYPEVAR
                && TypesUtils.isCaptured((TypeVariable) lub.getUnderlyingType())) {
            if (visited(lub)) {
                return;
            }
            AnnotatedTypeVariable type1typevar = (AnnotatedTypeVariable) type1AsLub;
            AnnotatedTypeVariable type2typevar = (AnnotatedTypeVariable) type2AsLub;
            AnnotatedTypeVariable lubTypevar = (AnnotatedTypeVariable) lub;
            lubWildcard(
                    type1typevar.getLowerBound(),
                    type1typevar.getUpperBound(),
                    type2typevar.getLowerBound(),
                    type2typevar.getUpperBound(),
                    lubTypevar.getLowerBound(),
                    lubTypevar.getUpperBound());
        } else {
            // Don't add to visit history because that will happen in visitTypevar_Typevar or
            // visitWildcard_Wildcard if needed.
            visit(type1AsLub, type2AsLub, lub);
        }
    }

    private void lubWildcard(
            AnnotatedTypeMirror type1LowerBound,
            AnnotatedTypeMirror type1UpperBound,
            AnnotatedTypeMirror type2LowerBound,
            AnnotatedTypeMirror type2UpperBound,
            AnnotatedTypeMirror lubLowerBound,
            AnnotatedTypeMirror lubUpperBound) {
        visit(type1UpperBound, type2UpperBound, lubUpperBound);
        visit(type1LowerBound, type2LowerBound, lubLowerBound);

        for (AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
            AnnotationMirror anno1 = type1LowerBound.getAnnotationInHierarchy(top);
            AnnotationMirror anno2 = type2LowerBound.getAnnotationInHierarchy(top);

            if (anno1 != null && anno2 != null) {
                AnnotationMirror glb = qualifierHierarchy.greatestLowerBound(anno1, anno2);
                lubLowerBound.replaceAnnotation(glb);
            }
        }
    }

    @Override
    public Void visitPrimitive_Primitive(
            AnnotatedPrimitiveType type1, AnnotatedPrimitiveType type2, AnnotatedTypeMirror lub) {
        // Called to issue warning
        castLub(type1, lub);
        lubPrimaryAnnotations(type1, type2, lub);
        return null;
    }

    @Override
    public Void visitTypevar_Typevar(
            AnnotatedTypeVariable type1, AnnotatedTypeVariable type2, AnnotatedTypeMirror lub1) {
        if (visited(lub1)) {
            return null;
        }

        AnnotatedTypeVariable lub = castLub(type1, lub1);
        visit(type1.getUpperBound(), type2.getUpperBound(), lub.getUpperBound());
        visit(type1.getLowerBound(), type2.getLowerBound(), lub.getLowerBound());

        lubPrimaryOnBoundedType(type1, type2, lub);

        return null;
    }

    @Override
    public Void visitWildcard_Wildcard(
            AnnotatedWildcardType type1, AnnotatedWildcardType type2, AnnotatedTypeMirror lub1) {
        if (visited(lub1)) {
            return null;
        }
        AnnotatedWildcardType lub = castLub(type1, lub1);
        visit(type1.getExtendsBound(), type2.getExtendsBound(), lub.getExtendsBound());
        visit(type1.getSuperBound(), type2.getSuperBound(), lub.getSuperBound());
        lubPrimaryOnBoundedType(type1, type2, lub);

        return null;
    }

    private void lubPrimaryOnBoundedType(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotatedTypeMirror lub) {
        // For each hierarchy, if type1 is not a subtype of type2 and type2 is not a
        // subtype of type1, then the primary annotation on lub must be the effective upper
        // bound of type1 or type2, whichever is higher.
        Set<AnnotationMirror> type1LowerBoundAnnos =
                AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualifierHierarchy, type1);
        Set<AnnotationMirror> type2LowerBoundAnnos =
                AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualifierHierarchy, type2);

        for (AnnotationMirror lower1 : type1LowerBoundAnnos) {
            AnnotationMirror top = qualifierHierarchy.getTopAnnotation(lower1);

            // Can't just call isSubtype because it will return false if bounds have
            // different annotations on component types
            AnnotationMirror lower2 =
                    qualifierHierarchy.findAnnotationInHierarchy(type2LowerBoundAnnos, top);
            AnnotationMirror upper1 = type1.getEffectiveAnnotationInHierarchy(lower1);
            AnnotationMirror upper2 = type2.getEffectiveAnnotationInHierarchy(lower1);

            if (qualifierHierarchy.isSubtype(upper2, upper1)
                    && qualifierHierarchy.isSubtype(upper1, upper2)
                    && qualifierHierarchy.isSubtype(lower1, lower2)
                    && qualifierHierarchy.isSubtype(lower2, lower1)) {
                continue;
            }

            if (!qualifierHierarchy.isSubtype(upper2, lower1)
                    && !qualifierHierarchy.isSubtype(upper1, lower2)) {
                lub.replaceAnnotation(qualifierHierarchy.leastUpperBound(upper1, upper2));
            }
        }
    }

    @Override
    public Void visitIntersection_Intersection(
            AnnotatedIntersectionType type1,
            AnnotatedIntersectionType type2,
            AnnotatedTypeMirror lub) {
        AnnotatedIntersectionType castedLub = castLub(type1, lub);
        lubPrimaryAnnotations(type1, type2, lub);

        for (int i = 0; i < castedLub.getBounds().size(); i++) {
            AnnotatedTypeMirror lubST = castedLub.getBounds().get(i);
            visit(type1.getBounds().get(i), type2.getBounds().get(i), lubST);
        }

        return null;
    }

    @Override
    public Void visitUnion_Union(
            AnnotatedUnionType type1, AnnotatedUnionType type2, AnnotatedTypeMirror lub) {
        AnnotatedUnionType castedLub = castLub(type1, lub);
        lubPrimaryAnnotations(type1, type2, lub);

        for (int i = 0; i < castedLub.getAlternatives().size(); i++) {
            AnnotatedDeclaredType lubAltern = castedLub.getAlternatives().get(i);
            visit(type1.getAlternatives().get(i), type2.getAlternatives().get(i), lubAltern);
        }
        return null;
    }

    @Override
    protected String defaultErrorMessage(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotatedTypeMirror lub) {
        return String.format(
                "AtmLubVisitor: Unexpected combination: type1: %s type2: %s.%n"
                        + "type1: %s%ntype2: %s%nlub: %s",
                type1.getKind(), type2.getKind(), type1, type2, lub);
    }

    /**
     * Returns true if the {@link AnnotatedTypeMirror} has been visited. If it has not, then it is
     * added to the list of visited AnnotatedTypeMirrors. This prevents infinite recursion on
     * recursive types.
     *
     * @param atm the type that might have been visited
     * @return true if the given type has been visited
     */
    private boolean visited(@FindDistinct AnnotatedTypeMirror atm) {
        for (AnnotatedTypeMirror atmVisit : visited) {
            // Use reference equality rather than equals because the visitor may visit two types
            // that are structurally equal, but not actually the same.  For example, the
            // wildcards in Pair<?,?> may be equal, but they both should be visited.
            if (atmVisit == atm) {
                return true;
            }
        }
        visited.add(atm);
        return false;
    }
}
