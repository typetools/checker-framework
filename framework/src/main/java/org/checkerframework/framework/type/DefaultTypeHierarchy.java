package org.checkerframework.framework.type;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.Covariant;
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
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Default implementation of TypeHierarchy that implements the JLS specification with minor
 * deviations as outlined by the Checker Framework manual. Changes to the JLS include forbidding
 * covariant array types, raw types, and allowing covariant type arguments depending on various
 * options passed to DefaultTypeHierarchy.
 *
 * <p>Subtyping rules of the JLS can be found in section 4.10, "Subtyping":
 * https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10
 *
 * <p>Note: The visit methods of this class must be public but it is intended to be used through a
 * TypeHierarchy interface reference which will only allow isSubtype to be called. It does not make
 * sense to call the visit methods on their own.
 */
public class DefaultTypeHierarchy extends AbstractAtmComboVisitor<Boolean, Void>
        implements TypeHierarchy {
    // used for processingEnvironment when needed
    protected final BaseTypeChecker checker;

    protected final QualifierHierarchy qualifierHierarchy;
    protected final StructuralEqualityComparer equalityComparer;

    protected final boolean ignoreRawTypes;
    protected final boolean invariantArrayComponents;

    // TODO: Incorporate feedback from David/Suzanne
    // IMPORTANT_NOTE:

    // For MultigraphQualifierHierarchies, we check the subtyping relationship of each annotation
    // hierarchy individually.  This is done because when comparing a pair of type variables,
    // sometimes you need to traverse and compare the bounds of two type variables.  Other times it
    // is incorrect to compare the bounds.  These two cases can occur simultaneously when comparing
    // two hierarchies at once.  In this case, comparing both hierarchies simultaneously will leadd
    // ot an error.  More detail is given below.
    //
    // Recall, type variables may or may not have a primary annotation for each individual
    // hierarchy.  When comparing
    // two type variables for a specific hierarchy we have five possible cases:
    //      case 1:  only one of the type variables has a primary annotation
    //      case 2a: both type variables have primary annotations and they are uses of the same type
    //               parameter
    //      case 2b: both type variables have primary annotations and they are uses of different
    //               type parameters
    //      case 3a: neither type variable has a primary annotation and they are uses of the same
    //               type parameter
    //      case 3b: neither type variable has a primary annotation and they are uses of different
    //               type parameters
    //
    // Case 1, 2b, and 3b require us to traverse both type variables bounds to ensure that the
    // subtype's upper bound is a subtype of the supertype's lower bound. Cases 2a requires only
    // that we check that the primary annotation on the subtype is a subtype of the primary
    // annotation on the supertype.  In case 3a, we can just return true, since two
    // non-primary-annotated uses of the same type parameter are equivalent.  In this case it would
    // be an error to check the bounds because the check would only return true when the bounds are
    // exact but it should always return true.
    //
    // A problem occurs when, one hierarchy matches cases 1, 2b, or 3b and the other matches 3a.  In
    // the first set of cases we MUST check the type variables' bounds.  In case 3a we MUST NOT
    // check the bounds.  e.g.
    //
    // Suppose I have a hierarchy with two tops @A1 and @B1.  Let @A0 <: @A1 and @B0 <: @B1.
    //  @A1 T t1;  T t2;
    //  t1 = t2;
    //
    // To typecheck "t1 = t2;" in the hierarchy topped by @A1, we need to descend into the bounds of
    // t1 and t2 (where t1's bounds will be overridden by @A1).  However, for hierarchy B we need
    // only recognize that since neither variable has a primary annotation, the types are equivalent
    // and no traversal is needed.  If we tried to do these two actions simultaneously, in every
    // visit and isSubtype call, we would have to check to see that the @B hierarchy has been
    // handled and ignore those annotations.
    //
    // Solutions:
    // We could handle this problem by keeping track of which hierarchies have already been taken
    // care of.  We could then check each hierarchy before making comparisons.  But this would lead
    // to complicated plumbing that would be hard to understand.
    // The chosen solution is to only check one hierarchy at a time.  One trade-off to this approach
    // is that we have to re-traverse the types for each hierarchy being checked.
    //
    // The field currentTop identifies the hierarchy for which the types are currently being
    // checked.
    // Final note: all annotation comparisons are done via isPrimarySubtype, isBottom, and
    // isAnnoSubtype in order to ensure that we first get the annotations in the hierarchy of
    // currentTop before passing annotations to qualifierHierarchy.
    /** The top annotation of the hierarchy currently being checked. */
    protected AnnotationMirror currentTop;

    /** Stores the result of isSubtype, if that result is true. */
    protected final SubtypeVisitHistory visitHistory;

    /**
     * Stores the result of isSubtype for type arguments. Prevents infinite recursion on types that
     * refer to themselves. (Stores both true and false results.)
     */
    protected final StructuralEqualityVisitHistory typeargVisitHistory;

    /** Creates a DefaultTypeHierarchy. */
    public DefaultTypeHierarchy(
            final BaseTypeChecker checker,
            final QualifierHierarchy qualifierHierarchy,
            boolean ignoreRawTypes,
            boolean invariantArrayComponents) {
        this.checker = checker;
        this.qualifierHierarchy = qualifierHierarchy;
        this.visitHistory = new SubtypeVisitHistory();
        this.typeargVisitHistory = new StructuralEqualityVisitHistory();
        this.equalityComparer = createEqualityComparer();

        this.ignoreRawTypes = ignoreRawTypes;
        this.invariantArrayComponents = invariantArrayComponents;
    }

    /** Create the equality comparer. */
    protected StructuralEqualityComparer createEqualityComparer() {
        return new StructuralEqualityComparer(typeargVisitHistory);
    }

    /**
     * Returns true if subtype {@literal <:} supertype. This implementation iterates over all top
     * annotations and invokes {@link #isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror,
     * AnnotationMirror)}. Most type systems should not override this method, but instead override
     * {@link #isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror, AnnotationMirror)} or some of the
     * {@code visitXXX} methods.
     *
     * @param subtype expected subtype
     * @param supertype expected supertype
     * @return true if subtype is actually a subtype of supertype
     */
    @Override
    public boolean isSubtype(
            final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype) {
        for (final AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
            if (!isSubtype(subtype, supertype, top)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if {@code subtype <: supertype}, but only for the hierarchy of which {@code top}
     * is the top.
     *
     * @param subtype expected subtype
     * @param supertype expected supertype
     * @param top the hierarchy for which we want to make a comparison
     * @return returns true if {@code subtype} is a subtype of {@code supertype} in the qualifier
     *     hierarchy whose top is {@code top}
     */
    protected boolean isSubtype(
            final AnnotatedTypeMirror subtype,
            final AnnotatedTypeMirror supertype,
            final AnnotationMirror top) {
        assert top != null;
        currentTop = top;
        return AtmCombo.accept(subtype, supertype, null, this);
    }

    /** @return error message for the case when two types shouldn't be compared */
    @Override
    protected String defaultErrorMessage(
            final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype, final Void p) {
        return "Incomparable types ("
                + subtype
                + ", "
                + supertype
                + ") visitHistory = "
                + visitHistory;
    }

    /**
     * Compare the primary annotations of subtype and supertype. Neither type can be missing
     * annotations.
     *
     * @return true if the primary annotation on subtype {@literal <:} primary annotation on
     *     supertype for the current top.
     */
    protected boolean isPrimarySubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
        return isPrimarySubtype(subtype, supertype, false);
    }

    /**
     * Compare the primary annotations of subtype and supertype.
     *
     * @param annosCanBeEmtpy indicates that annotations may be missing from the typemirror
     * @return true if the primary annotation on subtype {@literal <:} primary annotation on
     *     supertype for the current top or both annotations are null. False is returned if one
     *     annotation is null and the other is not.
     */
    protected boolean isPrimarySubtype(
            AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, boolean annosCanBeEmtpy) {
        final AnnotationMirror subtypeAnno = subtype.getAnnotationInHierarchy(currentTop);
        final AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(currentTop);

        return isAnnoSubtype(subtypeAnno, supertypeAnno, annosCanBeEmtpy);
    }

    /**
     * Compare the primary annotations of subtype and supertype.
     *
     * @param subtypeAnno annotation we expect to be a subtype
     * @param supertypeAnno annotation we expect to be a supertype of subtype
     * @param annosCanBeEmtpy indicates that annotations may be missing from the typemirror
     * @return true if subtype {@literal <:} supertype or both annotations are null. False is
     *     returned if one annotation is null and the other is not.
     */
    protected boolean isAnnoSubtype(
            AnnotationMirror subtypeAnno, AnnotationMirror supertypeAnno, boolean annosCanBeEmtpy) {
        if (annosCanBeEmtpy && subtypeAnno == null && supertypeAnno == null) {
            return true;
        }

        return qualifierHierarchy.isSubtype(subtypeAnno, supertypeAnno);
    }

    /**
     * Checks to see if subtype is bottom (if a bottom exists) If there is no explicit bottom then
     * false is returned.
     *
     * @param subtype type to isValid against bottom
     * @return true if subtype's primary annotation is bottom
     */
    protected boolean isBottom(final AnnotatedTypeMirror subtype) {
        final AnnotationMirror bottom = qualifierHierarchy.getBottomAnnotation(currentTop);
        if (bottom == null) {
            return false; // can't be below infinitely sized hierarchy
        }

        switch (subtype.getKind()) {
            case TYPEVAR:
                return isBottom(((AnnotatedTypeVariable) subtype).getUpperBound());

            case WILDCARD:
                final AnnotatedWildcardType subtypeWc = (AnnotatedWildcardType) subtype;
                return isBottom(subtypeWc);

                // TODO: DO ANYTHING SPECIAL FOR INTERSECTIONS OR UNIONS?
                // TODO: ENUMERATE THE VALID CASES?

            default:
                final AnnotationMirror subtypeAnno = subtype.getAnnotationInHierarchy(currentTop);
                return isAnnoSubtype(subtypeAnno, bottom, false);
        }
    }

    /**
     * Check and subtype first determines if the subtype/supertype combination has already been
     * visited. If so, it returns true, otherwise add the subtype/supertype combination and then
     * make a subtype check
     */
    protected boolean checkAndSubtype(
            final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype) {
        if (visitHistory.contains(subtype, supertype, currentTop)) {
            return true;
        }

        boolean result = isSubtype(subtype, supertype, currentTop);
        visitHistory.add(subtype, supertype, currentTop, result);
        return result;
    }

    /**
     * Are all the types in {@code subtypes} a subtype of {@code superType}?
     *
     * <p>The underlying type mirrors of {@code subtypes} must be subtypes of the underlying type
     * mirror of {@code superType}.
     */
    protected boolean areAllSubtypes(
            final Iterable<? extends AnnotatedTypeMirror> subtypes,
            final AnnotatedTypeMirror supertype) {
        for (final AnnotatedTypeMirror subtype : subtypes) {
            if (!isSubtype(subtype, supertype, currentTop)) {
                return false;
            }
        }

        return true;
    }

    protected boolean areEqualInHierarchy(
            final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        return equalityComparer.areEqualInHierarchy(type1, type2, currentTop);
    }

    /**
     * A declared type is considered a supertype of another declared type only if all of the type
     * arguments of the declared type "contain" the corresponding type arguments of the subtype.
     * Containment is described in the JLS section 4.5.1 "Type Arguments of Parameterized Types",
     * https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.5.1
     *
     * @param inside the "subtype" type argument
     * @param outside the "supertype" type argument
     * @param canBeCovariant whether or not type arguments are allowed to be covariant
     * @return true if inside is contained by outside OR, if canBeCovariant == true, inside is a
     *     subtype of outside
     */
    protected boolean isContainedBy(
            final AnnotatedTypeMirror inside,
            final AnnotatedTypeMirror outside,
            boolean canBeCovariant) {

        if (ignoreUninferredTypeArgument(inside) || ignoreUninferredTypeArgument(outside)) {
            typeargVisitHistory.add(inside, outside, currentTop, true);
            return true;
        }

        if (outside.getKind() != TypeKind.WILDCARD
                && !TypesUtils.isCaptured(outside.getUnderlyingType())) {
            if (canBeCovariant) {
                return isSubtype(inside, outside, currentTop);
            }
            return areEqualInHierarchy(inside, outside);
        }

        AnnotatedTypeMirror outsideUpperBound;
        AnnotatedTypeMirror outsideLowerBound;
        if (outside.getKind() == TypeKind.WILDCARD) {
            outsideUpperBound = ((AnnotatedWildcardType) outside).getExtendsBound();
            outsideLowerBound = ((AnnotatedWildcardType) outside).getSuperBound();
        } else if (TypesUtils.isCaptured(outside.getUnderlyingType())) {
            outsideUpperBound = ((AnnotatedTypeVariable) outside).getUpperBound();
            outsideLowerBound = ((AnnotatedTypeVariable) outside).getLowerBound();
        } else {
            throw new BugInCF(
                    "Expected a wildcard or captured type variable, but found " + outside);
        }
        Boolean previousResult = typeargVisitHistory.result(inside, outside, currentTop);
        if (previousResult != null) {
            return previousResult;
        }

        typeargVisitHistory.add(inside, outside, currentTop, true);
        boolean result =
                isContainedWildcard(
                        inside, outside, outsideUpperBound, outsideLowerBound, canBeCovariant);
        typeargVisitHistory.add(inside, outside, currentTop, result);
        return result;
    }

    private boolean isContainedWildcard(
            AnnotatedTypeMirror inside,
            AnnotatedTypeMirror outside,
            AnnotatedTypeMirror outsideUpperBound,
            AnnotatedTypeMirror outsideLowerBound,
            boolean canBeCovariant) {

        if (inside.getKind() == TypeKind.WILDCARD) {
            outsideUpperBound =
                    checker.getTypeFactory()
                            .widenToUpperBound(outsideUpperBound, (AnnotatedWildcardType) inside);
        }
        while (outsideUpperBound.getKind() == TypeKind.WILDCARD) {
            if (ignoreUninferredTypeArgument(outsideUpperBound)) {
                return true;
            }
            outsideUpperBound = ((AnnotatedWildcardType) outsideUpperBound).getExtendsBound();
        }

        AnnotatedTypeMirror castedInside =
                AnnotatedTypes.castedAsSuper(inside.atypeFactory, inside, outsideUpperBound);
        if (!checkAndSubtype(castedInside, outsideUpperBound)) {
            return false;
        }

        if (outside.getKind() == TypeKind.WILDCARD
                && outsideLowerBound.getKind() == TypeKind.TYPEVAR) {
            // tests/all-systems/Issue1991.java crashes without this.
            return true;
        }
        return canBeCovariant || checkAndSubtype(outsideLowerBound, inside);
    }

    private boolean ignoreUninferredTypeArgument(AnnotatedTypeMirror type) {
        if (type.atypeFactory.ignoreUninferredTypeArguments
                && type.getKind() == TypeKind.WILDCARD) {
            final AnnotatedWildcardType insideWc = (AnnotatedWildcardType) type;
            if (insideWc.isUninferredTypeArgument()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // Arrays as subtypes

    @Override
    public Boolean visitArray_Array(
            AnnotatedArrayType subtype, AnnotatedArrayType supertype, Void p) {
        return isPrimarySubtype(subtype, supertype)
                && (invariantArrayComponents
                        ? areEqualInHierarchy(
                                subtype.getComponentType(), supertype.getComponentType())
                        : isSubtype(
                                subtype.getComponentType(),
                                supertype.getComponentType(),
                                currentTop));
    }

    @Override
    public Boolean visitArray_Declared(
            AnnotatedArrayType subtype, AnnotatedDeclaredType supertype, Void p) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitArray_Null(
            AnnotatedArrayType subtype, AnnotatedNullType supertype, Void p) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitArray_Intersection(
            AnnotatedArrayType subtype, AnnotatedIntersectionType supertype, Void p) {
        return isSubtype(
                AnnotatedTypes.castedAsSuper(subtype.atypeFactory, subtype, supertype),
                supertype,
                currentTop);
    }

    @Override
    public Boolean visitArray_Wildcard(
            AnnotatedArrayType subtype, AnnotatedWildcardType supertype, Void p) {
        return visitWildcardSupertype(subtype, supertype);
    }

    // ------------------------------------------------------------------------
    // Declared as subtype
    @Override
    public Boolean visitDeclared_Array(
            AnnotatedDeclaredType subtype, AnnotatedArrayType supertype, Void p) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitDeclared_Declared(
            AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype, Void p) {
        AnnotatedDeclaredType subtypeAsSuper =
                AnnotatedTypes.castedAsSuper(subtype.atypeFactory, subtype, supertype);

        if (!isPrimarySubtype(subtypeAsSuper, supertype)) {
            return false;
        }

        if (visitHistory.contains(subtypeAsSuper, supertype, currentTop)) {
            return true;
        }

        final boolean result =
                visitTypeArgs(subtypeAsSuper, supertype, subtype.wasRaw(), supertype.wasRaw());
        visitHistory.add(subtypeAsSuper, supertype, currentTop, result);

        return result;
    }

    /**
     * A helper class for visitDeclared_Declared. There are subtypes of DefaultTypeHierarchy that
     * need to customize the handling of type arguments. This method provides a convenient extension
     * point.
     */
    protected boolean visitTypeArgs(
            final AnnotatedDeclaredType subtype,
            final AnnotatedDeclaredType supertype,
            final boolean subtypeRaw,
            final boolean supertypeRaw) {

        final boolean ignoreTypeArgs = ignoreRawTypes && (subtypeRaw || supertypeRaw);

        if (ignoreTypeArgs) {
            return true;
        }

        final List<? extends AnnotatedTypeMirror> subtypeTypeArgs = subtype.getTypeArguments();
        final List<? extends AnnotatedTypeMirror> supertypeTypeArgs = supertype.getTypeArguments();

        if (subtypeTypeArgs.size() != supertypeTypeArgs.size()) {
            return false;
        }
        if (subtypeTypeArgs.isEmpty()) {
            return true;
        }

        final TypeElement supertypeElem = (TypeElement) supertype.getUnderlyingType().asElement();
        List<Integer> covariantArgIndexes = null;
        AnnotationMirror covam =
                supertype.atypeFactory.getDeclAnnotation(supertypeElem, Covariant.class);

        if (covam != null) {
            covariantArgIndexes =
                    AnnotationUtils.getElementValueArray(covam, "value", Integer.class, false);
        }

        for (int i = 0; i < supertypeTypeArgs.size(); i++) {
            final AnnotatedTypeMirror superTypeArg = supertypeTypeArgs.get(i);
            final AnnotatedTypeMirror subTypeArg = subtypeTypeArgs.get(i);
            final boolean covariant =
                    covariantArgIndexes != null && covariantArgIndexes.contains(i);

            boolean result = isContainedBy(subTypeArg, superTypeArg, covariant);

            if (!result) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Boolean visitDeclared_Intersection(
            AnnotatedDeclaredType subtype, AnnotatedIntersectionType supertype, Void p) {
        return visitIntersectionSupertype(subtype, supertype);
    }

    @Override
    public Boolean visitDeclared_Null(
            AnnotatedDeclaredType subtype, AnnotatedNullType supertype, Void p) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitDeclared_Primitive(
            AnnotatedDeclaredType subtype, AnnotatedPrimitiveType supertype, Void p) {
        // We do an asSuper first because in some cases unboxing implies a more specific annotation
        // e.g. @UnknownInterned Integer => @Interned int  because primitives are always interned
        final AnnotatedPrimitiveType subAsSuper =
                AnnotatedTypes.castedAsSuper(subtype.atypeFactory, subtype, supertype);
        if (subAsSuper == null) {
            return isPrimarySubtype(subtype, supertype);
        }
        return isPrimarySubtype(subAsSuper, supertype);
    }

    @Override
    public Boolean visitDeclared_Typevar(
            AnnotatedDeclaredType subtype, AnnotatedTypeVariable supertype, Void p) {
        return visitTypevarSupertype(subtype, supertype);
    }

    @Override
    public Boolean visitDeclared_Union(
            AnnotatedDeclaredType subtype, AnnotatedUnionType supertype, Void p) {
        Types types = checker.getTypeUtils();
        for (AnnotatedDeclaredType supertypeAltern : supertype.getAlternatives()) {
            if (TypesUtils.isErasedSubtype(
                            subtype.getUnderlyingType(), supertypeAltern.getUnderlyingType(), types)
                    && isSubtype(subtype, supertypeAltern, currentTop)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitDeclared_Wildcard(
            AnnotatedDeclaredType subtype, AnnotatedWildcardType supertype, Void p) {
        return visitWildcardSupertype(subtype, supertype);
    }

    // ------------------------------------------------------------------------
    // Intersection as subtype
    @Override
    public Boolean visitIntersection_Declared(
            AnnotatedIntersectionType subtype, AnnotatedDeclaredType supertype, Void p) {
        Types types = checker.getTypeUtils();
        for (AnnotatedDeclaredType subtypeI : subtype.directSuperTypes()) {
            if (TypesUtils.isErasedSubtype(
                            subtypeI.getUnderlyingType(), supertype.getUnderlyingType(), types)
                    && isSubtype(subtypeI, supertype, currentTop)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitIntersection_Primitive(
            AnnotatedIntersectionType subtype, AnnotatedPrimitiveType supertype, Void p) {
        for (AnnotatedDeclaredType subtypeI : subtype.directSuperTypes()) {
            if (TypesUtils.isBoxedPrimitive(subtypeI.getUnderlyingType())
                    && isSubtype(subtypeI, supertype, currentTop)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitIntersection_Intersection(
            AnnotatedIntersectionType subtype, AnnotatedIntersectionType supertype, Void p) {
        Types types = checker.getTypeUtils();
        for (AnnotatedDeclaredType subtypeI : subtype.directSuperTypes()) {
            for (AnnotatedDeclaredType supertypeI : supertype.directSuperTypes()) {
                if (TypesUtils.isErasedSubtype(
                                subtypeI.getUnderlyingType(), supertypeI.getUnderlyingType(), types)
                        && !isSubtype(subtypeI, supertypeI, currentTop)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Boolean visitIntersection_Null(
            AnnotatedIntersectionType subtype, AnnotatedNullType supertype, Void p) {
        // this can occur through capture conversion/comparing bounds
        for (AnnotatedDeclaredType subtypeI : subtype.directSuperTypes()) {
            if (isPrimarySubtype(subtypeI, supertype)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitIntersection_Typevar(
            AnnotatedIntersectionType subtype, AnnotatedTypeVariable supertype, Void p) {
        // this can occur through capture conversion/comparing bounds
        Types types = checker.getTypeUtils();
        for (AnnotatedDeclaredType subtypeI : subtype.directSuperTypes()) {
            if (TypesUtils.isErasedSubtype(
                            subtypeI.getUnderlyingType(), supertype.getUnderlyingType(), types)
                    && isSubtype(subtypeI, supertype, currentTop)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // Null as subtype
    @Override
    public Boolean visitNull_Array(
            AnnotatedNullType subtype, AnnotatedArrayType supertype, Void p) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitNull_Declared(
            AnnotatedNullType subtype, AnnotatedDeclaredType supertype, Void p) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitNull_Typevar(
            AnnotatedNullType subtype, AnnotatedTypeVariable supertype, Void p) {
        return visitTypevarSupertype(subtype, supertype);
    }

    @Override
    public Boolean visitNull_Wildcard(
            AnnotatedNullType subtype, AnnotatedWildcardType supertype, Void p) {
        return visitWildcardSupertype(subtype, supertype);
    }

    @Override
    public Boolean visitNull_Null(AnnotatedNullType subtype, AnnotatedNullType supertype, Void p) {
        // this can occur when comparing typevar lower bounds since they are usually null types
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitNull_Union(
            AnnotatedNullType subtype, AnnotatedUnionType supertype, Void p) {
        for (AnnotatedDeclaredType supertypeAltern : supertype.getAlternatives()) {
            if (isSubtype(subtype, supertypeAltern, currentTop)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitNull_Intersection(
            AnnotatedNullType subtype, AnnotatedIntersectionType supertype, Void p) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitNull_Primitive(
            AnnotatedNullType subtype, AnnotatedPrimitiveType supertype, Void p) {
        return isPrimarySubtype(subtype, supertype);
    }

    // ------------------------------------------------------------------------
    // Primitive as subtype
    @Override
    public Boolean visitPrimitive_Declared(
            AnnotatedPrimitiveType subtype, AnnotatedDeclaredType supertype, Void p) {
        // see comment in visitDeclared_Primitive
        final AnnotatedDeclaredType subAsSuper =
                AnnotatedTypes.castedAsSuper(subtype.atypeFactory, subtype, supertype);
        if (subAsSuper == null) {
            return isPrimarySubtype(subtype, supertype);
        }
        return isPrimarySubtype(subAsSuper, supertype);
    }

    @Override
    public Boolean visitPrimitive_Primitive(
            AnnotatedPrimitiveType subtype, AnnotatedPrimitiveType supertype, Void p) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitPrimitive_Intersection(
            AnnotatedPrimitiveType subtype, AnnotatedIntersectionType supertype, Void p) {
        return visitIntersectionSupertype(subtype, supertype);
    }

    @Override
    public Boolean visitPrimitive_Typevar(
            AnnotatedPrimitiveType subtype, AnnotatedTypeVariable supertype, Void p) {
        return AtmCombo.accept(subtype, supertype.getUpperBound(), null, this);
    }

    @Override
    public Boolean visitPrimitive_Wildcard(
            AnnotatedPrimitiveType subtype, AnnotatedWildcardType supertype, Void p) {
        if (supertype.atypeFactory.ignoreUninferredTypeArguments
                && supertype.isUninferredTypeArgument()) {
            return true;
        }
        // this can occur when passing a primitive to a method on a raw type (see test
        // checker/tests/nullness/RawAndPrimitive.java).  This can also occur because we don't box
        // primitives when we should and don't capture convert.
        return isPrimarySubtype(subtype, supertype.getSuperBound());
    }

    // ------------------------------------------------------------------------
    // Union as subtype
    @Override
    public Boolean visitUnion_Declared(
            AnnotatedUnionType subtype, AnnotatedDeclaredType supertype, Void p) {
        return visitUnionSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitUnion_Intersection(
            AnnotatedUnionType subtype, AnnotatedIntersectionType supertype, Void p) {
        // For example:
        // <T extends Throwable & Cloneable> void method(T param) {}
        // ...
        // catch (Exception1 | Exception2 union) { // Assuming Exception1 and Exception2 implement
        // Cloneable
        //   method(union);
        // This case happens when checking that the inferred type argument is a subtype of the
        // declared type argument of method.
        // See org.checkerframework.common.basetype.BaseTypeVisitor#checkTypeArguments
        return visitUnionSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitUnion_Union(
            AnnotatedUnionType subtype, AnnotatedUnionType supertype, Void p) {
        // For example:
        // <T> void method(T param) {}
        // ...
        // catch (Exception1 | Exception2 union) {
        //   method(union);
        // This case happens when checking the arguments to method after type variable substitution
        return visitUnionSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitUnion_Wildcard(
            AnnotatedUnionType subtype, AnnotatedWildcardType supertype, Void p) {
        // For example:
        // } catch (RuntimeException | IOException e) {
        //     ArrayList<? super Exception> lWildcard = new ArrayList<>();
        //     lWildcard.add(e);

        return visitWildcardSupertype(subtype, supertype);
    }

    // ------------------------------------------------------------------------
    // typevar as subtype
    @Override
    public Boolean visitTypevar_Declared(
            AnnotatedTypeVariable subtype, AnnotatedDeclaredType supertype, Void p) {
        return visitTypevarSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitTypevar_Intersection(
            AnnotatedTypeVariable subtype, AnnotatedIntersectionType supertype, Void p) {
        // this can happen when checking type param bounds
        return visitIntersectionSupertype(subtype, supertype);
    }

    @Override
    public Boolean visitTypevar_Primitive(
            AnnotatedTypeVariable subtype, AnnotatedPrimitiveType supertype, Void p) {
        return visitTypevarSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitTypevar_Typevar(
            AnnotatedTypeVariable subtype, AnnotatedTypeVariable supertype, Void p) {

        if (AnnotatedTypes.haveSameDeclaration(checker.getTypeUtils(), subtype, supertype)) {
            // subtype and supertype are uses of the same type parameter
            boolean subtypeHasAnno = subtype.getAnnotationInHierarchy(currentTop) != null;
            boolean supertypeHasAnno = supertype.getAnnotationInHierarchy(currentTop) != null;

            if (subtypeHasAnno && supertypeHasAnno) {
                // if both have primary annotations then you can just check the primary annotations
                // as the bounds are the same
                return isPrimarySubtype(subtype, supertype, true);

            } else if (!subtypeHasAnno
                    && !supertypeHasAnno
                    && areEqualInHierarchy(subtype, supertype)) {
                // two unannotated uses of the same type parameter are of the same type
                return true;

            } else if (subtype.getUpperBound().getKind() == TypeKind.INTERSECTION) {
                // This case happens when a type has an intersection bound.  e.g.,
                // T extends A & B
                //
                // And one use of the type has an annotation and the other does not. e.g.,
                // @X T xt = ...;  T t = ..;
                // xt = t;
                //
                return visit(subtype.getUpperBound(), supertype.getLowerBound(), null);
            }
        }

        if (AnnotatedTypes.areCorrespondingTypeVariables(
                checker.getProcessingEnvironment().getElementUtils(), subtype, supertype)) {
            if (areEqualInHierarchy(subtype, supertype)) {
                return true;
            }
        }

        // check that the upper bound of the subtype is below the lower bound of the supertype
        return visitTypevarSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitTypevar_Null(
            AnnotatedTypeVariable subtype, AnnotatedNullType supertype, Void p) {
        return visitTypevarSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitTypevar_Wildcard(
            AnnotatedTypeVariable subtype, AnnotatedWildcardType supertype, Void p) {
        return visitWildcardSupertype(subtype, supertype);
    }

    // ------------------------------------------------------------------------
    // wildcard as subtype

    @Override
    public Boolean visitWildcard_Array(
            AnnotatedWildcardType subtype, AnnotatedArrayType supertype, Void p) {
        return visitWildcardSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitWildcard_Declared(
            AnnotatedWildcardType subtype, AnnotatedDeclaredType supertype, Void p) {
        if (subtype.isUninferredTypeArgument()) {
            if (subtype.atypeFactory.ignoreUninferredTypeArguments) {
                return true;
            } else if (supertype.getTypeArguments().isEmpty()) {
                // visitWildcardSubtype doesn't check uninferred type arguments, because the
                // underlying Java types may not be in the correct relationship.  But, if the
                // declared type does not have type arguments, then checking primary annotations is
                // sufficient.
                // For example, if the wildcard is ? extends @Nullable Object and the supertype is
                // @Nullable String, then it is safe to return true. However if the supertype is
                // @NullableList<@NonNull String> then it's not possible to decide if it is a
                // subtype of the wildcard.
                AnnotationMirror subtypeAnno =
                        subtype.getEffectiveAnnotationInHierarchy(currentTop);
                AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(currentTop);
                return isAnnoSubtype(subtypeAnno, supertypeAnno, false);
            }
        }
        return visitWildcardSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitWildcard_Intersection(
            AnnotatedWildcardType subtype, AnnotatedIntersectionType supertype, Void p) {
        return visitWildcardSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitWildcard_Primitive(
            AnnotatedWildcardType subtype, AnnotatedPrimitiveType supertype, Void p) {
        if (subtype.isUninferredTypeArgument()) {
            AnnotationMirror subtypeAnno = subtype.getEffectiveAnnotationInHierarchy(currentTop);
            AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(currentTop);
            return isAnnoSubtype(subtypeAnno, supertypeAnno, false);
        }
        return visitWildcardSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitWildcard_Typevar(
            AnnotatedWildcardType subtype, AnnotatedTypeVariable supertype, Void p) {
        return visitWildcardSubtype(subtype, supertype);
    }

    @Override
    public Boolean visitWildcard_Wildcard(
            AnnotatedWildcardType subtype, AnnotatedWildcardType supertype, Void p) {
        return visitWildcardSubtype(subtype, supertype);
    }

    // ------------------------------------------------------------------------
    // These "visit" methods are utility methods that aren't part of the visit
    // interface but that handle cases that more than one visit method shares
    // in commmon

    /** An intersection is a supertype if all of its bounds are a supertype of subtype. */
    protected boolean visitIntersectionSupertype(
            AnnotatedTypeMirror subtype, AnnotatedIntersectionType supertype) {
        if (visitHistory.contains(subtype, supertype, currentTop)) {
            return true;
        }
        boolean result = true;
        for (AnnotatedTypeMirror aSupertype : supertype.directSuperTypes()) {
            // Only call isSubtype if the Java type is actually a subtype; otherwise,
            // only check primary qualifiers.
            if (TypesUtils.isErasedSubtype(
                            subtype.getUnderlyingType(),
                            aSupertype.getUnderlyingType(),
                            subtype.atypeFactory.types)
                    && !isSubtype(subtype, aSupertype, currentTop)) {
                result = false;
                break;
            }
        }
        visitHistory.add(subtype, supertype, currentTop, result);
        return result;
    }

    /** A type variable is a supertype if its lower bound is above subtype. */
    protected boolean visitTypevarSupertype(
            AnnotatedTypeMirror subtype, AnnotatedTypeVariable supertype) {
        return checkAndSubtype(subtype, supertype.getLowerBound());
    }

    /**
     * A type variable is a subtype if its upper bounds is below the supertype. Note: When comparing
     * two type variables this method and visitTypevarSupertype will combine to isValid the subtypes
     * upper bound against the supertypes lower bound.
     */
    protected boolean visitTypevarSubtype(
            AnnotatedTypeVariable subtype, AnnotatedTypeMirror supertype) {
        AnnotatedTypeMirror upperBound = subtype.getUpperBound();
        if (TypesUtils.isBoxedPrimitive(upperBound.getUnderlyingType())
                && supertype instanceof AnnotatedPrimitiveType) {
            upperBound = supertype.atypeFactory.getUnboxedType((AnnotatedDeclaredType) upperBound);
        }
        if (supertype.getKind() == TypeKind.DECLARED
                && TypesUtils.getTypeElement(supertype.getUnderlyingType()).getKind()
                        == ElementKind.INTERFACE) {
            // Make sure the upper bound is no wildcard or type variable
            while (upperBound.getKind() == TypeKind.TYPEVAR
                    || upperBound.getKind() == TypeKind.WILDCARD) {
                if (upperBound.getKind() == TypeKind.TYPEVAR) {
                    upperBound = ((AnnotatedTypeVariable) upperBound).getUpperBound();
                }
                if (upperBound.getKind() == TypeKind.WILDCARD) {
                    upperBound = ((AnnotatedWildcardType) upperBound).getExtendsBound();
                }
            }
            // If the supertype is an interface, only compare the primary annotations.
            // The actual type argument could implement the interface and the bound of
            // the type variable must not implement the interface.
            if (upperBound.getKind() == TypeKind.INTERSECTION) {
                Types types = checker.getTypeUtils();
                for (AnnotatedTypeMirror ub :
                        ((AnnotatedIntersectionType) upperBound).directSuperTypes()) {
                    if (TypesUtils.isErasedSubtype(
                                    ub.getUnderlyingType(), supertype.getUnderlyingType(), types)
                            && isPrimarySubtype(ub, supertype)) {
                        return true;
                    }
                }
                return false;
            }
            return isPrimarySubtype(upperBound, supertype);
        }
        return checkAndSubtype(upperBound, supertype);
    }

    /** A union type is a subtype if ALL of its alternatives are subtypes of supertype. */
    protected boolean visitUnionSubtype(AnnotatedUnionType subtype, AnnotatedTypeMirror supertype) {
        return areAllSubtypes(subtype.getAlternatives(), supertype);
    }

    protected boolean visitWildcardSupertype(
            AnnotatedTypeMirror subtype, AnnotatedWildcardType supertype) {
        if (supertype.isUninferredTypeArgument()) { // TODO: REMOVE WHEN WE FIX TYPE ARG INFERENCE
            // Can't call isSubtype because underlying Java types won't be subtypes.
            return supertype.atypeFactory.ignoreUninferredTypeArguments;
        }
        return isSubtype(subtype, supertype.getSuperBound(), currentTop);
    }

    protected boolean visitWildcardSubtype(
            AnnotatedWildcardType subtype, AnnotatedTypeMirror supertype) {
        if (subtype.isUninferredTypeArgument()) {
            return subtype.atypeFactory.ignoreUninferredTypeArguments;
        }
        TypeMirror superTypeMirror = supertype.getUnderlyingType();
        if (supertype.getKind() == TypeKind.TYPEVAR) {
            TypeVariable atv = (TypeVariable) supertype.getUnderlyingType();
            if (TypesUtils.isCaptured(atv)) {
                superTypeMirror = TypesUtils.getCapturedWildcard(atv);
            }
        }

        if (superTypeMirror.getKind() == TypeKind.WILDCARD) {
            // This can happen at a method invocation where a type variable in the method
            // declaration is substituted with a wildcard.
            // For example:
            // <T> void method(Gen<T> t) {}
            // Gen<?> x;
            // method(x); // this method is called when checking this method call
            // And also when checking lambdas

            boolean subtypeHasAnno = subtype.getAnnotationInHierarchy(currentTop) != null;
            boolean supertypeHasAnno = supertype.getAnnotationInHierarchy(currentTop) != null;

            if (subtypeHasAnno && supertypeHasAnno) {
                // if both have primary annotations then just check the primary annotations
                // as the bounds are the same
                return isPrimarySubtype(subtype, supertype, true);

            } else if (!subtypeHasAnno
                    && !supertypeHasAnno
                    && areEqualInHierarchy(subtype, supertype)) {
                // TODO: wildcard capture conversion
                // Two unannotated uses of wildcard types are the same type
                return true;
            }
        }

        return isSubtype(subtype.getExtendsBound(), supertype, currentTop);
    }
}
