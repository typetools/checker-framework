package org.checkerframework.framework.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Implements asSuper {@link AnnotatedTypes#asSuper(AnnotatedTypeFactory, AnnotatedTypeMirror,
 * AnnotatedTypeMirror)}.
 */
public class AsSuperVisitor extends AbstractAtmComboVisitor<AnnotatedTypeMirror, Void> {

    private final Types types;
    private final AnnotatedTypeFactory annotatedTypeFactory;
    /**
     * Whether or not the type being visited is an uninferred type argument. If true, then the
     * underlying type may not have the correct relationship with the supertype.
     */
    private boolean isUninferredTypeAgrument = false;

    public AsSuperVisitor(AnnotatedTypeFactory annotatedTypeFactory) {
        this.annotatedTypeFactory = annotatedTypeFactory;
        types = annotatedTypeFactory.types;
    }

    /**
     * Implements asSuper. See {@link AnnotatedTypes#asSuper(AnnotatedTypeFactory,
     * AnnotatedTypeMirror, AnnotatedTypeMirror)} for details.
     *
     * @param type type from which to copy annotations
     * @param superType a type whose erased Java type is a supertype of {@code type}'s erased Java
     *     type.
     * @return a copy of {@code superType} with annotations copied from {@code type} and type
     *     variables substituted from {@code type}.
     */
    @SuppressWarnings("unchecked")
    public <T extends AnnotatedTypeMirror> T asSuper(AnnotatedTypeMirror type, T superType) {
        if (type == null || superType == null) {
            throw new BugInCF("AsSuperVisitor type and supertype cannot be null.");

        } else if (type == superType) {
            return (T) type.deepCopy();
        }

        // This visitor modifies superType and may return type, so pass it copies so that the
        // parameters to asSuper are not changed and a copy is returned.
        AnnotatedTypeMirror copyType = type.deepCopy();
        AnnotatedTypeMirror copySuperType = superType.deepCopy();
        reset();
        AnnotatedTypeMirror result = visit(copyType, copySuperType, null);

        if (result == null) {
            throw new BugInCF(
                    "AsSuperVisitor returned null.\ntype: %s\nsuperType: %s", type, copySuperType);
        }

        return (T) result;
    }

    private void reset() {
        isUninferredTypeAgrument = false;
    }

    @Override
    public AnnotatedTypeMirror visit(
            AnnotatedTypeMirror type, AnnotatedTypeMirror superType, Void p) {
        ensurePrimaryIsCorrectForUnionsAndIntersections(type);
        return super.visit(type, superType, p);
    }

    /**
     * The code in this class is assuming that the primary annotation of an {@link
     * AnnotatedIntersectionType} is the greatest lower bound of the annotations on its direct super
     * types and that the primary annotation of an {@link AnnotatedUnionType} is the least upper
     * bound of its alternatives. This method makes this assumption true.
     */
    private void ensurePrimaryIsCorrectForUnionsAndIntersections(AnnotatedTypeMirror type) {
        if (type.getKind() == TypeKind.INTERSECTION) {
            AnnotatedIntersectionType intersectionType = (AnnotatedIntersectionType) type;
            Set<AnnotationMirror> glbs = null;
            for (AnnotatedDeclaredType directST : intersectionType.directSuperTypes()) {
                if (glbs == null) {
                    glbs = directST.getAnnotations();
                } else {
                    Set<AnnotationMirror> newGlbs = AnnotationUtils.createAnnotationSet();
                    for (AnnotationMirror glb : glbs) {
                        AnnotationMirror anno = directST.getAnnotationInHierarchy(glb);
                        newGlbs.add(
                                annotatedTypeFactory
                                        .getQualifierHierarchy()
                                        .greatestLowerBound(anno, glb));
                    }
                    glbs = newGlbs;
                }
            }
            type.replaceAnnotations(glbs);
        } else if (type.getKind() == TypeKind.UNION) {
            AnnotatedUnionType annotatedUnionType = (AnnotatedUnionType) type;
            Set<AnnotationMirror> lubs = null;
            for (AnnotatedDeclaredType altern : annotatedUnionType.getAlternatives()) {
                if (lubs == null) {
                    lubs = altern.getAnnotations();
                } else {
                    Set<AnnotationMirror> newLubs = AnnotationUtils.createAnnotationSet();
                    for (AnnotationMirror lub : lubs) {
                        AnnotationMirror anno = altern.getAnnotationInHierarchy(lub);
                        newLubs.add(
                                annotatedTypeFactory
                                        .getQualifierHierarchy()
                                        .leastUpperBound(anno, lub));
                    }
                    lubs = newLubs;
                }
            }
            type.replaceAnnotations(lubs);
        }
    }

    @Override
    protected String defaultErrorMessage(
            AnnotatedTypeMirror type, AnnotatedTypeMirror superType, Void p) {
        return String.format(
                "AsSuperVisitor: Unexpected combination: type: %s superType: %s.\ntype: %s"
                        + "\nsuperType: %s",
                type.getKind(), superType.getKind(), type, superType);
    }

    private AnnotatedTypeMirror errorTypeNotErasedSubtypeOfSuperType(
            AnnotatedTypeMirror type, AnnotatedTypeMirror superType, Void p) {
        if (TypesUtils.isString(superType.getUnderlyingType())) {
            // Any type can be converted to a String
            return visit(annotatedTypeFactory.getStringType(type), superType, p);
        }
        if (isUninferredTypeAgrument) {
            return copyPrimaryAnnos(type, superType);
        }
        throw new BugInCF(
                "AsSuperVisitor: type is not an erased subtype of supertype."
                        + "\ntype: %s\nsuperType: %s",
                type, superType);
    }

    private AnnotatedTypeMirror copyPrimaryAnnos(AnnotatedTypeMirror from, AnnotatedTypeMirror to) {
        // There may have been annotations added by a recursive call to asSuper, so replace existing
        // annotations
        to.replaceAnnotations(new ArrayList<>(from.getAnnotations()));
        // if to is a Typevar or Wildcard, then replaceAnnotations also sets primary annotations on
        // the bounds to from.getAnnotations()

        if (to.getKind() == TypeKind.UNION) {
            // Make sure that the alternatives have a primary annotations
            // Alternatives cannot have type arguments, so asSuper isn't called recursively
            AnnotatedUnionType unionType = (AnnotatedUnionType) to;
            for (AnnotatedDeclaredType altern : unionType.getAlternatives()) {
                altern.addMissingAnnotations(unionType.getAnnotations());
            }
        }
        return to;
    }

    /**
     * A helper method for asSuper(AMT, Wildcard) methods to use to annotate the wildcard's lower
     * bound.
     *
     * <p>If the lower bound of superType is Null, then return copyPrimarayAnnos(type, superType)
     *
     * <p>otherwise, return asSuper(type, superType.getLowerBound()
     *
     * <p>An error is issued if type is a Primitive or Wildcard -- those case are handled in
     * asSuper(Primitive, Wildcard) and asSuper(Wildcard, Wildcard)
     *
     * <p>An error is issued if the lower bound of superType is not Null and type is not a subtype
     * of the lower bound.
     */
    private AnnotatedTypeMirror asSuperWildcardLowerBound(
            AnnotatedTypeMirror type, AnnotatedWildcardType superType, Void p) {
        AnnotatedTypeMirror lowerBound = superType.getSuperBound();
        return asSuperLowerBound(type, p, lowerBound);
    }

    /** Same as #asSuperWildcardLowerBound, but for Typevars. */
    private AnnotatedTypeMirror asSuperTypevarLowerBound(
            AnnotatedTypeMirror type, AnnotatedTypeVariable superType, Void p) {
        AnnotatedTypeMirror lowerBound = superType.getLowerBound();
        return asSuperLowerBound(type, p, lowerBound);
    }

    private AnnotatedTypeMirror asSuperLowerBound(
            AnnotatedTypeMirror type, Void p, AnnotatedTypeMirror lowerBound) {
        if (lowerBound.getKind() == TypeKind.NULL) {
            Set<AnnotationMirror> typeLowerBound =
                    AnnotatedTypes.findEffectiveLowerBoundAnnotations(
                            annotatedTypeFactory.getQualifierHierarchy(), type);
            lowerBound.replaceAnnotations(typeLowerBound);
            return lowerBound;
        }
        if (areErasedJavaTypesEquivalent(type, lowerBound)) {
            return visit(type, lowerBound, p);
        }
        // If type and lowerBound are not the same type, then lowerBound is a subtype of type,
        // but there is no way to convert type to a subtype -- there is not an asSub method.  So,
        // just copy the primary annotations.
        return copyPrimaryAnnos(type, lowerBound);
    }

    private boolean isErasedJavaSubtype(
            AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype) {
        TypeMirror javaSubtype = types.erasure(subtype.getUnderlyingType());
        TypeMirror javaSupertype = types.erasure(supertype.getUnderlyingType());
        return types.isSubtype(javaSubtype, javaSupertype);
    }

    private boolean isErasedJavaSubtype(
            AnnotatedDeclaredType subtype, AnnotatedUnionType supertype) {
        for (AnnotatedDeclaredType alternSuperType : supertype.getAlternatives()) {
            if (!isErasedJavaSubtype(subtype, alternSuperType)) {
                return false;
            }
        }
        return true;
    }

    private boolean areErasedJavaTypesEquivalent(
            AnnotatedTypeMirror typeA, AnnotatedTypeMirror typeB) {
        TypeMirror underlyingTypeA = types.erasure(typeA.getUnderlyingType());
        TypeMirror underlyingTypeB = types.erasure(typeB.getUnderlyingType());
        return types.isSameType(underlyingTypeA, underlyingTypeB);
    }

    // <editor-fold defaultstate="collapsed" desc="visitArray_Other methods">
    @Override
    public AnnotatedTypeMirror visitArray_Array(
            AnnotatedArrayType type, AnnotatedArrayType superType, Void p) {
        AnnotatedTypeMirror asSuperCT =
                visit(type.getComponentType(), superType.getComponentType(), p);
        superType.setComponentType(asSuperCT);
        return copyPrimaryAnnos(type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitArray_Intersection(
            AnnotatedArrayType type, AnnotatedIntersectionType superType, Void p) {
        for (AnnotatedTypeMirror t : superType.directSuperTypes()) {
            if (!(TypesUtils.isObject(t.getUnderlyingType())
                    || TypesUtils.isDeclaredOfName(t.getUnderlyingType(), "java.lang.Cloneable")
                    || TypesUtils.isDeclaredOfName(
                            t.getUnderlyingType(), "java.io.Serializable"))) {
                return errorTypeNotErasedSubtypeOfSuperType(type, superType, p);
            }
            copyPrimaryAnnos(type, t);
        }
        return copyPrimaryAnnos(type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitArray_Declared(
            AnnotatedArrayType type, AnnotatedDeclaredType superType, Void p) {

        TypeElement array = TypesUtils.getTypeElement(type.getUnderlyingType());
        TypeElement possibleArray = TypesUtils.getTypeElement(superType.getUnderlyingType());
        // If the TypeElements of type and superType are equal, then superType's underlyingType is
        // Array.class.  Array.class is the receiver of methods such as clone() of which an array
        // can be the receiver. (new int[].clone())
        boolean isArrayClass = array.equals(possibleArray);

        if (isArrayClass
                || TypesUtils.isObject(superType.getUnderlyingType())
                || TypesUtils.isDeclaredOfName(superType.getUnderlyingType(), "java.lang.Cloneable")
                || TypesUtils.isDeclaredOfName(
                        superType.getUnderlyingType(), "java.io.Serializable")) {
            return copyPrimaryAnnos(type, superType);
        }
        return errorTypeNotErasedSubtypeOfSuperType(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitArray_Typevar(
            AnnotatedArrayType type, AnnotatedTypeVariable superType, Void p) {
        AnnotatedTypeMirror upperBound = visit(type, superType.getUpperBound(), p);
        superType.setUpperBound(upperBound);

        AnnotatedTypeMirror lowerBound = asSuperTypevarLowerBound(type, superType, p);
        superType.setLowerBound(lowerBound);

        return copyPrimaryAnnos(type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitArray_Wildcard(
            AnnotatedArrayType type, AnnotatedWildcardType superType, Void p) {
        AnnotatedTypeMirror upperBound = visit(type, superType.getExtendsBound(), p);
        superType.setExtendsBound(upperBound);

        AnnotatedTypeMirror lowerBound = asSuperWildcardLowerBound(type, superType, p);
        superType.setSuperBound(lowerBound);

        return copyPrimaryAnnos(type, superType);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="visitDeclared_Other methods">
    @Override
    public AnnotatedTypeMirror visitDeclared_Declared(
            AnnotatedDeclaredType type, AnnotatedDeclaredType superType, Void p) {
        if (areErasedJavaTypesEquivalent(type, superType)) {
            return type;
        }

        // Not same erased Java type.
        // Walk up the directSuperTypes.
        // directSuperTypes() annotates type variables correctly and handles substitution.
        for (AnnotatedDeclaredType dst : type.directSuperTypes()) {
            if (isErasedJavaSubtype(dst, superType)) {
                // If two direct supertypes of type, dst1 and dst2, are subtypes of superType then
                // asSuper(dst1, superType) and asSuper(dst2, superType) return equivalent ATMs, so
                // return the first one found.
                return visit(dst, superType, p);
            }
        }

        return errorTypeNotErasedSubtypeOfSuperType(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitDeclared_Intersection(
            AnnotatedDeclaredType type, AnnotatedIntersectionType superType, Void p) {
        List<AnnotatedDeclaredType> newDirectSupertypes = new ArrayList<>();
        // Each type in the intersection must be a supertype of type, so call asSuper on all types
        // in the intersection.
        for (AnnotatedDeclaredType superDirect : superType.directSuperTypes()) {
            if (types.isSubtype(type.getUnderlyingType(), superDirect.getUnderlyingType())) {
                AnnotatedDeclaredType found = (AnnotatedDeclaredType) visit(type, superDirect, p);
                newDirectSupertypes.add(found);
            }
        }
        // The ATM for each type in an intersection is stored in the direct super types field.
        superType.setDirectSuperTypes(newDirectSupertypes);
        return copyPrimaryAnnos(type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitDeclared_Primitive(
            AnnotatedDeclaredType type, AnnotatedPrimitiveType superType, Void p) {
        if (!TypesUtils.isBoxedPrimitive(type.getUnderlyingType())) {
            throw new BugInCF("AsSuperVisitor Declared_Primitive: type is not a box primitive.");
        }
        AnnotatedTypeMirror unboxedType = annotatedTypeFactory.getUnboxedType(type);
        return copyPrimaryAnnos(unboxedType, superType);
    }

    @Override
    public AnnotatedTypeMirror visitDeclared_Typevar(
            AnnotatedDeclaredType type, AnnotatedTypeVariable superType, Void p) {
        AnnotatedTypeMirror upperBound = visit(type, superType.getUpperBound(), p).asUse();
        superType.setUpperBound(upperBound);

        AnnotatedTypeMirror lowerBound = asSuperTypevarLowerBound(type, superType, p).asUse();
        superType.setLowerBound(lowerBound);

        return copyPrimaryAnnos(type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitDeclared_Union(
            AnnotatedDeclaredType type, AnnotatedUnionType superType, Void p) {
        // Alternatives in a union type can't have type args, so just copy the primary annotation
        return copyPrimaryAnnos(type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitDeclared_Wildcard(
            AnnotatedDeclaredType type, AnnotatedWildcardType superType, Void p) {
        AnnotatedTypeMirror upperBound = visit(type, superType.getExtendsBound(), p).asUse();
        superType.setExtendsBound(upperBound);

        AnnotatedTypeMirror lowerBound = asSuperWildcardLowerBound(type, superType, p).asUse();
        superType.setSuperBound(lowerBound);

        return copyPrimaryAnnos(type, superType);
    }

    // </editor-fold>

    /* The primary annotation on an intersection type should be the GLB of the primary annotations of
    the alternatives.  #ensurePrimaryIsCorrectForUnionsAndIntersections ensures that this is the case.

    Example (java) subtyping relationship:
    C <: A & B <: A <:Object, where class C extends A implements B {...}

    Intersection#directSuperType returns a list of the ATM in the intersection.  For example, the
    direct supertypes of  @NonNull (@Nullable A && @NonNull B) are @Nullable A and @NonNull B.
     */
    // <editor-fold defaultstate="collapsed" desc="visitIntersection_Other methods">

    @Override
    public AnnotatedTypeMirror visitIntersection_Declared(
            AnnotatedIntersectionType type, AnnotatedDeclaredType superType, Void p) {
        for (AnnotatedDeclaredType typeDirect : type.directSuperTypes()) {
            // Find the directSuperType that is a subtype of superType,
            // then recur on that type so that type arguments in superType
            // are annotated correctly
            if (isErasedJavaSubtype(typeDirect, superType)) {
                AnnotatedTypeMirror asSuper = visit(typeDirect, superType, p);

                // The directSuperType might have a primary annotation that is a supertype of
                // primary annotation on type. Copy the primary annotation, because it is more
                // precise.
                return copyPrimaryAnnos(type, asSuper);
            }
        }
        return errorTypeNotErasedSubtypeOfSuperType(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitIntersection_Intersection(
            AnnotatedIntersectionType type, AnnotatedIntersectionType superType, Void p) {
        List<AnnotatedDeclaredType> newDirectSupertypes = new ArrayList<>();
        for (AnnotatedDeclaredType superDirect : superType.directSuperTypes()) {
            AnnotatedDeclaredType found = null;
            for (AnnotatedDeclaredType typeDirect : type.directSuperTypes()) {
                if (isErasedJavaSubtype(typeDirect, superDirect)) {
                    found = (AnnotatedDeclaredType) visit(typeDirect, superDirect, p);
                    newDirectSupertypes.add(found);
                    break;
                }
            }
            if (found == null) {
                throw new BugInCF(
                        "AsSuperVisitor visitIntersection_Intersection:\ntype: %s superType: %s",
                        type, superType);
            }
        }
        superType.setDirectSuperTypes(newDirectSupertypes);
        return copyPrimaryAnnos(type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitIntersection_Primitive(
            AnnotatedIntersectionType type, AnnotatedPrimitiveType superType, Void p) {
        for (AnnotatedDeclaredType typeDirect : type.directSuperTypes()) {
            // Find the directSuperType that is a subtype of superType, then recur on that type
            // so that type arguments in superType are annotated correctly
            if (TypesUtils.isBoxedPrimitive(typeDirect.getUnderlyingType())) {
                AnnotatedTypeMirror asSuper = visit(typeDirect, superType, p);

                // The directSuperType might have a primary annotation that is a supertype of
                // primary annotation on type. Copy the primary annotation, because it is more
                // precise.
                return copyPrimaryAnnos(type, asSuper);
            }
        }
        // Cannot happen: one of the types in the intersection must be a subtype of superType.
        throw new BugInCF(
                "AsSuperVisitor visitIntersection_Primitive:\ntype: %s superType: %s",
                type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitIntersection_Typevar(
            AnnotatedIntersectionType type, AnnotatedTypeVariable superType, Void p) {
        AnnotatedTypeMirror upperBound = visit(type, superType.getUpperBound(), p);
        superType.setUpperBound(upperBound);

        AnnotatedTypeMirror lowerBound = asSuperTypevarLowerBound(type, superType, p);
        superType.setLowerBound(lowerBound);

        return copyPrimaryAnnos(type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitIntersection_Union(
            AnnotatedIntersectionType type, AnnotatedUnionType superType, Void p) {
        for (AnnotatedDeclaredType typeDirect : type.directSuperTypes()) {
            if (isErasedJavaSubtype(typeDirect, superType)) {
                AnnotatedTypeMirror asSuper = visit(typeDirect, superType, p);
                return copyPrimaryAnnos(type, asSuper);
            }
        }
        // Cannot happen: one of the types in the intersection must be a subtype of superType.
        throw new BugInCF(
                "AsSuperVisitor visitIntersection_Union:\ntype: %s\nsuperType: %s",
                type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitIntersection_Wildcard(
            AnnotatedIntersectionType type, AnnotatedWildcardType superType, Void p) {
        AnnotatedTypeMirror upperBound = visit(type, superType.getExtendsBound(), p);
        superType.setExtendsBound(upperBound);

        AnnotatedTypeMirror lowerBound = asSuperWildcardLowerBound(type, superType, p);
        superType.setSuperBound(lowerBound);

        return copyPrimaryAnnos(type, superType);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="visitPrimitive_Other methods">

    @Override
    public AnnotatedTypeMirror visitPrimitive_Primitive(
            AnnotatedPrimitiveType type, AnnotatedPrimitiveType superType, Void p) {
        return copyPrimaryAnnos(type, superType);
    }

    private AnnotatedTypeMirror visitPrimitive_Other(
            AnnotatedPrimitiveType type, AnnotatedTypeMirror superType, Void p) {
        return visit(annotatedTypeFactory.getBoxedType(type), superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitPrimitive_Declared(
            AnnotatedPrimitiveType type, AnnotatedDeclaredType superType, Void p) {
        if (TypesUtils.isBoxedPrimitive(superType.getUnderlyingType())) {
            TypeMirror unboxedSuper = types.unboxedType(superType.getUnderlyingType());
            if (unboxedSuper.getKind() != type.getKind()
                    && canBeNarrowingPrimitiveConversion(unboxedSuper)) {
                AnnotatedPrimitiveType narrowedType =
                        annotatedTypeFactory.getNarrowedPrimitive(type, unboxedSuper);
                return visit(narrowedType, superType, p);
            }
        }
        return visitPrimitive_Other(type, superType, p);
    }

    /** All other narrowings require a cast. For example: Byte b = 100; See JLS 5.1.3 */
    private boolean canBeNarrowingPrimitiveConversion(TypeMirror type) {
        // org.checkerframework.dataflow.cfg.CFGBuilder
        // .CFGTranslationPhaseOne#conversionRequiresNarrowing()
        TypeMirror unboxedType = TypesUtils.isBoxedPrimitive(type) ? types.unboxedType(type) : type;
        TypeKind unboxedKind = unboxedType.getKind();
        return unboxedKind == TypeKind.BYTE
                || unboxedKind == TypeKind.SHORT
                || unboxedKind == TypeKind.CHAR;
    }

    @Override
    public AnnotatedTypeMirror visitPrimitive_Intersection(
            AnnotatedPrimitiveType type, AnnotatedIntersectionType superType, Void p) {
        return visitPrimitive_Other(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitPrimitive_Typevar(
            AnnotatedPrimitiveType type, AnnotatedTypeVariable superType, Void p) {
        return visitPrimitive_Other(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitPrimitive_Union(
            AnnotatedPrimitiveType type, AnnotatedUnionType superType, Void p) {
        return visitPrimitive_Other(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitPrimitive_Wildcard(
            AnnotatedPrimitiveType type, AnnotatedWildcardType superType, Void p) {
        return visitPrimitive_Other(type, superType, p);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="visitTypevar_Other methods">
    private AnnotatedTypeMirror visitTypevar_NotTypevarNorWildcard(
            AnnotatedTypeVariable type, AnnotatedTypeMirror superType, Void p) {
        AnnotatedTypeMirror asSuper = visit(type.getUpperBound(), superType, p);
        return copyPrimaryAnnos(type, asSuper);
    }

    @Override
    public AnnotatedTypeMirror visitTypevar_Declared(
            AnnotatedTypeVariable type, AnnotatedDeclaredType superType, Void p) {
        return visitTypevar_NotTypevarNorWildcard(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitTypevar_Intersection(
            AnnotatedTypeVariable type, AnnotatedIntersectionType superType, Void p) {
        return visitTypevar_NotTypevarNorWildcard(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitTypevar_Primitive(
            AnnotatedTypeVariable type, AnnotatedPrimitiveType superType, Void p) {
        return visitTypevar_NotTypevarNorWildcard(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitTypevar_Typevar(
            AnnotatedTypeVariable type, AnnotatedTypeVariable superType, Void p) {
        // Clear the superType annotations and copy over the primary annotations before computing
        // bounds, so that the superType annotations don't override the type annotations on the
        // bounds.
        superType.clearAnnotations();
        copyPrimaryAnnos(type, superType);

        AnnotatedTypeMirror upperBound = visit(type.getUpperBound(), superType.getUpperBound(), p);
        superType.setUpperBound(upperBound);

        AnnotatedTypeMirror lowerBound;
        if (type.getLowerBound().getKind() == TypeKind.NULL
                && superType.getLowerBound().getKind() == TypeKind.NULL) {
            lowerBound = copyPrimaryAnnos(type.getLowerBound(), superType.getLowerBound());
        } else if (type.getLowerBound().getKind() == TypeKind.NULL) {
            lowerBound = visit(type, superType.getLowerBound(), p);
        } else {
            lowerBound = asSuperTypevarLowerBound(type.getLowerBound(), superType, p);
        }
        superType.setLowerBound(lowerBound);

        return superType;
    }

    @Override
    public AnnotatedTypeMirror visitTypevar_Union(
            AnnotatedTypeVariable type, AnnotatedUnionType superType, Void p) {
        return visitTypevar_NotTypevarNorWildcard(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitTypevar_Wildcard(
            AnnotatedTypeVariable type, AnnotatedWildcardType superType, Void p) {
        AnnotatedTypeMirror upperBound =
                visit(type.getUpperBound(), superType.getExtendsBound(), p);
        superType.setExtendsBound(upperBound);

        AnnotatedTypeMirror lowerBound;
        if (type.getLowerBound().getKind() == TypeKind.NULL
                && superType.getSuperBound().getKind() == TypeKind.NULL) {
            lowerBound = copyPrimaryAnnos(type.getLowerBound(), superType.getSuperBound());
        } else if (type.getLowerBound().getKind() == TypeKind.NULL) {
            lowerBound = visit(type, superType.getSuperBound(), p);
        } else {
            lowerBound = asSuperWildcardLowerBound(type.getLowerBound(), superType, p);
        }
        superType.setSuperBound(lowerBound);

        return copyPrimaryAnnos(type, superType);
    }
    // </editor-fold>

    /* The primary annotation on a union type is the LUB of the primary annotations on its direct
    supertypes. #ensurePrimaryIsCorrectForUnionsAndIntersections ensures that this is the case.

    All the alternatives in a union type must be subtype of Throwable and cannot have type arguments;
    however, a union type can be a subtype of an interface with a type argument. For example:
    interface Interface<T>{}
    class MyException1 extends Throwable implements Interface<Number>{}
    class MyException2 extends Throwable implements Interface<Number>{}

    MyException1 <: MyException1 | MyException2 <: Interface<Number>
    MyException1 | MyException2 <: Throwable & Interface<Number>
    */
    // <editor-fold defaultstate="collapsed" desc="visitUnion_Other methods">

    private AnnotatedTypeMirror visitUnion_Other(
            AnnotatedUnionType type, AnnotatedTypeMirror superType, Void p) {
        // asSuper on any of the alternatives is the same, so just use the first one.
        AnnotatedTypeMirror asSuper = visit(type.getAlternatives().get(0), superType, p);
        return copyPrimaryAnnos(type, asSuper);
    }

    @Override
    public AnnotatedTypeMirror visitUnion_Declared(
            AnnotatedUnionType type, AnnotatedDeclaredType superType, Void p) {
        return visitUnion_Other(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitUnion_Intersection(
            AnnotatedUnionType type, AnnotatedIntersectionType superType, Void p) {
        return visitUnion_Other(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitUnion_Typevar(
            AnnotatedUnionType type, AnnotatedTypeVariable superType, Void p) {
        return visitUnion_Other(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitUnion_Union(
            AnnotatedUnionType type, AnnotatedUnionType superType, Void p) {
        for (AnnotatedTypeMirror superAltern : superType.getAlternatives()) {
            copyPrimaryAnnos(type, superAltern);
        }
        return copyPrimaryAnnos(type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitUnion_Wildcard(
            AnnotatedUnionType type, AnnotatedWildcardType superType, Void p) {
        return visitUnion_Other(type, superType, p);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="visitWildCard_Other methods">

    private AnnotatedTypeMirror visitWildcard_NotTypvarNorWildcard(
            AnnotatedWildcardType type, AnnotatedTypeMirror superType, Void p) {
        boolean oldIsUninferredTypeArgument = isUninferredTypeAgrument;
        if (type.isUninferredTypeArgument()) {
            isUninferredTypeAgrument = true;
        }
        AnnotatedTypeMirror asSuper = visit(type.getExtendsBound(), superType, p);
        isUninferredTypeAgrument = oldIsUninferredTypeArgument;
        annotatedTypeFactory.addDefaultAnnotations(superType);

        return copyPrimaryAnnos(type, asSuper);
    }

    @Override
    public AnnotatedTypeMirror visitWildcard_Array(
            AnnotatedWildcardType type, AnnotatedArrayType superType, Void p) {
        return visitWildcard_NotTypvarNorWildcard(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitWildcard_Declared(
            AnnotatedWildcardType type, AnnotatedDeclaredType superType, Void p) {
        return visitWildcard_NotTypvarNorWildcard(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitWildcard_Intersection(
            AnnotatedWildcardType type, AnnotatedIntersectionType superType, Void p) {
        return visitWildcard_NotTypvarNorWildcard(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitWildcard_Primitive(
            AnnotatedWildcardType type, AnnotatedPrimitiveType superType, Void p) {
        return visitWildcard_NotTypvarNorWildcard(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitWildcard_Typevar(
            AnnotatedWildcardType type, AnnotatedTypeVariable superType, Void p) {
        boolean oldIsUninferredTypeArgument = isUninferredTypeAgrument;
        if (type.isUninferredTypeArgument()) {
            isUninferredTypeAgrument = true;
        }
        AnnotatedTypeMirror upperBound =
                visit(type.getExtendsBound(), superType.getUpperBound(), p);
        superType.setUpperBound(upperBound);

        AnnotatedTypeMirror lowerBound;
        if (type.getSuperBound().getKind() == TypeKind.NULL
                && superType.getLowerBound().getKind() == TypeKind.NULL) {
            lowerBound = copyPrimaryAnnos(type.getSuperBound(), superType.getLowerBound());
        } else if (type.getSuperBound().getKind() == TypeKind.NULL) {
            lowerBound = visit(type, superType.getLowerBound(), p);
        } else {
            lowerBound = asSuperTypevarLowerBound(type.getSuperBound(), superType, p);
        }
        superType.setLowerBound(lowerBound);
        isUninferredTypeAgrument = oldIsUninferredTypeArgument;
        annotatedTypeFactory.addDefaultAnnotations(superType);

        return copyPrimaryAnnos(type, superType);
    }

    @Override
    public AnnotatedTypeMirror visitWildcard_Union(
            AnnotatedWildcardType type, AnnotatedUnionType superType, Void p) {
        return visitWildcard_NotTypvarNorWildcard(type, superType, p);
    }

    @Override
    public AnnotatedTypeMirror visitWildcard_Wildcard(
            AnnotatedWildcardType type, AnnotatedWildcardType superType, Void p) {
        boolean oldIsUninferredTypeArgument = isUninferredTypeAgrument;
        if (type.isUninferredTypeArgument()) {
            isUninferredTypeAgrument = true;
            superType.setUninferredTypeArgument();
        }
        if (types.isSubtype(
                type.getExtendsBound().getUnderlyingType(),
                superType.getExtendsBound().getUnderlyingType())) {
            AnnotatedTypeMirror upperBound =
                    visit(type.getExtendsBound(), superType.getExtendsBound(), p);
            superType.setExtendsBound(upperBound);
        } else {
            // The upper bound of a wildcard can be a super type of upper bound of the type
            // parameter for which it is an argument.
            // See org.checkerframework.framework.type.AnnotatedTypeFactory.widenToUpperBound for an
            // example.  In these cases, the upper bound of type might be a super type of the
            // upper bound of superType.

            // The underlying type of the annotated type mirror returned by asSuper must be the
            // same as the passed type, so just copy the primary annotations.
            copyPrimaryAnnos(type.getExtendsBound(), superType.getExtendsBound());

            // Add defaults in case any locations are missing annotations.
            annotatedTypeFactory.addDefaultAnnotations(superType.getExtendsBound());
        }

        AnnotatedTypeMirror lowerBound;
        if (type.getSuperBound().getKind() == TypeKind.NULL
                && superType.getSuperBound().getKind() == TypeKind.NULL) {
            lowerBound = copyPrimaryAnnos(type.getSuperBound(), superType.getSuperBound());
        } else if (type.getSuperBound().getKind() == TypeKind.NULL) {
            lowerBound = visit(type, superType.getSuperBound(), p);
        } else {
            lowerBound = asSuperWildcardLowerBound(type.getSuperBound(), superType, p);
        }
        superType.setSuperBound(lowerBound);
        isUninferredTypeAgrument = oldIsUninferredTypeArgument;
        annotatedTypeFactory.addDefaultAnnotations(superType);

        return copyPrimaryAnnos(type, superType);
    }

    public boolean sameAnnotatedTypeFactory(AnnotatedTypeFactory annotatedTypeFactory) {
        return this.annotatedTypeFactory == annotatedTypeFactory;
    }
    // </editor-fold>
}
