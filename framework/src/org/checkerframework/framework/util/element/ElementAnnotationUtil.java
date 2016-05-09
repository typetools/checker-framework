package org.checkerframework.framework.util.element;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.ElementAnnotationApplier;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.TargetType;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntryKind;


/**
 * Utility methods for adding the annotations that are stored in an Element to the
 * type that represents that element (or a use of that Element).  This class also contains
 * package private methods used by the ElementAnnotationAppliers that do most of the work.
 */
public class ElementAnnotationUtil {

    /**
     * For each type/element pair, add all of the annotations stored in Element to type.
     * See apply for more details.
     * @param types the types to which we wish to apply element annotations
     * @param elements the elements that may contain annotations to apply.  elements.size must == types.size
     * @param typeFactory the type factory used to create the AnnotatedTypeMirrors contained by types
     */
    public static void applyAllElementAnnotations(final List<? extends AnnotatedTypeMirror> types,
                                                  final List<? extends Element> elements,
                                                  final AnnotatedTypeFactory typeFactory) {


        if (types.size() != elements.size()) {
            ErrorReporter.errorAbort("Number of types and elements don't match!" +
                    "types ( "   + PluginUtil.join(", ", types) + " ) " +
                    "element ( " + PluginUtil.join(", ", elements) + " ) ");
        }

        for (int i = 0; i < types.size(); i++ ) {
            ElementAnnotationApplier.apply(types.get(i), elements.get(i), typeFactory);
        }
    }

    /**
     * For backwards-compatibility: treat declaration annotations
     * as type annotations, if we now understand them as type annotations.
     * In particular, this allows the transition from Java 5 declaration
     * annotations to Java 8 type annotations.
     *
     * There are some caveats to this: the interpretation for declaration
     * and type annotations differs, in particular for arrays and inner
     * types. See the manual for a discussion.
     *
     * @param type the type to annotate
     * @param annotations the annotations to add
     */
    static void addAnnotationsFromElement(final AnnotatedTypeMirror type,
                                          final List<? extends AnnotationMirror> annotations) {
        AnnotatedTypeMirror innerType = AnnotatedTypes.innerMostType(type);
        innerType.addAnnotations(annotations);
    }


    /**
     * Does expectedValues contain enumValue.  This is just a linear search.
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
     * Use a map to partition annotations with the given TargetTypes into Lists,
     * where each target type is a key in the output map.  Any annotation that
     * does not have one of these target types will be added to unmatched
     * @param annos the collection of annotations to partition
     * @param unmatched a list to add annotations with unmatched target types to
     * @param targetTypes a list of target types to partition annos with
     * @return a map from targetType &rarr; List of Annotations that have that targetType
     */
    static Map<TargetType, List<TypeCompound>> partitionByTargetType(Collection<TypeCompound> annos,
                                                                     List<TypeCompound> unmatched,
                                                                     TargetType... targetTypes) {
        final Map<TargetType, List<TypeCompound>> targetTypeToAnnos = new HashMap<>();
        for (TargetType targetType : targetTypes) {
            targetTypeToAnnos.put(targetType, new ArrayList<TypeCompound>(10));
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
     * A class used solely to annotate wildcards from Element annotations.  Instances
     * of WildcardBoundAnnos are used to aggregate ALL annotations for a given Wildcard
     * and then apply them all at once in order to resolve the annotations in front of
     * unbound wildcards.
     *
     * Wildcard annotations are applied as follows:
     *
     * a) If an Annotation is in front of a extends or super bounded wildcard,
     * it applies to the bound that is NOT explicitly present. e.g.
     * <pre>{@code
     * <@A ? extends Object> - @A is placed on the super bound (Void)
     * <@B ? super CharSequence> - @B is placed on the extends bound (probably Object)
     * }</pre>
     *
     * b) If an Annotation is on a bound, it applies to that bound.  E.g.
     * <pre>{@code
     * <? extends @A Object> - @A is placed on the extends bound (Object)
     * <? super @B CharSequence> - @B is placed on the super bound (CharSequence)
     * }</pre>
     *
     * c) If an Annotation is on an unbounded wildard there are two subcases.
     *    c.1 The user wrote the annotation explicitly - these annotations apply to both bounds
     *    e.g. the user wrote
     * <pre>{@code
     *    <@C ?> - the annotation is placed on the extends/super bounds
     * }</pre>
     *
     *    c.2 Previous calls to getAnnotatedType have annotated this wildcard with BOTH bounds
     *    e.g. the user wrote {@code <?>} but the checker framework added {@code <@C ? extends @D Object>}
     *         to the corresponding element
     *    {@code <?>} - @C is placed on the lower bound and @D is placed on the upper bound
     *          This case is treated just like annotations in cases a/b.
     *
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

        /**
         * Whether or not wildcard has an explicit super bound.
         */
        private final boolean isSuperBounded;

        /**
         * Whether or not wildcard has NO explicit bound whatsoever
         */
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
            final boolean isInFrontOfWildcard = anno.getPosition().location.last() != TypePathEntry.WILDCARD;
            if (isInFrontOfWildcard && isUnbounded) {
                possiblyBoth.add(anno);

            } else {
                // A TypePathEntry of WILDCARD indicates that is is placed on the bound
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
         * Apply the annotations to wildcard according to the rules outlined in the
         * comment at the beginning of this class
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

                // this will be false if we've defaulted the bounds and are reading them again
                // in that case, we will have already created an annotation for the extends bound
                // that should be honored and NOT overwritten
                if (extendsBound.getAnnotationInHierarchy(anno) == null) {
                    extendsBound.addAnnotation(anno);
                }
            }

        }
    }

    /**
     * TypeCompounds are implementations of AnnotationMirror that are stored on Elements.  Each type compound
     * has a TypeAnnotationPosition which identifies, relative to the "root" of a type, where an annotation
     * should be placed.  This method adds all of the given TypeCompounds to the correct location on type
     * by interpreting the TypeAnnotationPosition.
     *
     * Note: We handle all of the Element annotations on a type at once because we need to identify whether
     * or not the element annotation in front of an unbound wildcard (e.g. {@code <@HERE ?>}) should apply to
     * only the super bound or both the super bound and the extends bound.
     * @see org.checkerframework.framework.util.element.ElementAnnotationUtil.WildcardBoundAnnos
     *
     * @param type the type in which annos should be placed
     * @param annos all of the element annotations, TypeCompounds, for type
     */
    static void annotateViaTypeAnnoPosition(final AnnotatedTypeMirror type, final Collection<TypeCompound> annos) {
        final Map<AnnotatedWildcardType, WildcardBoundAnnos> wildcardToAnnos = new IdentityHashMap<>();
        for (final TypeCompound anno : annos) {
            AnnotatedTypeMirror target = getTypeAtLocation(type, anno.position.location);
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
     * Creates an entry in wildcardToAnnos for wildcard if one does not already exists.  Adds
     * anno to the WildcardBoundAnnos object for wildcard.
     */
    private static void addWildcardToBoundMap(final AnnotatedWildcardType wildcard, final TypeCompound anno,
                                              final Map<AnnotatedWildcardType, WildcardBoundAnnos> wildcardToAnnos) {
        WildcardBoundAnnos boundAnnos = wildcardToAnnos.get(wildcard);
        if (boundAnnos == null) {
            boundAnnos = new WildcardBoundAnnos(wildcard);
            wildcardToAnnos.put(wildcard, boundAnnos);
        }

        boundAnnos.addAnnotation(anno);
    }

    /**
     * Returns true if the typeCompound is a primary annotation for the type it targets (or lower bound if this
     * is a type variable or wildcard ).  If you think of a type as a tree-like structure then a nested type
     * any type that is not the root.  E.g. {@code @T List< @N String>},  @T is on a top-level NON-nested type where as
     * the annotation @N is on a nested type.
     *
     * @param typeCompound the type compound to inspect
     * @return true if typeCompound is placed on a nested type, false otherwise
     */
    static boolean isOnComponentType( final Attribute.TypeCompound typeCompound ) {
        return !typeCompound.position.location.isEmpty();
    }

    /**
     * See the Type Annotation Specification on bounds
     * (http://types.cs.washington.edu/jsr308/specification/java-annotation-design.html)
     *
     * TypeAnnotationPositions have bound indices when they represent an upper bound on a TypeVariable.  The index
     * 0 ALWAYS refers to the superclass type.  If that supertype is implied to be Object (because we didn't
     * specify an extends) then the actual types will be offset by 1 (because index 0 is ALWAYS a class.
     *
     * Therefore, These indices will be offset by -1 if the first type in the bound is an interface which
     * implies the specified type itself is an interface.
     *
     * Reminder: There will only be multiple bound types if the upperBound is an intersection.
     *
     * @param upperBoundTypes the list of upperBounds for the type with bound positions you wish to offset
     * @return the bound offset for all TypeAnnotationPositions of TypeCompounds targeting these bounds
     */
    static int getBoundIndexOffset( final List<? extends AnnotatedTypeMirror> upperBoundTypes ) {
        final int boundIndexOffset;
        if ( ((Type)upperBoundTypes.get(0).getUnderlyingType()).isInterface()) {
            boundIndexOffset = -1;
        } else {
            boundIndexOffset = 0;
        }

        return boundIndexOffset;
    }

    /**
     * Given a TypePath into a type, return the component type that is located at the end of the TypePath
     * @param type a type containing the type specified by location
     * @param location a type path into type
     * @return the type specified by location
     */
    static AnnotatedTypeMirror getTypeAtLocation(AnnotatedTypeMirror type, List<TypeAnnotationPosition.TypePathEntry> location) {

        if (location.isEmpty()) {
            return type;
        } else if (type.getKind() == TypeKind.NULL) {
            return getLocationTypeANT((AnnotatedNullType) type, location);
        } else if (type.getKind() == TypeKind.DECLARED) {
            return getLocationTypeADT((AnnotatedDeclaredType) type, location);
        } else if (type.getKind() == TypeKind.WILDCARD) {
            return getLocationTypeAWT((AnnotatedWildcardType) type, location);
        } else if (type.getKind() == TypeKind.ARRAY) {
            return getLocationTypeAAT((AnnotatedArrayType) type, location);
        } else {
            ErrorReporter.errorAbort("ElementAnnotationUtil.getTypeAtLocation: only declared types, "
                                   + "arrays, and null types can have annotations with location; found type: "
                                   + type + " location: " + location);
            return null; // dead code
        }
    }

    /**
     * Given a TypePath into a declared type, return the component type that is located at the end of the TypePath
     * @param type a type containing the type specified by location
     * @param location a type path into type
     * @return the type specified by location
     */
    private static AnnotatedTypeMirror getLocationTypeADT(AnnotatedDeclaredType type,  List<TypeAnnotationPosition.TypePathEntry> location) {

        if (location.isEmpty()) {
            return type;

        } else if (location.get(0).tag.equals(TypeAnnotationPosition.TypePathEntryKind.TYPE_ARGUMENT) &&
                location.get(0).arg < type.getTypeArguments().size()) {
            return getTypeAtLocation(type.getTypeArguments().get(location.get(0).arg), tail(location));
        } else if (location.get(0).tag.equals(TypeAnnotationPosition.TypePathEntryKind.INNER_TYPE)) {
            // TODO: annotations on enclosing classes (e.g. @A Map.Entry<K, V>) not tested yet
            int totalEncl = countEnclosing(type);
            int totalInner = countInner(location);
            if (totalInner > totalEncl) {
                return type;

            } else if (totalInner == totalEncl) {
                List<TypeAnnotationPosition.TypePathEntry> loc = location;
                for (int i = 0; i < totalEncl; ++i) {
                    loc = tail(loc);
                }
                return getTypeAtLocation(type, loc);
            } else {
                AnnotatedDeclaredType toret = type;
                List<TypeAnnotationPosition.TypePathEntry> loc = location;
                for (int i = 0; i < (totalEncl-totalInner); ++i) {
                    if (toret.getEnclosingType() != null) {
                        toret = toret.getEnclosingType();
                        loc = tail(loc);
                    }
                }
                return getTypeAtLocation(toret, loc);
            }
        } else {
            return type;
        }
    }

    /**
     * @param location a type path
     * @return starting from the first index of location, return the number of INNER_TYPE entries in location
     * that occur consecutively
     */
    private static int countInner(List<TypeAnnotationPosition.TypePathEntry> location) {
        int cnt = 0;
        while (!location.isEmpty() &&
                location.get(0).tag.equals(TypeAnnotationPosition.TypePathEntryKind.INNER_TYPE)) {
            ++cnt;
            location = tail(location);
        }
        return cnt;
    }

    private static int countEnclosing(AnnotatedDeclaredType type) {
        int cnt = 0;
        while (type.getEnclosingType() != null) {
            ++cnt;
            type = type.getEnclosingType();
        }
        return cnt;
    }

    private static AnnotatedTypeMirror getLocationTypeANT(AnnotatedNullType type, List<TypeAnnotationPosition.TypePathEntry> location) {
        if (location.size() == 1 && location.get(0).tag == TypePathEntryKind.TYPE_ARGUMENT) {
            return type;
        }

        ErrorReporter.errorAbort("ElementAnnotationUtil.getLocationTypeANT: " +
                                 "invalid location " + location + " for type: " + type);
        return null; // dead code
    }

    private static AnnotatedTypeMirror getLocationTypeAWT(final AnnotatedWildcardType type,
                                                          final List<TypeAnnotationPosition.TypePathEntry> location) {

        // the last step into the Wildcard type is handled in WildcardToBoundAnnos.addAnnotation
        if (location.size() == 1) {
            return type;
        }

        if (!location.isEmpty() && location.get(0).tag.equals(TypeAnnotationPosition.TypePathEntryKind.WILDCARD)) {
            if (AnnotatedTypes.hasExplicitExtendsBound(type)) {
                   return getTypeAtLocation(type.getExtendsBound(), tail(location));
               } else if (AnnotatedTypes.hasExplicitSuperBound(type)) {
                   return getTypeAtLocation(type.getSuperBound(), tail(location));
               }  else {
                   return getTypeAtLocation(type.getExtendsBound(), tail(location));
               }

        } else {
            ErrorReporter.errorAbort("ElementAnnotationUtil.getLocationTypeAWT: " +
                                      "invalid location " + location + " for type: " + type);
            return null;
        }
    }

    /**
     * When we have an (e.g. @Odd int @NonNull []) the type-annotation position of the array annotation (@NonNull)
     * is really the outer most type in the TypeAnnotationPosition and will NOT have TypePathEntryKind.ARRAY
     * at the end of its position.  The position of the component type (@Odd) is considered deeper in the type
     * and therefore has the TypePathEntryKind.ARRAY in its position.
     */
    private static AnnotatedTypeMirror getLocationTypeAAT(AnnotatedArrayType type, List<TypeAnnotationPosition.TypePathEntry> location) {
        if (location.size() >= 1 &&
                location.get(0).tag.equals(TypeAnnotationPosition.TypePathEntryKind.ARRAY)) {
            AnnotatedTypeMirror comptype = type.getComponentType();
            return getTypeAtLocation(comptype, tail(location));
        } else {
            ErrorReporter.errorAbort("ElementAnnotationUtil.annotateAAT: " +
                    "invalid location " + location + " for type: " + type);
            return null; // dead code
        }
    }


    private static <T> List<T> tail(List<T> list) {
        return list.subList(1, list.size());
    }
}
