package org.checkerframework.framework.type;

import static org.checkerframework.framework.util.AnnotatedTypes.isDeclarationOfJavaLangEnum;
import static org.checkerframework.framework.util.AnnotatedTypes.isEnum;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
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
import org.checkerframework.framework.type.visitor.VisitHistory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.framework.util.TypeArgumentMapper;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Default implementation of TypeHierarchy that implements the JLS specification with minor
 * deviations as outlined by the Checker Framework manual. Changes to the JLS include forbidding
 * covariant array types, raw types, and allowing covariant type arguments depending on various
 * options passed to DefaultTypeHierarchy.
 *
 * <p>Subtyping rules of the JLS can be found in section 4.10, "Subtyping":
 * http://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.10
 *
 * <p>Note: The visit methods of this class must be public but it is intended to be used through a
 * TypeHierarchy interface reference which will only allow isSubtype to be called. It does not make
 * sense to call the visit methods on their own.
 */
public class DefaultTypeHierarchy extends AbstractAtmComboVisitor<Boolean, VisitHistory>
        implements TypeHierarchy {
    // used for processingEnvironment when needed
    protected final BaseTypeChecker checker;

    protected final QualifierHierarchy qualifierHierarchy;
    protected final StructuralEqualityComparer equalityComparer;
    protected final DefaultRawnessComparer rawnessComparer;

    protected final boolean ignoreRawTypes;
    protected final boolean invariantArrayComponents;

    //TODO: Incorporate feedback from David/Suzanne
    // IMPORTANT_NOTE:
    // For MultigraphQualifierHierarchies, we check the subtyping relationship of each annotation
    // hierarchy individually.
    // This is done because when comparing a pair of type variables, sometimes you need to traverse and
    // compare the bounds of two type variables.  Other times it is incorrect to compare the bounds.  These
    // two cases can occur simultaneously when comparing two hierarchies at once.  In this case,
    // comparing both hierarchies simultaneously will leadd ot an error.  More detail is given below.
    //
    // Recall, type variables may or may not have a primary annotation for each individual hierarchy.  When comparing
    // two type variables for a specific hierarchy we have five possible cases:
    //      case 1:  only one of the type variables has a primary annotation
    //      case 2a: both type variables have primary annotations and they are uses of the same type parameter
    //      case 2b: both type variables have primary annotations and they are uses of different type parameters
    //      case 3a: neither type variable has a primary annotation and they are uses of the same type parameter
    //      case 3b: neither type variable has a primary annotation and they are uses of different type parameters
    //
    // Case 1, 2b, and 3b require us to traverse both type variables bounds to ensure that the subtype's upper bound
    // is a subtype of the supertype's lower bound. Cases 2a requires only that we check that the primary annotation on
    // the subtype is a subtype of the primary annotation on the supertype.  In case 3a, we can just return true,
    // since two non-primary-annotated uses of the same type parameter are equivalent.  In this case it would be an
    // error to check the bounds because the check would only return true when the bounds are exact but it should
    // always return true.
    //
    // A problem occurs when, one hierarchy matches cases 1, 2b, or 3b and the other matches 3a.  In the first set of
    // cases we MUST check the type variables' bounds.  In case 3a we MUST NOT check the bounds.
    // e.g.
    //
    // Suppose I have a hierarchy with two tops @A1 and @B1.  Let @A0 <: @A1 and @B0 <: @B1.
    //  @A1 T t1;  T t2;
    //  t1 = t2;
    //
    // To typecheck "t1 = t2;" in the hierarchy topped by @A1, we need to descend into the bounds of t1 and t2 (where
    // t1's bounds will be overridden by @A1).  However, for hierarchy B we need only recognize that
    // since neither variable has a primary annotation, the types are equivalent and no traversal is needed.
    // If we tried to do these two actions simultaneously, in every visit and isSubtype call, we would have to
    // check to see that the @B hierarchy has been handled and ignore those annotations.
    //
    // Solutions:
    // We could handle this problem by keeping track of which hierarchies have already been taken care of.  We could
    // then check each hierarchy before making comparisons.  But this would lead to complicated plumbing that would be
    // hard to understand.
    // The chosen solution is to only check one hierarchy at a time.  One trade-off to this approach is that we have
    // to re-traverse the types for each hierarchy being checked.
    //
    // The field currentTop identifies the hierarchy for which the types are currently being checked.
    // Final note: all annotation comparisons are done via isPrimarySubtype, isBottom, and isAnnoSubtype
    // in order to ensure that we first get the annotations in the hierarchy of currentTop before
    // passing annotations to qualifierHierarchy.
    protected AnnotationMirror currentTop;

    public DefaultTypeHierarchy(
            final BaseTypeChecker checker,
            final QualifierHierarchy qualifierHierarchy,
            boolean ignoreRawTypes,
            boolean invariantArrayComponents) {
        this.checker = checker;
        this.qualifierHierarchy = qualifierHierarchy;
        this.rawnessComparer = createRawnessComparer();
        this.equalityComparer = createEqualityComparer();

        this.ignoreRawTypes = ignoreRawTypes;
        this.invariantArrayComponents = invariantArrayComponents;
    }

    public DefaultRawnessComparer createRawnessComparer() {
        return new DefaultRawnessComparer(this);
    }

    public StructuralEqualityComparer createEqualityComparer() {
        return new StructuralEqualityComparer(rawnessComparer);
    }

    /**
     * Returns true if subtype {@literal <:} supertype
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
     * Returns true if subtype {@literal <:} supertype
     *
     * @param subtype expected subtype
     * @param supertype expected supertype
     * @param top the hierarchy for which we want to make a comparison
     * @return true if subtype is actually a subtype of supertype
     */
    @Override
    public boolean isSubtype(
            final AnnotatedTypeMirror subtype,
            final AnnotatedTypeMirror supertype,
            final AnnotationMirror top) {
        currentTop = top;
        return isSubtype(subtype, supertype, new VisitHistory());
    }

    /**
     * Calls is subtype pair-wise on the elements of the subtypes/supertypes Iterable.
     *
     * @return true if for each pair, the subtype element is a subtype of the supertype element. An
     *     exception will be thrown if the iterables are of different sizes.
     */
    public boolean areSubtypes(
            final Iterable<? extends AnnotatedTypeMirror> subtypes,
            final Iterable<? extends AnnotatedTypeMirror> supertypes) {
        final Iterator<? extends AnnotatedTypeMirror> subtypeIterator = subtypes.iterator();
        final Iterator<? extends AnnotatedTypeMirror> supertypesIterator = supertypes.iterator();

        while (subtypeIterator.hasNext() && supertypesIterator.hasNext()) {
            final AnnotatedTypeMirror subtype = subtypeIterator.next();
            final AnnotatedTypeMirror supertype = supertypesIterator.next();

            if (!isSubtype(subtype, supertype)) {
                return false;
            }
        }

        if (subtypeIterator.hasNext() || supertypesIterator.hasNext()) {
            ErrorReporter.errorAbort(
                    "Unbalanced set of type arguments.\n"
                            + "  subtype=( "
                            + PluginUtil.join(", ", subtypes)
                            + ")\n"
                            + "  supertype=( "
                            + PluginUtil.join(", ", supertypes)
                            + ")");
        }

        return true;
    }

    /** @return error message for the case when two types shouldn't be compared */
    @Override
    protected String defaultErrorMessage(
            final AnnotatedTypeMirror subtype,
            final AnnotatedTypeMirror supertype,
            final VisitHistory visited) {
        return "Incomparable types ( "
                + subtype
                + ", "
                + supertype
                + ")"
                + "visitHistory = "
                + visited;
    }

    /**
     * Returns true if subtype {@literal <:} supertype. The only difference between this and
     * isSubtype(subtype, supertype) is that this method passes a pre-existing visited
     *
     * @param subtype expected subtype
     * @param supertype expected supertype
     * @return true if subtype is actually a subtype of supertype
     */
    public boolean isSubtype(
            final AnnotatedTypeMirror subtype,
            final AnnotatedTypeMirror supertype,
            VisitHistory visited) {
        return AtmCombo.accept(subtype, supertype, visited, this);
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
     * false is returned
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

                //TODO: DO ANYTHING SPECIAL FOR INTERSECTIONS OR UNIONS?
                //TODO: ENUMERATE THE VALID CASES?

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
            final AnnotatedTypeMirror subtype,
            final AnnotatedTypeMirror supertype,
            VisitHistory visited) {
        if (visited.contains(subtype, supertype)) {
            return true;
        }

        visited.add(subtype, supertype);
        return isSubtype(subtype, supertype, visited);
    }

    protected boolean isSubtypeOfAll(
            final AnnotatedTypeMirror subtype,
            final Iterable<? extends AnnotatedTypeMirror> supertypes,
            final VisitHistory visited) {
        for (final AnnotatedTypeMirror supertype : supertypes) {
            if (!isSubtype(subtype, supertype, visited)) {
                return false;
            }
        }

        return true;
    }

    protected boolean areAllSubtypes(
            final Iterable<? extends AnnotatedTypeMirror> subtypes,
            final AnnotatedTypeMirror supertype,
            final VisitHistory visited) {
        for (final AnnotatedTypeMirror subtype : subtypes) {
            if (!isSubtype(subtype, supertype, visited)) {
                return false;
            }
        }

        return true;
    }

    protected boolean areEqual(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        return equalityComparer.areEqual(type1, type2);
    }

    protected boolean areEqualInHierarchy(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            final AnnotationMirror top) {
        return equalityComparer.areEqualInHierarchy(type1, type2, top);
    }

    /**
     * A declared type is considered a supertype of another declared type only if all of the type
     * arguments of the declared type "contain" the corresponding type arguments of the subtype.
     * Containment is described in the JLS section 4.5.1 "Type Arguments of Parameterized Types",
     * http://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.5.1
     *
     * @param inside the "subtype" type argument
     * @param outside the "supertype" type argument
     * @param visited a history of type pairs that have been visited, used to halt on recursive
     *     bounds
     * @param canBeCovariant whether or not type arguments are allowed to be covariant
     * @return true if inside is contained by outside OR, if canBeCovariant == true, inside is a
     *     subtype of outside
     */
    protected boolean isContainedBy(
            final AnnotatedTypeMirror inside,
            final AnnotatedTypeMirror outside,
            VisitHistory visited,
            boolean canBeCovariant) {
        if (ignoreUninferredTypeArgument(inside) || ignoreUninferredTypeArgument(outside)) {
            return true;
        }

        if (canBeCovariant && isSubtype(inside, outside, visited)) {
            return true;
        }

        if (outside.getKind() == TypeKind.WILDCARD) {
            final AnnotatedWildcardType outsideWc = (AnnotatedWildcardType) outside;

            if (!checkAndSubtype(outsideWc.getSuperBound(), inside, visited)) {
                return false;
            }

            AnnotatedTypeMirror outsideWcUB = outsideWc.getExtendsBound();
            if (inside.getKind() == TypeKind.WILDCARD) {
                outsideWcUB =
                        checker.getTypeFactory()
                                .widenToUpperBound(outsideWcUB, (AnnotatedWildcardType) inside);
            }
            while (outsideWcUB.getKind() == TypeKind.WILDCARD) {
                outsideWcUB = ((AnnotatedWildcardType) outsideWcUB).getExtendsBound();
            }

            AnnotatedTypeMirror castedInside = castedAsSuper(inside, outsideWcUB);
            return checkAndSubtype(castedInside, outsideWcUB, visited);
        } else { //TODO: IF WE NEED TO COMPARE A WILDCARD TO A CAPTURE OF A WILDCARD WE FAIL IN ARE_EQUAL -> DO CAPTURE CONVERSION
            return areEqualInHierarchy(inside, outside, currentTop);
        }
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

    //------------------------------------------------------------------------
    // Arrays as subtypes

    @Override
    public Boolean visitArray_Array(
            AnnotatedArrayType subtype, AnnotatedArrayType supertype, VisitHistory visited) {
        return isPrimarySubtype(subtype, supertype)
                && (invariantArrayComponents
                        ? areEqualInHierarchy(
                                subtype.getComponentType(),
                                supertype.getComponentType(),
                                currentTop)
                        : isSubtype(
                                subtype.getComponentType(), supertype.getComponentType(), visited));
    }

    @Override
    public Boolean visitArray_Declared(
            AnnotatedArrayType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitArray_Null(
            AnnotatedArrayType subtype, AnnotatedNullType supertype, VisitHistory visited) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitArray_Intersection(
            AnnotatedArrayType subtype,
            AnnotatedIntersectionType supertype,
            VisitHistory visitHistory) {
        return isSubtype(castedAsSuper(subtype, supertype), supertype);
    }

    @Override
    public Boolean visitArray_Wildcard(
            AnnotatedArrayType subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    //------------------------------------------------------------------------
    // Declared as subtype
    @Override
    public Boolean visitDeclared_Array(
            AnnotatedDeclaredType subtype, AnnotatedArrayType supertype, VisitHistory visited) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitDeclared_Declared(
            AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        AnnotatedDeclaredType subtypeAsSuper = castedAsSuper(subtype, supertype);

        if (!isPrimarySubtype(subtypeAsSuper, supertype)) {
            return false;
        }

        if (visited.contains(subtypeAsSuper, supertype)) {
            return true;
        }

        visited.add(subtypeAsSuper, supertype);
        final Boolean result =
                visitTypeArgs(
                        subtypeAsSuper, supertype, visited, subtype.wasRaw(), supertype.wasRaw());

        return result;
    }

    /**
     * A helper class for visitDeclared_Declared. There are subtypes of DefaultTypeHierarchy that
     * need to customize the handling of type arguments. This method provides a convenient extension
     * point.
     */
    public Boolean visitTypeArgs(
            final AnnotatedDeclaredType subtype,
            final AnnotatedDeclaredType supertype,
            final VisitHistory visited,
            final boolean subtypeRaw,
            final boolean supertypeRaw) {

        final boolean ignoreTypeArgs = ignoreRawTypes && (subtypeRaw || supertypeRaw);

        if (!ignoreTypeArgs) {
            final List<? extends AnnotatedTypeMirror> subtypeTypeArgs = subtype.getTypeArguments();
            final List<? extends AnnotatedTypeMirror> supertypeTypeArgs =
                    supertype.getTypeArguments();

            //TODO: IN THE ORIGINAL TYPE_HIERARCHY WE ALWAYS RETURN TRUE IF ONE OF THE LISTS IS EMPTY
            //TODO: THIS SEEMS LIKE WE SHOULD ONLY RETURN TRUE HERE IF ignoreRawTypes == TRUE OR IF BOTH ARE EMPTY
            //TODO: ARE WE MORE STRICT THAN JAVAC OR DO WE WANT TO FOLLOW JAVAC RAW TYPES SEMANTICS?
            if (subtypeTypeArgs.isEmpty() || supertypeTypeArgs.isEmpty()) {
                return true;
            }

            final TypeElement supertypeElem =
                    (TypeElement) supertype.getUnderlyingType().asElement();
            List<Integer> covariantArgIndexes = null;
            AnnotationMirror covam =
                    supertype.atypeFactory.getDeclAnnotation(supertypeElem, Covariant.class);

            if (covam == null) {
                // Fall back to deprecated Nullness Checker version of the annotation.
                // This should be removed once that version is removed.
                // Using String instead of .class to prevent dependency.
                try {
                    @SuppressWarnings({"unchecked", "LiteralClassName"})
                    Class<? extends Annotation> nncov =
                            (Class<? extends Annotation>)
                                    Class.forName(
                                            "org.checkerframework.checker.nullness.qual.Covariant");
                    covam = supertype.atypeFactory.getDeclAnnotation(supertypeElem, nncov);
                } catch (ClassNotFoundException ex) {
                    covam = null;
                }
            }

            if (covam != null) {
                covariantArgIndexes =
                        AnnotationUtils.getElementValueArray(covam, "value", Integer.class, false);
            }

            for (int i = 0; i < supertypeTypeArgs.size(); i++) {
                final AnnotatedTypeMirror superTypeArg = supertypeTypeArgs.get(i);
                final AnnotatedTypeMirror subTypeArg = subtypeTypeArgs.get(i);
                final boolean covariant =
                        covariantArgIndexes != null && covariantArgIndexes.contains(i);

                if (!compareTypeArgs(
                        subTypeArg, superTypeArg, supertypeRaw, subtypeRaw, covariant, visited)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Compare typeArgs is called on a single pair of type args that should share a relationship
     * subTypeArg {@literal <:} superTypeArg (subtypeArg is contained by superTypeArg). However, if
     * either type is raw then either (subTypeArg {@literal <:} superTypeArg) or the
     * rawnessComparer.isValid(superTypeArg, subTypeArg, visited)
     */
    protected boolean compareTypeArgs(
            AnnotatedTypeMirror subTypeArg,
            AnnotatedTypeMirror superTypeArg,
            boolean subtypeRaw,
            boolean supertypeRaw,
            boolean isCovariant,
            VisitHistory visited) {
        if (subtypeRaw || supertypeRaw) {
            return rawnessComparer.isValidInHierarchy(subTypeArg, superTypeArg, currentTop, visited)
                    || isContainedBy(subTypeArg, superTypeArg, visited, isCovariant);
        } else {
            return isContainedBy(subTypeArg, superTypeArg, visited, isCovariant);
        }
    }

    @Override
    public Boolean visitDeclared_Intersection(
            AnnotatedDeclaredType subtype,
            AnnotatedIntersectionType supertype,
            VisitHistory visited) {
        return visitIntersectionSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitDeclared_Null(
            AnnotatedDeclaredType subtype, AnnotatedNullType supertype, VisitHistory visited) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitDeclared_Primitive(
            AnnotatedDeclaredType subtype, AnnotatedPrimitiveType supertype, VisitHistory visited) {
        // We do an asSuper first because in some cases unboxing implies a more specific annotation
        // e.g. @UnknownInterned Integer => @Interned int  because primitives are always interned
        final AnnotatedPrimitiveType subAsSuper = castedAsSuper(subtype, supertype);
        if (subAsSuper == null) {
            return isPrimarySubtype(subtype, supertype);
        }
        return isPrimarySubtype(subAsSuper, supertype);
    }

    @Override
    public Boolean visitDeclared_Typevar(
            AnnotatedDeclaredType subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitDeclared_Union(
            AnnotatedDeclaredType subtype, AnnotatedUnionType supertype, VisitHistory visited) {
        Types types = checker.getTypeUtils();
        for (AnnotatedDeclaredType supertypeAltern : supertype.getAlternatives()) {
            if (TypesUtils.isErasedSubtype(
                            types, subtype.getUnderlyingType(), supertypeAltern.getUnderlyingType())
                    && isSubtype(subtype, supertypeAltern)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitDeclared_Wildcard(
            AnnotatedDeclaredType subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    //------------------------------------------------------------------------
    // Intersection as subtype
    @Override
    public Boolean visitIntersection_Declared(
            AnnotatedIntersectionType subtype,
            AnnotatedDeclaredType supertype,
            VisitHistory visited) {
        for (AnnotatedDeclaredType subtypeI : subtype.directSuperTypes()) {
            Types types = checker.getTypeUtils();
            if (TypesUtils.isErasedSubtype(
                            types, subtypeI.getUnderlyingType(), supertype.getUnderlyingType())
                    && isSubtype(subtypeI, supertype)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitIntersection_Primitive(
            AnnotatedIntersectionType subtype,
            AnnotatedPrimitiveType supertype,
            VisitHistory visited) {
        for (AnnotatedDeclaredType subtypeI : subtype.directSuperTypes()) {
            if (TypesUtils.isBoxedPrimitive(subtypeI.getUnderlyingType())
                    && isSubtype(subtypeI, supertype)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitIntersection_Intersection(
            AnnotatedIntersectionType subtype,
            AnnotatedIntersectionType supertype,
            VisitHistory visited) {
        Types types = checker.getTypeUtils();
        for (AnnotatedDeclaredType subtypeI : subtype.directSuperTypes()) {
            for (AnnotatedDeclaredType supertypeI : supertype.directSuperTypes()) {
                if (TypesUtils.isErasedSubtype(
                                types, subtypeI.getUnderlyingType(), supertypeI.getUnderlyingType())
                        && !isSubtype(subtypeI, supertypeI)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Boolean visitIntersection_Null(
            AnnotatedIntersectionType subtype, AnnotatedNullType supertype, VisitHistory visited) {
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
            AnnotatedIntersectionType subtype,
            AnnotatedTypeVariable supertype,
            VisitHistory visited) {
        // this can occur through capture conversion/comparing bounds
        for (AnnotatedDeclaredType subtypeI : subtype.directSuperTypes()) {
            Types types = checker.getTypeUtils();
            if (TypesUtils.isErasedSubtype(
                            types, subtypeI.getUnderlyingType(), supertype.getUnderlyingType())
                    && isSubtype(subtypeI, supertype)) {
                return true;
            }
        }
        return false;
    }

    //------------------------------------------------------------------------
    // Null as subtype
    @Override
    public Boolean visitNull_Array(
            AnnotatedNullType subtype, AnnotatedArrayType supertype, VisitHistory visited) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitNull_Declared(
            AnnotatedNullType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitNull_Typevar(
            AnnotatedNullType subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return visitTypevarSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitNull_Wildcard(
            AnnotatedNullType subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitNull_Null(
            AnnotatedNullType subtype, AnnotatedNullType supertype, VisitHistory visited) {
        // this can occur when comparing typevar lower bounds since they are usually null types
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitNull_Union(
            AnnotatedNullType subtype, AnnotatedUnionType supertype, VisitHistory visited) {
        for (AnnotatedDeclaredType supertypeAltern : supertype.getAlternatives()) {
            if (isSubtype(subtype, supertypeAltern)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitNull_Intersection(
            AnnotatedNullType subtype, AnnotatedIntersectionType supertype, VisitHistory visited) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitNull_Primitive(
            AnnotatedNullType subtype, AnnotatedPrimitiveType supertype, VisitHistory visited) {
        return isPrimarySubtype(subtype, supertype);
    }

    //------------------------------------------------------------------------
    // Primitive as subtype
    @Override
    public Boolean visitPrimitive_Declared(
            AnnotatedPrimitiveType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        // see comment in visitDeclared_Primitive
        final AnnotatedDeclaredType subAsSuper = castedAsSuper(subtype, supertype);
        if (subAsSuper == null) {
            return isPrimarySubtype(subtype, supertype);
        }
        return isPrimarySubtype(subAsSuper, supertype);
    }

    @Override
    public Boolean visitPrimitive_Primitive(
            AnnotatedPrimitiveType subtype,
            AnnotatedPrimitiveType supertype,
            VisitHistory visited) {
        return isPrimarySubtype(subtype, supertype);
    }

    @Override
    public Boolean visitPrimitive_Intersection(
            AnnotatedPrimitiveType subtype,
            AnnotatedIntersectionType supertype,
            VisitHistory visited) {
        return visitIntersectionSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitPrimitive_Wildcard(
            AnnotatedPrimitiveType subtype,
            AnnotatedWildcardType supertype,
            VisitHistory visitHistory) {
        if (supertype.atypeFactory.ignoreUninferredTypeArguments
                && supertype.isUninferredTypeArgument()) {
            return true;
        }
        // this can occur when passing a primitive to a method on a raw type (see test checker/tests/nullness/RawAndPrimitive.java)
        // this can also occur because we don't box primitives when we should and don't capture convert
        return isPrimarySubtype(subtype, supertype.getSuperBound());
    }

    //------------------------------------------------------------------------
    // Union as subtype
    @Override
    public Boolean visitUnion_Declared(
            AnnotatedUnionType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        return visitUnionSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitUnion_Intersection(
            AnnotatedUnionType subtype, AnnotatedIntersectionType supertype, VisitHistory visited) {
        // <T extends Throwable & Cloneable> void method(T param) {}
        // ...
        // catch (Exception1 | Exception2 union) { // Assuming Exception1 and Exception2 implement Cloneable
        //   method(union);
        // This case happens when checking that the inferred type argument is a subtype of the declared type argument of method.
        // See org.checkerframework.common.basetype.BaseTypeVisitor#checkTypeArguments
        return visitUnionSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitUnion_Union(
            AnnotatedUnionType subtype, AnnotatedUnionType supertype, VisitHistory visited) {
        // <T> void method(T param) {}
        // ...
        // catch (Exception1 | Exception2 union) {
        //   method(union);
        // This case happens when checking the arguments to method after type variable substitution
        return visitUnionSubtype(subtype, supertype, visited);
    }

    //------------------------------------------------------------------------
    // typevar as subtype
    @Override
    public Boolean visitTypevar_Declared(
            AnnotatedTypeVariable subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
        return visitTypevarSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Intersection(
            AnnotatedTypeVariable subtype,
            AnnotatedIntersectionType supertype,
            VisitHistory visited) {
        // this can happen when checking type param bounds
        return visitIntersectionSupertype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Primitive(
            AnnotatedTypeVariable subtype, AnnotatedPrimitiveType supertype, VisitHistory visited) {
        return visitTypevarSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Typevar(
            AnnotatedTypeVariable subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {

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
                    && areEqualInHierarchy(subtype, supertype, currentTop)) {
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
                return visit(subtype.getUpperBound(), supertype.getLowerBound(), visited);
            }
        }

        if (AnnotatedTypes.areCorrespondingTypeVariables(
                checker.getProcessingEnvironment().getElementUtils(), subtype, supertype)) {
            if (areEqualInHierarchy(subtype, supertype, currentTop)) {
                return true;
            }
        }

        // check that the upper bound of the subtype is below the lower bound of the supertype
        return visitTypevarSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Null(
            AnnotatedTypeVariable subtype, AnnotatedNullType supertype, VisitHistory visited) {
        return visitTypevarSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitTypevar_Wildcard(
            AnnotatedTypeVariable subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSupertype(subtype, supertype, visited);
    }

    //------------------------------------------------------------------------
    // wildcard as subtype

    @Override
    public Boolean visitWildcard_Array(
            AnnotatedWildcardType subtype, AnnotatedArrayType supertype, VisitHistory visited) {
        return visitWildcardSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitWildcard_Declared(
            AnnotatedWildcardType subtype, AnnotatedDeclaredType supertype, VisitHistory visited) {
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
                // @NullableList<@NonNull String> then it's not possible to decide if it is a subtype of
                // the wildcard.
                AnnotationMirror subtypeAnno =
                        subtype.getEffectiveAnnotationInHierarchy(currentTop);
                AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(currentTop);
                return isAnnoSubtype(subtypeAnno, supertypeAnno, false);
            }
        }
        return visitWildcardSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitWildcard_Intersection(
            AnnotatedWildcardType subtype,
            AnnotatedIntersectionType supertype,
            VisitHistory visited) {
        return visitWildcardSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitWildcard_Primitive(
            AnnotatedWildcardType subtype, AnnotatedPrimitiveType supertype, VisitHistory visited) {
        if (subtype.isUninferredTypeArgument()) {
            AnnotationMirror subtypeAnno = subtype.getEffectiveAnnotationInHierarchy(currentTop);
            AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(currentTop);
            return isAnnoSubtype(subtypeAnno, supertypeAnno, false);
        }
        return visitWildcardSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitWildcard_Typevar(
            AnnotatedWildcardType subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return visitWildcardSubtype(subtype, supertype, visited);
    }

    @Override
    public Boolean visitWildcard_Wildcard(
            AnnotatedWildcardType subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        return visitWildcardSubtype(subtype, supertype, visited);
    }

    //------------------------------------------------------------------------
    // These "visit" methods are utility methods that aren't part of the visit
    // interface but that handle cases that more than one visit method shares
    // in commmon

    /** An intersection is a supertype if all of its bounds are a supertype of subtype */
    protected boolean visitIntersectionSupertype(
            AnnotatedTypeMirror subtype,
            AnnotatedIntersectionType supertype,
            VisitHistory visited) {
        if (visited.contains(subtype, supertype)) {
            return true;
        }
        visited.add(subtype, supertype);
        return isSubtypeOfAll(subtype, supertype.directSuperTypes(), visited);
    }

    /** A type variable is a supertype if its lower bound is above subtype. */
    protected boolean visitTypevarSupertype(
            AnnotatedTypeMirror subtype, AnnotatedTypeVariable supertype, VisitHistory visited) {
        return checkAndSubtype(subtype, supertype.getLowerBound(), visited);
    }

    /**
     * A type variable is a subtype if its upper bounds is below the supertype. Note: When comparing
     * two type variables this method and visitTypevarSupertype will combine to isValid the subtypes
     * upper bound against the supertypes lower bound.
     */
    protected boolean visitTypevarSubtype(
            AnnotatedTypeVariable subtype, AnnotatedTypeMirror supertype, VisitHistory visited) {
        AnnotatedTypeMirror upperBound = subtype.getUpperBound();
        if (TypesUtils.isBoxedPrimitive(upperBound.getUnderlyingType())
                && supertype instanceof AnnotatedPrimitiveType) {
            upperBound = supertype.atypeFactory.getUnboxedType((AnnotatedDeclaredType) upperBound);
        }
        return checkAndSubtype(upperBound, supertype, visited);
    }

    /** A union type is a subtype if ALL of its alternatives are subtypes of supertype */
    protected Boolean visitUnionSubtype(
            AnnotatedUnionType subtype, AnnotatedTypeMirror supertype, VisitHistory visited) {
        return areAllSubtypes(subtype.getAlternatives(), supertype, visited);
    }

    protected boolean visitWildcardSupertype(
            AnnotatedTypeMirror subtype, AnnotatedWildcardType supertype, VisitHistory visited) {
        if (supertype.isUninferredTypeArgument()) { //TODO: REMOVE WHEN WE FIX TYPE ARG INFERENCE
            // Can't call isSubtype because underlying Java types won't be subtypes.
            return supertype.atypeFactory.ignoreUninferredTypeArguments;
        }
        return isSubtype(subtype, supertype.getSuperBound(), visited);
    }

    protected boolean visitWildcardSubtype(
            AnnotatedWildcardType subtype, AnnotatedTypeMirror supertype, VisitHistory visited) {
        if (subtype.isUninferredTypeArgument()) {
            return subtype.atypeFactory.ignoreUninferredTypeArguments;
        }
        TypeMirror superTypeMirror = supertype.getUnderlyingType();
        if (supertype.getKind() == TypeKind.TYPEVAR) {
            TypeVariable atv = (TypeVariable) supertype.getUnderlyingType();
            if (InternalUtils.isCaptured(atv)) {
                superTypeMirror = InternalUtils.getCapturedWildcard(atv);
            }
        }

        if (subtype.getUnderlyingType() == superTypeMirror) {
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
                    && areEqualInHierarchy(subtype, supertype, currentTop)) {
                // TODO: wildcard capture conversion
                // Two unannotated uses of reference-equal wildcard types are the same type
                return true;
            }
        }

        return isSubtype(subtype.getExtendsBound(), supertype, visited);
    }

    /**
     * Calls asSuper and casts the result to the same type as the input supertype
     *
     * @param subtype subtype to be transformed to supertype
     * @param supertype supertype that subtype is transformed to
     * @param <T> the type of supertype and return type
     * @return subtype as an instance of supertype
     */
    @SuppressWarnings("unchecked")
    public static <T extends AnnotatedTypeMirror> T castedAsSuper(
            final AnnotatedTypeMirror subtype, final T supertype) {
        final Types types = subtype.atypeFactory.getProcessingEnv().getTypeUtils();
        final Elements elements = subtype.atypeFactory.getProcessingEnv().getElementUtils();

        if (subtype.getKind() == TypeKind.NULL) {
            // Make a copy of the supertype so that if supertype is a composite type, the
            // returned type will be fully annotated.  (For example, if sub is @C null and super is
            // @A List<@B String>, then the returned type is @C List<@B String>.)
            T copy = (T) supertype.deepCopy();
            copy.replaceAnnotations(subtype.getAnnotations());
            return copy;
        }

        final T asSuperType = AnnotatedTypes.asSuper(subtype.atypeFactory, subtype, supertype);

        fixUpRawTypes(subtype, asSuperType, supertype, types);

        // if we have a type for enum MyEnum {...}
        // When the supertype is the declaration of java.lang.Enum<E>, MyEnum values become
        // Enum<MyEnum>.  Where really, we would like an Enum<E> with the annotations from Enum<MyEnum>
        // are transferred to Enum<E>.  That is, if we have a type:
        // @1 Enum<@2 MyEnum>
        // asSuper should return:
        // @1 Enum<@2 E>
        if (asSuperType != null
                && isEnum(asSuperType)
                && isDeclarationOfJavaLangEnum(types, elements, supertype)) {
            final AnnotatedDeclaredType resultAtd = ((AnnotatedDeclaredType) supertype).deepCopy();
            resultAtd.clearAnnotations();
            resultAtd.addAnnotations(asSuperType.getAnnotations());

            final AnnotatedDeclaredType asSuperAdt = (AnnotatedDeclaredType) asSuperType;
            if (resultAtd.getTypeArguments().size() > 0
                    && asSuperAdt.getTypeArguments().size() > 0) {
                final AnnotatedTypeMirror sourceTypeArg = asSuperAdt.getTypeArguments().get(0);
                final AnnotatedTypeMirror resultTypeArg = resultAtd.getTypeArguments().get(0);
                resultTypeArg.clearAnnotations();
                resultTypeArg.addAnnotations(sourceTypeArg.getAnnotations());
                return (T) resultAtd;
            }
        }
        return asSuperType;
    }

    /**
     * Some times we create type arguments for types that were raw. When we do an asSuper we lose
     * these arguments. If in the converted type (i.e. the subtype as super) is missing type
     * arguments AND those type arguments should come from the original subtype's type arguments
     * then we copy the original type arguments to the converted type. e.g. We have a type W, that
     * "wasRaw" {@code ArrayList<? extends Object>} When W is converted to type A, List, using
     * asSuper it no longer has its type argument. But since the type argument to List should be the
     * same as that to ArrayList we copy over the type argument of W to A. A becomes {@code List<?
     * extends Object>}
     *
     * @param originalSubtype the subtype before being converted by asSuper
     * @param asSuperType he subtype after being converted by asSuper
     * @param supertype the supertype for which asSuperType should have the same underlying type
     * @param types the types utility
     */
    private static void fixUpRawTypes(
            final AnnotatedTypeMirror originalSubtype,
            final AnnotatedTypeMirror asSuperType,
            final AnnotatedTypeMirror supertype,
            final Types types) {
        if (asSuperType != null
                && asSuperType.getKind() == TypeKind.DECLARED
                && originalSubtype.getKind() == TypeKind.DECLARED) {
            final AnnotatedDeclaredType declaredAsSuper = (AnnotatedDeclaredType) asSuperType;
            final AnnotatedDeclaredType declaredSubtype = (AnnotatedDeclaredType) originalSubtype;

            if (declaredAsSuper.wasRaw()
                    && declaredAsSuper.getTypeArguments().isEmpty()
                    && !declaredSubtype.getTypeArguments().isEmpty()) {

                Set<Pair<Integer, Integer>> typeArgMap =
                        TypeArgumentMapper.mapTypeArgumentIndices(
                                (TypeElement) declaredSubtype.getUnderlyingType().asElement(),
                                (TypeElement) declaredAsSuper.getUnderlyingType().asElement(),
                                types);

                if (typeArgMap.size() == declaredSubtype.getTypeArguments().size()) {

                    List<AnnotatedTypeMirror> newTypeArgs = new ArrayList<AnnotatedTypeMirror>();

                    List<Pair<Integer, Integer>> orderedByDestination = new ArrayList<>(typeArgMap);
                    Collections.sort(
                            orderedByDestination,
                            new Comparator<Pair<Integer, Integer>>() {
                                @Override
                                public int compare(
                                        Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
                                    return o1.second - o2.second;
                                }
                            });

                    final List<? extends AnnotatedTypeMirror> subTypeArgs =
                            declaredSubtype.getTypeArguments();
                    if (typeArgMap.size()
                            == ((AnnotatedDeclaredType) supertype).getTypeArguments().size()) {
                        for (Pair<Integer, Integer> mapping : orderedByDestination) {
                            newTypeArgs.add(subTypeArgs.get(mapping.first).deepCopy());
                        }
                    }
                    declaredAsSuper.setTypeArguments(newTypeArgs);
                }
            }
        }
    }
}
