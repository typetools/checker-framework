package org.checkerframework.framework.util.typeinference.constraint;

import java.util.List;
import java.util.Set;
import javax.lang.model.type.TypeKind;
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
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.PluginUtil;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Takes a single step in reducing a AFConstraint.
 *
 * <p>The visit method will determine if the given constraint should either:
 *
 * <ul>
 *   <li>be discarded - in this case, the visitor just returns
 *   <li>reduced to a simpler constraint or set of constraints - in this case, the new constraint or
 *       set of constraints is added to newConstraints
 * </ul>
 *
 * Sprinkled throughout this class are comments of the form:
 *
 * <pre>{@code
 * // If F has the form G<..., Yk-1, ? super U, Yk+1, ...>, where U involves Tj
 * }</pre>
 *
 * These are excerpts from the JLS, if you search for them you will find the corresponding JLS
 * description of the case being covered.
 */
abstract class AFReducingVisitor extends AbstractAtmComboVisitor<Void, Set<AFConstraint>> {

    public final Class<? extends AFConstraint> reducerType;
    public final AnnotatedTypeFactory typeFactory;

    public AFReducingVisitor(
            final Class<? extends AFConstraint> reducerType,
            final AnnotatedTypeFactory typeFactory) {
        this.reducerType = reducerType;
        this.typeFactory = typeFactory;
    }

    public abstract AFConstraint makeConstraint(
            AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype);

    public abstract AFConstraint makeInverseConstraint(
            AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype);

    public abstract AFConstraint makeEqualityConstraint(
            AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype);

    public void addConstraint(
            AnnotatedTypeMirror subtype,
            AnnotatedTypeMirror supertype,
            Set<AFConstraint> constraints) {
        constraints.add(makeConstraint(subtype, supertype));
    }

    public void addInverseConstraint(
            AnnotatedTypeMirror subtype,
            AnnotatedTypeMirror supertype,
            Set<AFConstraint> constraints) {
        constraints.add(makeInverseConstraint(subtype, supertype));
    }

    public void addEqualityConstraint(
            AnnotatedTypeMirror subtype,
            AnnotatedTypeMirror supertype,
            Set<AFConstraint> constraints) {
        constraints.add(makeEqualityConstraint(subtype, supertype));
    }

    /**
     * Called when we encounter an AF constraint on a type combination that we did not think is
     * possible. This either implies that the type combination is possible, we accidentally created
     * an invalid A2F or F2A Constraint, or we called the visit method on two AnnotatedTypeMirrors
     * that do not appear together in a constraint.
     */
    @Override
    protected String defaultErrorMessage(
            AnnotatedTypeMirror subtype,
            AnnotatedTypeMirror supertype,
            Set<AFConstraint> constraints) {
        return "Unexpected "
                + reducerType.getSimpleName()
                + " + Combination:\n"
                + "subtype="
                + subtype
                + "\n"
                + "supertype="
                + supertype
                + "\n"
                + "constraints=[\n"
                + PluginUtil.join(", ", constraints)
                + "\n]";
    }

    // ------------------------------------------------------------------------
    // Arrays as arguments
    // From the JLS:
    //    If F = U[], where the type U involves Tj, then if A is an array type V[], or a type
    //    variable with an upper bound that is an array type V[], where V is a reference type, this
    //    algorithm is applied recursively to the constraint V << U or U << V (depending on the
    //    constraint type).

    @Override
    public Void visitArray_Array(
            AnnotatedArrayType subtype,
            AnnotatedArrayType supertype,
            Set<AFConstraint> constraints) {
        addConstraint(subtype.getComponentType(), supertype.getComponentType(), constraints);
        return null;
    }

    @Override
    public Void visitArray_Declared(
            AnnotatedArrayType subtype,
            AnnotatedDeclaredType supertype,
            Set<AFConstraint> constraints) {
        return null;
    }

    @Override
    public Void visitArray_Null(
            AnnotatedArrayType subtype,
            AnnotatedNullType supertype,
            Set<AFConstraint> constraints) {
        return null;
    }

    @Override
    public Void visitArray_Wildcard(
            AnnotatedArrayType subtype,
            AnnotatedWildcardType supertype,
            Set<AFConstraint> constraints) {
        visitWildcardAsSuperType(subtype, supertype, constraints);
        return null;
    }

    // Despite the above the comment at the beginning of the "array as arguments" section, a type
    // variable cannot actually have an array type as its upper bound (e.g. <T extends Integer[]> is
    // not allowed).
    // So the only cases in which we visitArray_Typevar would be cases in which either:
    //   1) Typevar is a type parameter for which we are inferring an argument, in which case the
    //      combination is already irreducible and we would not pass it to this class.
    //   2) Typevar is an outer scope type variable, in which case it could NOT reference any of the
    //      type parameters for which we are inferring arguments and therefore will not lead to any
    //      meaningful AFConstraints.
    // public void visitArray_Typevar

    // ------------------------------------------------------------------------
    // Declared as argument

    /**
     * I believe there should be only 1 way to have a constraint of this form: {@code visit
     * (Array<T>, T [])} At this point, I don't think that's a valid argument for a formal
     * parameter. If this occurs it is because of idiosyncrasies with the Checker Framework . We're
     * going to skip this case for now.
     */
    @Override
    public Void visitDeclared_Array(
            AnnotatedDeclaredType subtype,
            AnnotatedArrayType supertype,
            Set<AFConstraint> constraints) {
        return null;
    }

    // From the JLS Spec:
    //  If F has the form G<..., Yk-1,U, Yk+1, ...>, where U involves Tj
    @Override
    public Void visitDeclared_Declared(
            AnnotatedDeclaredType subtype,
            AnnotatedDeclaredType supertype,
            Set<AFConstraint> constraints) {
        if (subtype.wasRaw() || supertype.wasRaw()) {
            // The error will be caught in {@link DefaultTypeArgumentInference#infer} and
            // inference will be aborted, but type-checking will continue.
            throw new BugInCF("Can't infer type arguments when raw types are involved.");
        }

        if (!TypesUtils.isErasedSubtype(
                subtype.getUnderlyingType(),
                supertype.getUnderlyingType(),
                typeFactory.getContext().getTypeUtils())) {
            return null;
        }
        AnnotatedDeclaredType subAsSuper =
                AnnotatedTypes.castedAsSuper(typeFactory, subtype, supertype);

        final List<AnnotatedTypeMirror> subTypeArgs = subAsSuper.getTypeArguments();
        final List<AnnotatedTypeMirror> superTypeArgs = supertype.getTypeArguments();
        for (int i = 0; i < subTypeArgs.size(); i++) {
            final AnnotatedTypeMirror subTypeArg = subTypeArgs.get(i);
            final AnnotatedTypeMirror superTypeArg = superTypeArgs.get(i);

            // If F has the form G<..., Yk-1, ? extends U, Yk+1, ...>, where U involves Tj
            // If F has the form G<..., Yk-1, ? super U, Yk+1, ...>, where U involves Tj
            // Since we always have both bounds in the checker framework we always compare both
            if (superTypeArg.getKind() == TypeKind.WILDCARD) {
                final AnnotatedWildcardType superWc = (AnnotatedWildcardType) superTypeArg;

                if (subTypeArg.getKind() == TypeKind.WILDCARD) {
                    final AnnotatedWildcardType subWc = (AnnotatedWildcardType) subTypeArg;
                    TypeArgInferenceUtil.checkForUninferredTypes(subWc);
                    addConstraint(subWc.getExtendsBound(), superWc.getExtendsBound(), constraints);
                    addInverseConstraint(
                            superWc.getSuperBound(), subWc.getSuperBound(), constraints);
                } else {
                    addConstraint(subTypeArg, superWc.getExtendsBound(), constraints);
                    addInverseConstraint(superWc.getSuperBound(), subTypeArg, constraints);
                }

            } else {
                // if F has the form G<..., Yk-1, U, Yk+1, ...>, where U is a type expression that
                // involves Tj
                addEqualityConstraint(subTypeArg, superTypeArg, constraints);
            }
        }

        return null;
    }

    @Override
    public Void visitDeclared_Intersection(
            AnnotatedDeclaredType subtype,
            AnnotatedIntersectionType supertype,
            Set<AFConstraint> constraints) {

        // Note: AnnotatedIntersectionTypes cannot have a type variable as one of the direct
        // parameters but a type variable may be the type subtype to an intersection bound <e.g.  <T
        // extends Serializable & Iterable<T>>
        for (final AnnotatedTypeMirror intersectionBound : supertype.directSuperTypes()) {
            if (intersectionBound instanceof AnnotatedDeclaredType
                    && !((AnnotatedDeclaredType) intersectionBound).getTypeArguments().isEmpty()) {
                addConstraint(subtype, supertype, constraints);
            }
        }

        return null;
    }

    // Remember that NULL types can come from lower bounds
    @Override
    public Void visitDeclared_Null(
            AnnotatedDeclaredType subtype,
            AnnotatedNullType supertype,
            Set<AFConstraint> constraints) {
        return null;
    }

    // a primitive supertype provides us no information on the type of any type parameters for that
    // method
    @Override
    public Void visitDeclared_Primitive(
            AnnotatedDeclaredType subtype,
            AnnotatedPrimitiveType supertype,
            Set<AFConstraint> constraints) {
        return null;
    }

    @Override
    public Void visitDeclared_Typevar(
            AnnotatedDeclaredType subtype,
            AnnotatedTypeVariable supertype,
            Set<AFConstraint> constraints) {
        // Note: We expect the A2F constraints where F == a targeted type supertype to already be
        // removed.  Therefore, supertype should NOT be a target.
        addConstraint(subtype, supertype, constraints);
        return null;
    }

    @Override
    public Void visitDeclared_Union(
            AnnotatedDeclaredType subtype,
            AnnotatedUnionType supertype,
            Set<AFConstraint> constraints) {
        return null; // TODO: NOT SUPPORTED AT THE MOMENT
    }

    @Override
    public Void visitDeclared_Wildcard(
            AnnotatedDeclaredType subtype,
            AnnotatedWildcardType supertype,
            Set<AFConstraint> constraints) {
        visitWildcardAsSuperType(subtype, supertype, constraints);
        return null;
    }

    // ------------------------------------------------------------------------
    // Intersection as subtype
    @Override
    public Void visitIntersection_Declared(
            AnnotatedIntersectionType subtype,
            AnnotatedDeclaredType supertype,
            Set<AFConstraint> constraints) {

        // at least one of the intersection bound types must be convertible to the param type
        final AnnotatedDeclaredType subtypeAsParam =
                AnnotatedTypes.castedAsSuper(typeFactory, subtype, supertype);
        if (subtypeAsParam != null && !subtypeAsParam.equals(subtype)) {
            addConstraint(subtypeAsParam, supertype, constraints);
        }

        return null;
    }

    @Override
    public Void visitIntersection_Intersection(
            AnnotatedIntersectionType argument,
            AnnotatedIntersectionType parameter,
            Set<AFConstraint> constraints) {
        return null; // TODO: NOT SUPPORTED AT THE MOMENT
    }

    // provides no information as the AnnotatedNullType cannot refer to a type parameter
    @Override
    public Void visitIntersection_Null(
            AnnotatedIntersectionType argument,
            AnnotatedNullType parameter,
            Set<AFConstraint> constraints) {
        return null;
    }

    // ------------------------------------------------------------------------
    // Null as argument

    /**
     * NULL types only have primary annotations. A type parameter could only appear as a component
     * of the parameter type and therefore has no relationship to these primary annotations
     */
    @Override
    public Void visitNull_Array(
            AnnotatedNullType argument,
            AnnotatedArrayType parameter,
            Set<AFConstraint> constraints) {
        return null;
    }

    /**
     * NULL types only have primary annotations. A type parameter could only appear as a component
     * of the parameter type and therefore has no relationship to these primary annotations
     */
    @Override
    public Void visitNull_Declared(
            AnnotatedNullType argument,
            AnnotatedDeclaredType parameter,
            Set<AFConstraint> constraints) {
        return null;
    }

    /**
     * TODO: PERHAPS FOR ALL OF THESE WHERE WE COMPARE AGAINST THE LOWER BOUND, WE SHOULD INSTEAD
     * COMPARE TODO: against the UPPER_BOUND with the LOWER_BOUND's PRIMARY ANNOTATIONS For captured
     * types, the lower bound might be interesting so we compare against the lower bound but for
     * most types the constraint added in this method is probably discarded in the next round of
     * reduction (especially since we don't implement capture at the moment).
     */
    @Override
    public Void visitNull_Typevar(
            AnnotatedNullType subtype,
            AnnotatedTypeVariable supertype,
            Set<AFConstraint> constraints) {
        // Note: We would expect that parameter is not one of the targets or else it would already
        // be removed. Therefore we compare NULL against its bound.
        addConstraint(subtype, supertype.getLowerBound(), constraints);
        return null;
    }

    @Override
    public Void visitNull_Wildcard(
            AnnotatedNullType subtype,
            AnnotatedWildcardType supertype,
            Set<AFConstraint> constraints) {
        TypeArgInferenceUtil.checkForUninferredTypes(supertype);
        // we don't use visitSupertype because Null types won't have interesting components
        constraints.add(new A2F(subtype, supertype.getSuperBound()));
        return null;
    }

    @Override
    public Void visitNull_Null(
            AnnotatedNullType argument,
            AnnotatedNullType parameter,
            Set<AFConstraint> constraints) {
        return null;
    }

    @Override
    public Void visitNull_Union(
            AnnotatedNullType argument,
            AnnotatedUnionType parameter,
            Set<AFConstraint> constraints) {
        return null; // TODO: UNIONS ARE NOT YET SUPPORTED
    }

    // Despite the fact that intersections are not yet supported, this is the right impelementation.
    // NULL types only have primary annotations.  Since type parameters cannot be a member of the
    // intersection's bounds (though they can be component types), we do not need to do anything
    // further.
    @Override
    public Void visitNull_Intersection(
            AnnotatedNullType argument,
            AnnotatedIntersectionType parameter,
            Set<AFConstraint> constraints) {
        return null;
    }

    // Primitive parameter types tell us nothing about the type parameters
    @Override
    public Void visitNull_Primitive(
            AnnotatedNullType argument,
            AnnotatedPrimitiveType parameter,
            Set<AFConstraint> constraints) {
        return null;
    }

    // ------------------------------------------------------------------------
    // Primitive as argument

    @Override
    public Void visitPrimitive_Declared(
            AnnotatedPrimitiveType subtype,
            AnnotatedDeclaredType supertype,
            Set<AFConstraint> constraints) {
        // we may be able to eliminate this case, since I believe the corresponding constraint will
        // just be discarded as the parameter must be a boxed primitive
        addConstraint(typeFactory.getBoxedType(subtype), supertype, constraints);
        return null;
    }

    // Primitive parameter types tell us nothing about the type parameters
    @Override
    public Void visitPrimitive_Primitive(
            AnnotatedPrimitiveType subtype,
            AnnotatedPrimitiveType supertype,
            Set<AFConstraint> constraints) {
        return null;
    }

    @Override
    public Void visitPrimitive_Intersection(
            AnnotatedPrimitiveType subtype,
            AnnotatedIntersectionType supertype,
            Set<AFConstraint> constraints) {
        addConstraint(typeFactory.getBoxedType(subtype), supertype, constraints);
        return null;
    }

    // ------------------------------------------------------------------------
    // Union as argument
    @Override
    public Void visitUnion_Declared(
            AnnotatedUnionType argument,
            AnnotatedDeclaredType parameter,
            Set<AFConstraint> constraints) {
        return null; // TODO: UNIONS ARE NOT CURRENTLY SUPPORTED
    }

    // ------------------------------------------------------------------------
    // typevar as argument
    // If we've reached this point, the typevar is NOT one of the types we are inferring.

    @Override
    public Void visitTypevar_Declared(
            AnnotatedTypeVariable subtype,
            AnnotatedDeclaredType supertype,
            Set<AFConstraint> constraints) {
        addConstraint(subtype.getUpperBound(), supertype, constraints);
        return null;
    }

    @Override
    public Void visitTypevar_Typevar(
            AnnotatedTypeVariable subtype,
            AnnotatedTypeVariable supertype,
            Set<AFConstraint> constraints) {
        // if we've reached this point and the two are corresponding type variables, then they are
        // NOT ones that may have a type variable we are inferring types for and therefore we can
        // discard this constraint
        if (!AnnotatedTypes.areCorrespondingTypeVariables(
                typeFactory.getElementUtils(), subtype, supertype)) {
            addConstraint(subtype.getUpperBound(), supertype.getLowerBound(), constraints);
        }

        return null;
    }

    @Override
    public Void visitTypevar_Null(
            AnnotatedTypeVariable subtype,
            AnnotatedNullType supertype,
            Set<AFConstraint> constraints) {
        addConstraint(subtype.getUpperBound(), supertype, constraints);
        return null;
    }

    @Override
    public Void visitTypevar_Wildcard(
            AnnotatedTypeVariable subtype,
            AnnotatedWildcardType supertype,
            Set<AFConstraint> constraints) {
        visitWildcardAsSuperType(subtype, supertype, constraints);
        return null;
    }

    @Override
    public Void visitTypevar_Intersection(
            AnnotatedTypeVariable subtype,
            AnnotatedIntersectionType supertype,
            Set<AFConstraint> constraints) {
        addConstraint(subtype.getUpperBound(), supertype, constraints);
        return null;
    }

    // ------------------------------------------------------------------------
    // wildcard as subtype
    @Override
    public Void visitWildcard_Array(
            AnnotatedWildcardType subtype,
            AnnotatedArrayType supertype,
            Set<AFConstraint> constraints) {
        TypeArgInferenceUtil.checkForUninferredTypes(subtype);
        addConstraint(subtype.getExtendsBound(), supertype, constraints);
        return null;
    }

    @Override
    public Void visitWildcard_Declared(
            AnnotatedWildcardType subtype,
            AnnotatedDeclaredType supertype,
            Set<AFConstraint> constraints) {
        TypeArgInferenceUtil.checkForUninferredTypes(subtype);
        addConstraint(subtype.getExtendsBound(), supertype, constraints);
        return null;
    }

    @Override
    public Void visitWildcard_Intersection(
            AnnotatedWildcardType subtype,
            AnnotatedIntersectionType supertype,
            Set<AFConstraint> constraints) {
        TypeArgInferenceUtil.checkForUninferredTypes(subtype);
        addConstraint(subtype.getExtendsBound(), supertype, constraints);
        return null;
    }

    @Override
    public Void visitWildcard_Primitive(
            AnnotatedWildcardType subtype,
            AnnotatedPrimitiveType supertype,
            Set<AFConstraint> constraints) {
        return null;
    }

    @Override
    public Void visitWildcard_Typevar(
            AnnotatedWildcardType subtype,
            AnnotatedTypeVariable supertype,
            Set<AFConstraint> constraints) {
        TypeArgInferenceUtil.checkForUninferredTypes(subtype);
        addConstraint(subtype.getExtendsBound(), supertype, constraints);
        return null;
    }

    @Override
    public Void visitWildcard_Wildcard(
            AnnotatedWildcardType subtype,
            AnnotatedWildcardType supertype,
            Set<AFConstraint> constraints) {
        TypeArgInferenceUtil.checkForUninferredTypes(subtype);
        // since wildcards are handled in visitDeclared_Declared this could only occur if two
        // wildcards were passed to type subtype inference at the top level.  This can only occur
        // because we do not implement capture conversion.
        visitWildcardAsSuperType(subtype.getExtendsBound(), supertype, constraints);
        return null;
    }

    // should the same logic apply to typevars?
    public void visitWildcardAsSuperType(
            AnnotatedTypeMirror subtype,
            AnnotatedWildcardType supertype,
            Set<AFConstraint> constraints) {
        TypeArgInferenceUtil.checkForUninferredTypes(supertype);
        // this case occur only when supertype should actually be capture converted (which we don't
        // do) because all other wildcard cases would be handled via Declared_Declared
        addConstraint(subtype, supertype.getSuperBound(), constraints);

        // if type1 is below the superbound then it is necessarily below the extends bound
        // BUT the extends bound may have interesting component types (like the array component)
        // to which we also want to apply constraints
        // e.g. visitArray_Wildcard(@I String[], ? extends @A String[])
        // if @I is an annotation we are trying to infer then we still want to infer that @I <: @A
        // in fact
        addInverseConstraint(subtype, supertype.getExtendsBound(), constraints);
    }
}
