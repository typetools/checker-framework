package org.checkerframework.framework.type.explicit;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntryKind;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.PluginUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import java.lang.annotation.Annotation;
import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;


/**
 * Utility methods for adding the annotations that are stored in an Element to the
 * type that represents that element (or a use of that Element).  This class also contains
 * package private methods used by the ElementAnnotationAppliers that do most of the work.
 */
public class ElementAnnotationUtil {

    /**
     * Add all of the relevant annotations stored in Element to type. This includes both top-level primary
     * annotations and nested annotations.  For the most part the TypeAnnotationPosition of the
     * element annotations are used to locate the annotation in the right AnnotatedTypeMirror location
     * though the individual applier classes may have special rules (such as those for upper and lower bounds
     * and intersections).
     *
     * Note:  Element annotations come from two sources.
     *
     * 1) Annotations found on elements may represent those in source code or bytecode;
     * these are added to the element by the compiler.
     *
     * 2) The annotations may also represent those that were inferred or defaulted by the Checker
     * Framework after a previous call to this method.  The Checker Framework will store
     * any annotations on declarations back into the elements that represent them
     * (see TypeIntoElement).  Subsequent, calls to applyElementAnnotations will encounter
     * these annotations on the provided element.
     *
     * Note:  This is not the ONLY place that annotations are explicitly added to types.
     * See TypeFromTree.
     *
     * @param type The type to which we wish to apply the element's annotations
     * @param element An element that possibly contains annotations
     * @param typeFactory The typeFactory used to create the given type.
     */
    public static void applyElementAnnotations(final AnnotatedTypeMirror type, final Element element,
                                               final AnnotatedTypeFactory typeFactory) {

        if ( element == null ) {
            ErrorReporter.errorAbort("ElementAnnotationUtil.applyElementAnnotations: element cannot be null");

        } else if( TypeVarUseApplier.accepts(type, element) ) {
            TypeVarUseApplier.apply(type, element, typeFactory);

        } else if( VariableApplier.accepts(type, element) ) {
            VariableApplier.apply(type, element);

        } else if ( MethodApplier.accepts(type, element) ) {
            MethodApplier.apply(type, element, typeFactory);

        } else if ( TypeDeclarationApplier.accepts(type, element) ) {
            TypeDeclarationApplier.apply(type, element, typeFactory);

        } else if ( ClassTypeParamApplier.accepts(type, element) ) {
            ClassTypeParamApplier.apply((AnnotatedTypeVariable) type, element, typeFactory);

        } else if ( MethodTypeParamApplier.accepts(type, element)) {
            MethodTypeParamApplier.apply((AnnotatedTypeVariable) type, element, typeFactory);

        } else if ( ParamApplier.accepts(type, element) ) {
            ParamApplier.apply(type, element, typeFactory);

        } else if ( isCaptureConvertedTypeVar(type, element ) ){
            //Types resulting from capture conversion cannot have explicit annotations

        } else {
            ErrorReporter.errorAbort("ElementAnnotationUtil.applyElementAnnotations: illegal argument: " +
                    element + " [" + element.getKind() + "]" + " with type " + type);
        }
    }

    /**
     * For each type/element pair, add all of the annotations stored in Element to type.
     * See applyElementAnnotations for more details.
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
            applyElementAnnotations(types.get(i), elements.get(i), typeFactory);
        }
    }

    //TODO: NEED TO ACTUALLY USE THIS IN PLACE OF TYPEFROMELEMENT
    /**
     * Annotate the list of supertypes using the annotations on the TypeElement representing a class or interface
     * @param supertypes Types representing supertype declarations of TypeElement
     * @param subtypeElement An element representing the declaration of the class which is a subtype of supertypes
     */
    public static void annotateSupers(List<AnnotatedDeclaredType> supertypes, TypeElement subtypeElement ) {
        SuperTypeApplier.annotateSupers(supertypes, subtypeElement);
    }

    /**
     * If typeVar has primary annotations:
     *    Then typeVar represents an annotated use of a type parameter and
     *    its bounds are exactly the primary annotations.  Therefore,
     *    we copy the primary annotations over the annotations of its bounds.
     *
     * Otherwise, if typeVar does not have primary annotations:
     *    Then the typeVar represents either an unannotated use of a type
     *    or a type parameter declaration.  Either way, the bounds are the
     *    same as a type parameter declaration.  However, if the lowerBound
     *    does not have an annotation at present we add the bottom of the type
     *    hierarchy to the lower bound.
     *
     * HACK:  At the moment the Nullness type system uses a
     * GeneralAnnotatedTypeFactory to get types for constructors.  This
     * type factory does not support getting the top or bottom annotations
     * of the hierarchy and therefore must be special cased below.  The
     * Nullness type system should be fixed and this special case should be
     * removed.
     *
     *
     * @param typeVar
     * @param typeFactory
     */
    public static void fixAnnotatedTypeVariableBounds(final AnnotatedTypeVariable typeVar, final AnnotatedTypeFactory typeFactory) {

        final AnnotatedTypeMirror lowerBound = typeVar.getLowerBound();
        final Set<AnnotationMirror> primaryAnnotations = typeVar.getAnnotations();
        if( lowerBound.getKind() != TypeKind.TYPEVAR ) { //TODO JB: Ask Werner, can lower bounds be intersections?  Do we need to do something clever?

            //Terrible kludge to support GeneralAnnotatedTypeFactory
            if( isNullnessGeneralAtf(typeFactory) ) {
                final List<AnnotationMirror> annos = getNullnessAndInitAnnos(typeVar, typeFactory);
                lowerBound.clearAnnotations();
                lowerBound.addAnnotations(annos);

            } else {
                final QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();
                for(final AnnotationMirror top : typeFactory.getQualifierHierarchy().getTopAnnotations()) {
                    if( lowerBound.getAnnotationInHierarchy(top) == null ) {
                        lowerBound.addAnnotation(qualifierHierarchy.getBottomAnnotation(top));

                    } else {
                        //TODO JB: CHECK BOTTOM IS BELOW TOP AND ISSUE A WARNING? OR DO THIS IN isValidType

                    }

                }

            }
        }

        //We allow the above replacement first because primary annotations might not have annotations for
        //all hierarchies, so we don't want to avoid placing bottom on the lower bound for those hierarchies that
        //don't have a qualifier in primaryAnnotations
        if( !primaryAnnotations.isEmpty() ) {
            replaceUpperBoundAnnotations(typeVar, primaryAnnotations);

            //Note:
            // if the lower bound is a type variable
            // then when we place annotations on the primary annotation
            //   this will actually cause the type variable to be exact and
            //   propagate the primary annotation to the type variable because
            //   primary annotations overwrite the upper and lower bounds of type variables
            //   when getUpperBound/getLowerBound is called
            typeVar.getLowerBound().replaceAnnotations(primaryAnnotations);
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
    public static boolean contains(Object enumValue, Object[] expectedValues) {
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
     * Return the enclosing MethodSymbol of the given element, throwing an exception of the symbol's enclosing
     * element is not a MethodSymbol
     * @param methodChildElem Some element that is a child of a method typeDeclaration (e.g. a parameter or return type)
     * @return The MethodSymbol of the method containing methodChildElem
     */
    static Symbol.MethodSymbol getParentMethod(final Element methodChildElem) {
        if(!( methodChildElem.getEnclosingElement() instanceof Symbol.MethodSymbol)) {
            throw new RuntimeException("Element is not a direct child of a MethodSymbol. Element ( " + methodChildElem +
                                       " parent ( " + methodChildElem.getEnclosingElement() + " ) ");
        }
        return (Symbol.MethodSymbol) methodChildElem.getEnclosingElement();
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
    static boolean isOnNestedType( final Attribute.TypeCompound typeCompound ) {
        return !typeCompound.position.location.isEmpty();
    }

    /**
     * Was the type passed in generated by capture conversion.
     * @param type The type to test
     * @param element The element which type represents
     * @return true if type was generated via capture conversion
     *         false otherwise
     */
    public static boolean isCaptureConvertedTypeVar(final AnnotatedTypeMirror type, final Element element) {
        final Element enclosure = element.getEnclosingElement();
        return (((Symbol)enclosure).kind == com.sun.tools.javac.code.Kinds.NIL);
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
     * Replaces (or adds if none exist) the primary annotation of all upper bounds of typeVar,
     * the AnnotatedTypeVariable with the annotations provided.  The AnnotatedTypeVariable will only
     * have multiple upper bounds if the upper bound is an intersection.
     * @param typeVar The type variable whose bound annotations should be replaces
     * @param annos The annotations to place on typeVar's upperbound
     */
    private static void replaceUpperBoundAnnotations(final AnnotatedTypeVariable typeVar,
                                                     final Collection<AnnotationMirror> annos) {
        final AnnotatedTypeMirror upperBound = typeVar.getUpperBound();
        if (upperBound.getKind() == TypeKind.INTERSECTION) {
            final List<AnnotatedDeclaredType> bounds = ((AnnotatedIntersectionType) upperBound).directSuperTypes();
            for (final AnnotatedDeclaredType bound : bounds) {
                bound.replaceAnnotations(annos);
            }
        } else {
            upperBound.replaceAnnotations(annos);
        }
    }


    private static AnnotatedTypeMirror getDeepestLowerBound(final AnnotatedTypeVariable typeVar) {
        AnnotatedTypeMirror lowerBound = typeVar.getLowerBound();
        while(lowerBound instanceof AnnotatedTypeVariable) {
            lowerBound = ((AnnotatedTypeVariable) lowerBound).getLowerBound();
        }
        return lowerBound;
    }

    /**
     * Detect whether or not the passed in type factory is the GeneralAnnotatedTypeFactory used
     * for constructors in the Nullness type system.
     *
     * HACK: This goes along with the hack described in fixAnnotatedTypeVariableBounds.  Please read
     * that comment.  This should be removed when we fix the Nullness type system.
     * @param typeFactory The type factory we wish to identify
     * @return true if this typeFactory is a GeneralAnnotatedTypeFactory in the nullness type system,
     *         false otherwise.
     */
    public static boolean isNullnessGeneralAtf(final AnnotatedTypeFactory typeFactory) {
        if( typeFactory instanceof GeneralAnnotatedTypeFactory) {
            for(final Class<? extends Annotation> anno : typeFactory.getSupportedTypeQualifiers() ) {
                if( isDefinedInPackage(anno, "org.checkerframework.checker.nullness.qual") ) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Detect if the given type mirror has one annotation in the Nullness and one in the Initialization type
     * system.  If it does, add those to the resulting list, if it doesn't, add the bottom annotation for
     * in the type system of the missing annotation(s).
     *
     * This should only be called when typeFactory is a GeneralAnnotatedTypeFactory for the Nullness/Initialization
     * type system because the GeneralAnnotatedTypeFactory will throw an exception when you use any
     * function that actually needs the hierarchical relationship of qualifiers, like getTopAnnotations or
     * getAnnotationInHierarchy.
     *
     * HACK: This goes along with the hack described in fixAnnotatedTypeVariableBounds.  Please read
     * that comment.  This should be removed when we fix the Nullness type system.
     *
     * @param annotatedTypeMirror
     * @param typeFactory
     * @return
     */
    public static List<AnnotationMirror> getNullnessAndInitAnnos(
            final AnnotatedTypeMirror annotatedTypeMirror, final AnnotatedTypeFactory typeFactory) {
        AnnotationMirror nullness = null;
        AnnotationMirror initialization = null;
        for(final AnnotationMirror anno : annotatedTypeMirror.getAnnotations()) {
            if( isDefinedInPackage(anno, "org.checkerframework.checker.nullness.qual") ) {
                nullness = anno;
            } else if( isDefinedInPackage(anno, "org.checkerframework.checker.initialization.qual") ) {
                initialization = anno;
            }
        }

        if( nullness == null ) {
            nullness = new AnnotationBuilder(typeFactory.getProcessingEnv(), "org.checkerframework.checker.nullness.qual.NonNull").build();
        }

        if( initialization == null ) {
            initialization = new AnnotationBuilder(typeFactory.getProcessingEnv(), "org.checkerframework.checker.initialization.qual.FBCBottom").build();
        }

        return Arrays.asList(nullness, initialization);
    }

    /**
     * TODO: ASK Werner is there a better way to do this
     * Determine if the package name of anno begins with the name of pckage.
     * @param anno The annotation whose package we wish to match
     * @param pckage The package to match against
     * @return true if anno's package name starts with the name of pckage
     *         false otherwise
     */
    private static boolean isDefinedInPackage(final AnnotationMirror anno, final String pckage) {
        final String annoName = AnnotationUtils.annotationName(anno);
        return annoName.startsWith(pckage);
    }
    private static boolean isDefinedInPackage(final Class<? extends Annotation> anno, final String pckage) {
        final String annoPackage = anno.getPackage().getName();
        return annoPackage.equals(pckage);
    }

    /**
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
