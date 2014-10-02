package org.checkerframework.framework.util.element;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntryKind;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.javacutil.ErrorReporter;

import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;


/**
 * Utility methods for adding the annotations that are stored in an Element to the
 * type that represents that element (or a use of that Element).  This class also contains
 * package private methods used by the ElementAnnotationAppliers that do most of the work.
 */
public class ElementAnnotationUtil {

    /**
     * For each type/element pair, add all of the annotations stored in Element to type.
     * See apply for more details.
     * @param types The types to which we wish to apply element annotations.
     * @param elements The elements that may contain annotations to apply.  elements.size must == types.size
     * @param typeFactory The type factory used to create the AnnotatedTypeMirrors contained by types
     */
    public static void applyAllElementAnnotations(final List<? extends AnnotatedTypeMirror> types,
                                                  final List<? extends Element> elements,
                                                  final AnnotatedTypeFactory typeFactory) {


        if( types.size() != elements.size()) {
            ErrorReporter.errorAbort("Number of types and elements don't match!" +
                    "types ( "   + PluginUtil.join(", ", types) + " ) " +
                    "element ( " + PluginUtil.join(", ", elements) + " ) ");
        }

        for( int i = 0; i < types.size(); i++ ) {
            ElementAnnotationApplier.apply(types.get(i), elements.get(i), typeFactory);
        }
    }

    /**
     * For backwards-compatibility: treat declaration annotations
     * as type annotations, if we now understand them as type annotations.
     * In particular, this allows the transition from JSR 305 declaration
     * annotations to JSR 308 type annotations.
     *
     * There are some caveats to this: the interpretation for declaration
     * and type annotations differs, in particular for arrays and inner
     * types. See the manual for a discussion.
     *
     * @param type The type to annotate
     * @param annotations The annotations to add
     */
    static void addAnnotationsFromElement(final AnnotatedTypeMirror type,
                                          final List<? extends AnnotationMirror> annotations) {
        AnnotatedTypeMirror innerType = AnnotatedTypes.innerMostType(type);
        innerType.addAnnotations(annotations);
    }


    /**
     * Does expectedValues contain enumValue.  This is just a linear search.
     * @param enumValue Value to search for, a needle
     * @param expectedValues Values to search through, a haystack
     * @return true if enumValue is in expectedValues, false otherwise
     */
    static boolean contains(Object enumValue, Object[] expectedValues) {
        for( final Object expected : expectedValues ) {
            if( enumValue.equals(expected) ) {
                return true;
            }
        }

        return false;
    }

    /**
     * TypeCompounds are implementations of AnnotationMirror that are stored on Elements.  Each type compound
     * has a TypeAnnotationPosition which identifies, relative to the "root" of a type, where an annotation
     * should be placed.  This method adds the given TypeCompound to the correct location on type by interpreting
     * the TypeAnnotationPosition.
     *
     * @param type The type in which annoTc should be placed
     * @param anno A TypeCompound representing an annotation to be placed on type
     */
    static void annotateViaTypeAnnoPosition(final AnnotatedTypeMirror type, final Attribute.TypeCompound anno) {
        TypeAnnotationPosition pos = anno.position;
        if (pos.location.isEmpty()) {
            // This check prevents that annotations on the declaration of
            // the type variable are also added to the type variable use.
            if (type.getKind() == TypeKind.TYPEVAR) {
                type.removeAnnotationInHierarchy(anno);
            }
            type.addAnnotation(anno);
        } else { //annotate inner locations

            AnnotatedTypeMirror inner = getTypeAtLocation(type, anno.getPosition().location);
            inner.addAnnotation(anno);
        }
    }

    /**
     * Returns true if the typeCompound is a primary annotation for the type it targets (or lower bound if this
     * is a type variable or wildcard ).  If you think of a type as a tree-like structure then a nested type
     * any type that is not the root.  E.g. @T List< @N String>,  @T is on a top-level NON-nested type where as
     * the annotation @N is on a nested type.
     *
     * @param typeCompound The type compound to inspect
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
     * @param upperBoundTypes The list of upperBounds for the type with bound positions you wish to offset.
     * @return The bound offset for all TypeAnnotationPositions of TypeCompounds targeting these bounds.
     */
    static int getBoundIndexOffset( final List<? extends AnnotatedTypeMirror> upperBoundTypes ) {
        final int boundIndexOffset;
        if( ((Type)upperBoundTypes.get(0).getUnderlyingType()).isInterface()) {
            boundIndexOffset = -1;
        } else {
            boundIndexOffset = 0;
        }

        return boundIndexOffset;
    }

    /**
     * Given a TypePath into a type, return the component type that is located at the end of the TypePath
     * @param type A type containing the type specified by location
     * @param location A type path into type
     * @return The type specified by location
     */
    static AnnotatedTypeMirror getTypeAtLocation(AnnotatedTypeMirror type, List<TypeAnnotationPosition.TypePathEntry> location) {

        if (type.getKind() != TypeKind.WILDCARD && location.isEmpty()) {
            return type;
        } else if (type.getKind() == TypeKind.NULL) {
            return getLocationTypeANT((AnnotatedNullType) type, location);
        } else if (type.getKind() == TypeKind.DECLARED) {
            return getLocationTypeADT((AnnotatedDeclaredType)type, location);
        } else if (type.getKind() == TypeKind.WILDCARD) {
            return getLocationTypeAWT((AnnotatedWildcardType)type, location);
        } else if (type.getKind() == TypeKind.ARRAY) {
            return getLocationTypeAAT((AnnotatedArrayType)type, location);
        } else {
            ErrorReporter.errorAbort("ElementAnnotationUtil.getTypeAtLocation: only declared types, "
                                   + "arrays, and null types can have annotations with location; found type: "
                                   + type + " location: " + location);
            return null; // dead code
        }
    }

    /**
     * Given a TypePath into a declared type, return the component type that is located at the end of the TypePath
     * @param type A type containing the type specified by location
     * @param location A type path into type
     * @return The type specified by location
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

    /**
     * @param type
     * @return co
     */
    private static int countEnclosing(AnnotatedDeclaredType type) {
        int cnt = 0;
        while (type.getEnclosingType() != null) {
            ++cnt;
            type = type.getEnclosingType();
        }
        return cnt;
    }

    private static AnnotatedTypeMirror getLocationTypeANT(AnnotatedNullType type, List<TypeAnnotationPosition.TypePathEntry> location) {
        if( location.size() == 1 && location.get(0).tag == TypePathEntryKind.TYPE_ARGUMENT) {
            return type;
        }

        ErrorReporter.errorAbort("ElementAnnotationUtil.getLocationTypeANT: " +
                                 "invalid location " + location + " for type: " + type);
        return null; //dead code
    }

    private static boolean isExtendsBounded(final AnnotatedWildcardType wcType) {
        return wcType.getUnderlyingType().getExtendsBound() != null;
    }

    private static boolean isSuperBounded(final AnnotatedWildcardType wcType) {
        return wcType.getUnderlyingType().getSuperBound() != null;
    }

    private static AnnotatedTypeMirror getLocationTypeAWT(final AnnotatedWildcardType type,
                                                          final List<TypeAnnotationPosition.TypePathEntry> location) {

        if (location.isEmpty()) {
            //Applying an annotation in front of a wildcard applies it to the bound that is not explicitly written
            //in the wildcard.  E.g.
            // @P ? extends Object
            // In this case, the Type location of @P indicates that it is on the wildcard.  But since the
            // Checker Framework treats that location as if it applies to the lower bound, we apply the
            //annotation to the superBound type
            //That is the type becomease  ? [ super @P <null> extends Object]

            if( isExtendsBounded(type) ) {
                return type.getSuperBound();
            } else if( isSuperBounded(type) ) {
                return type.getExtendsBound();
            }  else {
                return type.getSuperBound();
            }

        } else if (location.get(0).tag.equals(TypeAnnotationPosition.TypePathEntryKind.WILDCARD)) {
            if( isExtendsBounded(type) ) {
                return getTypeAtLocation(type.getExtendsBound(), tail(location));
            } else if( isSuperBounded(type) ) {
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
