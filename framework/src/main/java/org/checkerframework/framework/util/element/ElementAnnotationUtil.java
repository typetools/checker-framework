package org.checkerframework.framework.util.element;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntryKind;

import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.ElementAnnotationApplier;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.plumelib.util.StringsPlume;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

/**
 * Utility methods for adding the annotations that are stored in an Element to the type that
 * represents that element (or a use of that Element). This class also contains package private
 * methods used by the ElementAnnotationAppliers that do most of the work.
 */
public class ElementAnnotationUtil {

    /**
     * For each type/element pair, add all of the annotations stored in Element to type. See apply
     * for more details.
     *
     * @param types the types to which we wish to apply element annotations
     * @param elements the elements that may contain annotations to apply. elements.size must ==
     *     types.size
     * @param typeFactory the type factory used to create the AnnotatedTypeMirrors contained by
     *     types
     */
    public static void applyAllElementAnnotations(
            final List<? extends AnnotatedTypeMirror> types,
            final List<? extends Element> elements,
            final AnnotatedTypeFactory typeFactory) {

        if (types.size() != elements.size()) {
            throw new BugInCF(
                    "Number of types and elements don't match. "
                            + "types ( "
                            + StringsPlume.join(", ", types)
                            + " ) "
                            + "element ( "
                            + StringsPlume.join(", ", elements)
                            + " ) ");
        }

        for (int i = 0; i < types.size(); i++) {
            ElementAnnotationApplier.apply(types.get(i), elements.get(i), typeFactory);
        }
    }

    /**
     * When a declaration annotation is an alias for a type annotation, then the Checker Framework
     * may move the annotation before replacing it by the canonical version.
     *
     * <p>If the annotation is one of the Checker Framework compatibility annotations, for example
     * org.checkerframework.checker.nullness.compatqual.NonNullDecl, then it is interpreted as a
     * type annotation in the same location.
     *
     * @param type the type to annotate
     * @param annotations the annotations to add
     */
    @SuppressWarnings("interning:not.interned") // AST node comparison
    static void addDeclarationAnnotationsFromElement(
            final AnnotatedTypeMirror type, final List<? extends AnnotationMirror> annotations) {
        // The code here should be similar to
        // org.checkerframework.framework.type.TypeFromMemberVisitor.visitVariable
        AnnotatedTypeMirror innerType = AnnotatedTypes.innerMostType(type);
        if (innerType != type) {
            for (AnnotationMirror annotation : annotations) {
                if (AnnotationUtils.annotationName(annotation).startsWith("org.checkerframework")) {
                    innerType.addAnnotation(annotation);
                } else {
                    type.addAnnotation(annotation);
                }
            }
        } else {
            type.addAnnotations(annotations);
        }
    }

    /**
     * Does expectedValues contain enumValue. This is just a linear search.
     *
     * @param enumValue value to search for, a needle
     * @param expectedValues values to search through, a haystack
     * @return true if enumValue is in expectedValues, false otherwise
     */
    static boolean contains(Object enumValue, Object[] expectedValues) {
        for (final Object expected : expectedValues) {
            if (enumValue.equals(expected)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Use a map to partition annotations with the given TargetTypes into Lists, where each target
     * type is a key in the output map. Any annotation that does not have one of these target types
     * will be added to unmatched
     *
     * @param annos the collection of annotations to partition
     * @param unmatched a list to add annotations with unmatched target types to
     * @param targetTypes a list of target types to partition annos with
     * @return a map from targetType &rarr; List of Annotations that have that targetType
     */
    static Map<TargetType, List<TypeCompound>> partitionByTargetType(
            Collection<TypeCompound> annos,
            List<TypeCompound> unmatched,
            TargetType... targetTypes) {
        final Map<TargetType, List<TypeCompound>> targetTypeToAnnos = new HashMap<>();
        for (TargetType targetType : targetTypes) {
            targetTypeToAnnos.put(targetType, new ArrayList<>());
        }

        for (final TypeCompound anno : annos) {
            final List<TypeCompound> annoSet = targetTypeToAnnos.get(anno.getPosition().type);
            if (annoSet != null) {
                annoSet.add(anno);
            } else if (unmatched != null) {
                unmatched.add(anno);
            }
        }

        return targetTypeToAnnos;
    }

    /**
     * A class used solely to annotate wildcards from Element annotations. Instances of
     * WildcardBoundAnnos are used to aggregate ALL annotations for a given Wildcard and then apply
     * them all at once in order to resolve the annotations in front of unbound wildcards.
     *
     * <p>Wildcard annotations are applied as follows:
     *
     * <ul>
     *   <li>a) If an Annotation is in front of a extends or super bounded wildcard, it applies to
     *       the bound that is NOT explicitly present. e.g.
     *       <pre>{@code
     * <@A ? extends Object> -- @A is placed on the super bound (Void)
     * <@B ? super CharSequence> -- @B is placed on the extends bound (probably Object)
     * }</pre>
     *   <li>b) If an Annotation is on a bound, it applies to that bound. E.g.
     *       <pre>{@code
     * <? extends @A Object> -- @A is placed on the extends bound (Object)
     * <? super @B CharSequence> -- @B is placed on the super bound (CharSequence)
     * }</pre>
     *   <li>c) If an Annotation is on an unbounded wildcard there are two subcases.
     *       <ul>
     *         <li>c.1 The user wrote the annotation explicitly -- these annotations apply to both
     *             bounds e.g. the user wrote
     *             <pre>{@code
     * <@C ?> -- the annotation is placed on the extends/super bounds
     * }</pre>
     *         <li>c.2 Previous calls to getAnnotatedType have annotated this wildcard with BOTH
     *             bounds e.g. the user wrote {@code <?>} but the checker framework added {@code <@C
     *             ? extends @D Object>} to the corresponding element.
     *             <pre>
     *             {@code <?> -- @C is placed on the lower bound and @D is placed on the upper bound
     *          This case is treated just like annotations in cases a/b.
     * }</pre>
     *       </ul>
     * </ul>
     */
    private static final class WildcardBoundAnnos {
        public final AnnotatedWildcardType wildcard;
        public final Set<AnnotationMirror> upperBoundAnnos;
        public final Set<AnnotationMirror> lowerBoundAnnos;

        // indicates that this is an annotation in front of an unbounded wildcard
        // e.g.  < @A ? >
        // For each annotation in this set, if there is no annotation in upperBoundAnnos
        // that is in the same hierarchy then the annotation will be applied to both bounds
        // otherwise the annotation applies to the lower bound only
        public final Set<AnnotationMirror> possiblyBoth;

        /** Whether or not wildcard has an explicit super bound. */
        private final boolean isSuperBounded;

        /** Whether or not wildcard has NO explicit bound whatsoever. */
        private final boolean isUnbounded;

        WildcardBoundAnnos(AnnotatedWildcardType wildcard) {
            this.wildcard = wildcard;
            this.upperBoundAnnos = AnnotationUtils.createAnnotationSet();
            this.lowerBoundAnnos = AnnotationUtils.createAnnotationSet();
            this.possiblyBoth = AnnotationUtils.createAnnotationSet();

            this.isSuperBounded = AnnotatedTypes.hasExplicitSuperBound(wildcard);
            this.isUnbounded = AnnotatedTypes.hasNoExplicitBound(wildcard);
        }

        void addAnnotation(final TypeCompound anno) {
            // if the typepath entry ends in Wildcard then the annotation should go on a bound
            // otherwise, the annotation is in front of the wildcard
            // e.g. @HERE ? extends Object
            final boolean isInFrontOfWildcard =
                    anno.getPosition().location.last() != TypePathEntry.WILDCARD;
            if (isInFrontOfWildcard && isUnbounded) {
                possiblyBoth.add(anno);

            } else {
                // A TypePathEntry of WILDCARD indicates that it is placed on the bound
                // use the type of the wildcard bound to determine which set to put it in

                if (isInFrontOfWildcard) {
                    if (isSuperBounded) {
                        upperBoundAnnos.add(anno);
                    } else {
                        lowerBoundAnnos.add(anno);
                    }
                } else { // it's on the bound
                    if (isSuperBounded) {
                        lowerBoundAnnos.add(anno);
                    } else {
                        upperBoundAnnos.add(anno);
                    }
                }
            }
        }

        /**
         * Apply the annotations to wildcard according to the rules outlined in the comment at the
         * beginning of this class.
         */
        void apply() {
            final AnnotatedTypeMirror extendsBound = wildcard.getExtendsBound();
            final AnnotatedTypeMirror superBound = wildcard.getSuperBound();

            for (AnnotationMirror extAnno : upperBoundAnnos) {
                extendsBound.addAnnotation(extAnno);
            }
            for (AnnotationMirror supAnno : lowerBoundAnnos) {
                superBound.addAnnotation(supAnno);
            }

            for (AnnotationMirror anno : possiblyBoth) {
                superBound.addAnnotation(anno);

                // This will be false if we've defaulted the bounds and are reading them again.
                // In that case, we will have already created an annotation for the extends bound
                // that should be honored and NOT overwritten.
                if (!extendsBound.isAnnotatedInHierarchy(anno)) {
                    extendsBound.addAnnotation(anno);
                }
            }
        }
    }

    /**
     * TypeCompounds are implementations of AnnotationMirror that are stored on Elements. Each type
     * compound has a TypeAnnotationPosition which identifies, relative to the "root" of a type,
     * where an annotation should be placed. This method adds all of the given TypeCompounds to the
     * correct location on type by interpreting the TypeAnnotationPosition.
     *
     * <p>Note: We handle all of the Element annotations on a type at once because we need to
     * identify whether or not the element annotation in front of an unbound wildcard (e.g. {@code
     * <@HERE ?>}) should apply to only the super bound or both the super bound and the extends
     * bound.
     *
     * @see org.checkerframework.framework.util.element.ElementAnnotationUtil.WildcardBoundAnnos
     * @param type the type in which annos should be placed
     * @param annos all of the element annotations, TypeCompounds, for type
     */
    static void annotateViaTypeAnnoPosition(
            final AnnotatedTypeMirror type, final Collection<TypeCompound> annos)
            throws UnexpectedAnnotationLocationException {
        final IdentityHashMap<AnnotatedWildcardType, WildcardBoundAnnos> wildcardToAnnos =
                new IdentityHashMap<>();
        for (final TypeCompound anno : annos) {
            AnnotatedTypeMirror target =
                    getTypeAtLocation(type, anno.position.location, anno, false);
            if (target.getKind() == TypeKind.WILDCARD) {
                addWildcardToBoundMap((AnnotatedWildcardType) target, anno, wildcardToAnnos);
            } else {
                target.addAnnotation(anno);
            }
        }

        for (WildcardBoundAnnos wildcardAnnos : wildcardToAnnos.values()) {
            wildcardAnnos.apply();
        }
    }

    /**
     * Creates an entry in wildcardToAnnos for wildcard if one does not already exists. Adds anno to
     * the WildcardBoundAnnos object for wildcard.
     */
    private static void addWildcardToBoundMap(
            final AnnotatedWildcardType wildcard,
            final TypeCompound anno,
            final Map<AnnotatedWildcardType, WildcardBoundAnnos> wildcardToAnnos) {
        WildcardBoundAnnos boundAnnos =
                wildcardToAnnos.computeIfAbsent(wildcard, WildcardBoundAnnos::new);
        boundAnnos.addAnnotation(anno);
    }

    /**
     * Returns true if the typeCompound is a primary annotation for the type it targets (or lower
     * bound if this is a type variable or wildcard ). If you think of a type as a tree-like
     * structure then a nested type any type that is not the root. E.g. {@code @T List< @N
     * String>}, @T is on a top-level NON-nested type where as the annotation @N is on a nested
     * type.
     *
     * @param typeCompound the type compound to inspect
     * @return true if typeCompound is placed on a nested type, false otherwise
     */
    static boolean isOnComponentType(final Attribute.TypeCompound typeCompound) {
        return !typeCompound.position.location.isEmpty();
    }

    /**
     * See the Type Annotation Specification on bounds
     * (https://checkerframework.org/jsr308/specification/java-annotation-design.html).
     *
     * <p>TypeAnnotationPositions have bound indices when they represent an upper bound on a
     * TypeVariable. The index 0 ALWAYS refers to the superclass type. If that supertype is implied
     * to be Object (because we didn't specify an extends) then the actual types will be offset by 1
     * (because index 0 is ALWAYS a class.
     *
     * <p>Therefore, These indices will be offset by -1 if the first type in the bound is an
     * interface which implies the specified type itself is an interface.
     *
     * <p>Reminder: There will only be multiple bound types if the upperBound is an intersection.
     *
     * @param upperBoundTypes the list of upperBounds for the type with bound positions you wish to
     *     offset
     * @return the bound offset for all TypeAnnotationPositions of TypeCompounds targeting these
     *     bounds
     */
    static int getBoundIndexOffset(final List<? extends AnnotatedTypeMirror> upperBoundTypes) {
        final int boundIndexOffset;
        if (((Type) upperBoundTypes.get(0).getUnderlyingType()).isInterface()) {
            boundIndexOffset = -1;
        } else {
            boundIndexOffset = 0;
        }

        return boundIndexOffset;
    }

    /**
     * Overload of getTypeAtLocation with default values null/false for the annotation and array
     * component flag, to make usage easier. Default visibility to allow usage within package.
     */
    static AnnotatedTypeMirror getTypeAtLocation(
            AnnotatedTypeMirror type, List<TypeAnnotationPosition.TypePathEntry> location)
            throws UnexpectedAnnotationLocationException {
        return getTypeAtLocation(type, location, null, false);
    }

    /**
     * Given a TypePath into a type, return the component type that is located at the end of the
     * TypePath.
     *
     * @param type a type containing the type specified by location
     * @param location a type path into type
     * @param anno an annotation to be applied to the inner types of a declared type if the declared
     *     type is itself a component type of an array
     * @param isComponentTypeOfArray indicates whether the type under analysis is a component type
     *     of some array type
     * @return the type specified by location
     */
    private static AnnotatedTypeMirror getTypeAtLocation(
            AnnotatedTypeMirror type,
            List<TypeAnnotationPosition.TypePathEntry> location,
            TypeCompound anno,
            boolean isComponentTypeOfArray)
            throws UnexpectedAnnotationLocationException {
        if (location.isEmpty() && type.getKind() != TypeKind.DECLARED) {
            // An annotation with an empty type path on a declared type applies to the outermost
            // enclosing type. This logic is handled together with non-empty type paths in
            // getLocationTypeADT.
            // For other kinds of types, no work is required for an empty type path.
            return type;
        }
        switch (type.getKind()) {
            case NULL:
                return getLocationTypeANT((AnnotatedNullType) type, location);
            case DECLARED:
                return getLocationTypeADT(
                        (AnnotatedDeclaredType) type, location, anno, isComponentTypeOfArray);
            case WILDCARD:
                return getLocationTypeAWT((AnnotatedWildcardType) type, location);
            case ARRAY:
                return getLocationTypeAAT((AnnotatedArrayType) type, location, anno);
            case UNION:
                return getLocationTypeAUT((AnnotatedUnionType) type, location);
            case INTERSECTION:
                return getLocationTypeAIT((AnnotatedIntersectionType) type, location);
            default:
                // Raise an error for all other types below.
        }
        throw new UnexpectedAnnotationLocationException(
                "ElementAnnotationUtil.getTypeAtLocation: unexpected annotation with location found"
                        + " for type: %s (kind: %s ) location: ",
                type, type.getKind(), location);
    }

    /**
     * Given a TypePath into a declared type, return the component type that is located at the end
     * of the TypePath.
     *
     * @param type a type containing the type specified by location
     * @param location a type path into type
     * @param anno an annotation to be applied to the inner types of the declared type if the
     *     declared type is itself a component type of an array
     * @param isComponentTypeOfArray indicates whether the type under analysis is a component type
     *     of some array type
     * @return the type specified by location
     */
    @SuppressWarnings("JdkObsolete") // error is issued on every operation, must suppress here
    private static AnnotatedTypeMirror getLocationTypeADT(
            AnnotatedDeclaredType type,
            List<TypeAnnotationPosition.TypePathEntry> location,
            TypeCompound anno,
            boolean isComponentTypeOfArray)
            throws UnexpectedAnnotationLocationException {
        // List order by outermost type to innermost type.
        ArrayDeque<AnnotatedDeclaredType> outerToInner = new ArrayDeque<>();
        AnnotatedDeclaredType enclosing = type;
        while (enclosing != null) {
            outerToInner.addFirst(enclosing);
            enclosing = enclosing.getEnclosingType();
        }

        // If the AnnotatedDeclaredType is a component of an array type, then apply anno to all
        // possible inner types.
        // NOTE: This workaround can be removed once
        // https://bugs.openjdk.java.net/browse/JDK-8208470 is fixed
        // The number of enclosing types is outerToInner.size() - 1; there only is
        // work to do if outerToInner contains more than one element.
        if (anno != null
                && isComponentTypeOfArray
                && location.isEmpty()
                && outerToInner.size() > 1) {
            ArrayDeque<AnnotatedDeclaredType> innerTypes = new ArrayDeque<>(outerToInner);
            innerTypes.removeFirst();
            while (!innerTypes.isEmpty()) {
                innerTypes.removeFirst().addAnnotation(anno);
            }
        }

        // Create a linked list of the location, so removing the first element is easier.
        // Also, the tail() operation wouldn't work with a Deque.
        @SuppressWarnings("JdkObsolete")
        LinkedList<TypePathEntry> tailOfLocations = new LinkedList<>(location);
        boolean error = false;
        while (!tailOfLocations.isEmpty()) {
            TypePathEntry currentLocation = tailOfLocations.removeFirst();
            switch (currentLocation.tag) {
                case INNER_TYPE:
                    outerToInner.removeFirst();
                    break;
                case TYPE_ARGUMENT:
                    AnnotatedDeclaredType innerType = outerToInner.getFirst();
                    if (currentLocation.arg < innerType.getTypeArguments().size()) {
                        AnnotatedTypeMirror typeArg =
                                innerType.getTypeArguments().get(currentLocation.arg);
                        return getTypeAtLocation(typeArg, tailOfLocations);
                    } else {
                        error = true;
                        break;
                    }
                default:
                    error = true;
            }
            if (error) {
                break;
            }
        }

        if (outerToInner.isEmpty() || error) {
            throw new UnexpectedAnnotationLocationException(
                    "ElementAnnotationUtil.getLocationTypeADT: invalid location %s for type: %s",
                    location, type);
        }

        return outerToInner.getFirst();
    }

    private static AnnotatedTypeMirror getLocationTypeANT(
            AnnotatedNullType type, List<TypeAnnotationPosition.TypePathEntry> location)
            throws UnexpectedAnnotationLocationException {
        if (location.size() == 1 && location.get(0).tag == TypePathEntryKind.TYPE_ARGUMENT) {
            return type;
        }

        throw new UnexpectedAnnotationLocationException(
                "ElementAnnotationUtil.getLocationTypeANT: " + "invalid location %s for type: %s ",
                location, type);
    }

    private static AnnotatedTypeMirror getLocationTypeAWT(
            final AnnotatedWildcardType type,
            final List<TypeAnnotationPosition.TypePathEntry> location)
            throws UnexpectedAnnotationLocationException {

        // the last step into the Wildcard type is handled in WildcardToBoundAnnos.addAnnotation
        if (location.size() == 1) {
            return type;
        }

        if (!location.isEmpty()
                && location.get(0).tag == TypeAnnotationPosition.TypePathEntryKind.WILDCARD) {
            if (AnnotatedTypes.hasExplicitExtendsBound(type)) {
                return getTypeAtLocation(type.getExtendsBound(), tail(location));
            } else if (AnnotatedTypes.hasExplicitSuperBound(type)) {
                return getTypeAtLocation(type.getSuperBound(), tail(location));
            } else {
                return getTypeAtLocation(type.getExtendsBound(), tail(location));
            }

        } else {
            throw new UnexpectedAnnotationLocationException(
                    "ElementAnnotationUtil.getLocationTypeAWT: "
                            + "invalid location %s for type: %s ",
                    location, type);
        }
    }

    /**
     * When we have an (e.g. @Odd int @NonNull []) the type-annotation position of the array
     * annotation (@NonNull) is really the outermost type in the TypeAnnotationPosition and it will
     * NOT have TypePathEntryKind.ARRAY at the end of its position. The position of the component
     * type (@Odd) is considered deeper in the type and therefore has the TypePathEntryKind.ARRAY in
     * its position.
     */
    private static AnnotatedTypeMirror getLocationTypeAAT(
            AnnotatedArrayType type,
            List<TypeAnnotationPosition.TypePathEntry> location,
            TypeCompound anno)
            throws UnexpectedAnnotationLocationException {
        if (location.size() >= 1
                && location.get(0).tag == TypeAnnotationPosition.TypePathEntryKind.ARRAY) {
            AnnotatedTypeMirror comptype = type.getComponentType();
            return getTypeAtLocation(comptype, tail(location), anno, true);
        } else {
            throw new UnexpectedAnnotationLocationException(
                    "ElementAnnotationUtil.annotateAAT: " + "invalid location %s for type: %s ",
                    location, type);
        }
    }

    /*
     * TODO: this case should never occur!
     * A union type can only occur in special locations, e.g. for exception
     * parameters. The EXCEPTION_PARAMETER TartetType should be used to
     * decide which of the alternatives in the union to annotate.
     * Only the TypePathEntry is not enough.
     * As a hack, always annotate the first alternative.
     */
    private static AnnotatedTypeMirror getLocationTypeAUT(
            AnnotatedUnionType type, List<TypeAnnotationPosition.TypePathEntry> location)
            throws UnexpectedAnnotationLocationException {
        AnnotatedTypeMirror comptype = type.getAlternatives().get(0);
        return getTypeAtLocation(comptype, location);
    }

    /** Intersection types use the TYPE_ARGUMENT index to separate the individual types. */
    private static AnnotatedTypeMirror getLocationTypeAIT(
            AnnotatedIntersectionType type, List<TypeAnnotationPosition.TypePathEntry> location)
            throws UnexpectedAnnotationLocationException {
        if (location.size() >= 1
                && location.get(0).tag == TypeAnnotationPosition.TypePathEntryKind.TYPE_ARGUMENT) {
            AnnotatedTypeMirror bound = type.getBounds().get(location.get(0).arg);
            return getTypeAtLocation(bound, tail(location));
        } else {
            throw new UnexpectedAnnotationLocationException(
                    "ElementAnnotationUtil.getLocatonTypeAIT: invalid location %s for type: %s ",
                    location, type);
        }
    }

    private static <T> List<T> tail(List<T> list) {
        return list.subList(1, list.size());
    }

    /** Exception indicating an invalid location for an annotation was found. */
    @SuppressWarnings("serial")
    public static class UnexpectedAnnotationLocationException extends Exception {

        /**
         * Creates an UnexpectedAnnotationLocationException.
         *
         * @param format format string
         * @param args arguments to the format string
         */
        @FormatMethod
        private UnexpectedAnnotationLocationException(String format, Object... args) {
            super(String.format(format, args));
        }
    }

    /** An ERROR TypeKind was found. */
    @SuppressWarnings("serial")
    public static class ErrorTypeKindException extends Error {

        /**
         * Creates an ErrorTypeKindException.
         *
         * @param format format string
         * @param args arguments to the format string
         */
        @FormatMethod
        public ErrorTypeKindException(String format, Object... args) {
            super(String.format(format, args));
        }
    }
}
