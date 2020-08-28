package org.checkerframework.framework.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.WildcardType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AsSuperVisitor;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.SyntheticArrays;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.UtilPlume;

/**
 * Utility methods for operating on {@code AnnotatedTypeMirror}. This class mimics the class {@link
 * Types}.
 */
public class AnnotatedTypes {
    // Class cannot be instantiated.
    private AnnotatedTypes() {
        throw new AssertionError("Class AnnotatedTypes cannot be instantiated.");
    }

    private static AsSuperVisitor asSuperVisitor;

    /**
     * Copies annotations from {@code type} to a copy of {@code superType} where the type variables
     * of {@code superType} have been substituted. How the annotations are copied depends on the
     * kinds of AnnotatedTypeMirrors given. Generally, if {@code type} and {@code superType} are
     * both declared types, asSuper is called recursively on the direct super types, see {@link
     * AnnotatedTypeMirror#directSuperTypes()}, of {@code type} until {@code type}'s erased Java
     * type is the same as {@code superType}'s erased super type. Then {@code type is returned}. For
     * compound types, asSuper is called recursively on components.
     *
     * <p>Preconditions:<br>
     * {@code superType} may have annotations, but they are ignored. <br>
     * {@code type} may not be an instanceof AnnotatedNullType, because if {@code superType} is a
     * compound type, the annotations on the component types are undefined.<br>
     * The underlying {@code type} (ie the Java type) of {@code type} should be a subtype (or the
     * same type) of the underlying type of {@code superType}. Except for these cases:
     *
     * <ul>
     *   <li>If {@code type} is a primitive, then the boxed type of {@code type} must be subtype of
     *       {@code superType}.
     *   <li>If {@code superType} is a primitive, then {@code type} must be convertible to {@code
     *       superType}.
     *   <li>If {@code superType} is a type variable or wildcard without a lower bound, then {@code
     *       type} must be a subtype of the upper bound of {@code superType}. (This relaxed rule is
     *       used during type argument inference where the type variable or wildcard is the type
     *       argument that was inferred.)
     *   <li>If {@code superType} is a wildcard with a lower bound, then {@code type} must be a
     *       subtype of the lower bound of {@code superType}.
     * </ul>
     *
     * <p>Postconditions: {@code type} and {@code superType} are not modified.
     *
     * @param atypeFactory {@link AnnotatedTypeFactory}
     * @param type type from which to copy annotations
     * @param superType a type whose erased Java type is a supertype of {@code type}'s erased Java
     *     type.
     * @return {@code superType} with annotations copied from {@code type} and type variables
     *     substituted from {@code type}.
     */
    public static <T extends AnnotatedTypeMirror> T asSuper(
            AnnotatedTypeFactory atypeFactory, AnnotatedTypeMirror type, T superType) {
        if (asSuperVisitor == null || !asSuperVisitor.sameAnnotatedTypeFactory(atypeFactory)) {
            asSuperVisitor = new AsSuperVisitor(atypeFactory);
        }
        return asSuperVisitor.asSuper(type, superType);
    }

    /**
     * Calls asSuper and casts the result to the same type as the input supertype.
     *
     * @param subtype subtype to be transformed to supertype
     * @param supertype supertype that subtype is transformed to
     * @param <T> the type of supertype and return type
     * @return subtype as an instance of supertype
     */
    public static <T extends AnnotatedTypeMirror> T castedAsSuper(
            final AnnotatedTypeFactory atypeFactory,
            final AnnotatedTypeMirror subtype,
            final T supertype) {
        final Types types = atypeFactory.getProcessingEnv().getTypeUtils();
        final Elements elements = atypeFactory.getProcessingEnv().getElementUtils();

        if (subtype.getKind() == TypeKind.NULL) {
            // Make a copy of the supertype so that if supertype is a composite type, the
            // returned type will be fully annotated.  (For example, if sub is @C null and super is
            // @A List<@B String>, then the returned type is @C List<@B String>.)
            @SuppressWarnings("unchecked")
            T copy = (T) supertype.deepCopy();
            copy.replaceAnnotations(subtype.getAnnotations());
            return copy;
        }

        final T asSuperType = AnnotatedTypes.asSuper(atypeFactory, subtype, supertype);

        fixUpRawTypes(subtype, asSuperType, supertype, types);

        // if we have a type for enum MyEnum {...}
        // When the supertype is the declaration of java.lang.Enum<E>, MyEnum values become
        // Enum<MyEnum>.  Where really, we would like an Enum<E> with the annotations from
        // Enum<MyEnum> are transferred to Enum<E>.  That is, if we have a type:
        // @1 Enum<@2 MyEnum>
        // asSuper should return:
        // @1 Enum<E extends @2 Enum<E>>
        if (asSuperType != null
                && AnnotatedTypes.isEnum(asSuperType)
                && AnnotatedTypes.isDeclarationOfJavaLangEnum(types, elements, supertype)) {
            final AnnotatedDeclaredType resultAtd = ((AnnotatedDeclaredType) supertype).deepCopy();
            resultAtd.clearAnnotations();
            resultAtd.addAnnotations(asSuperType.getAnnotations());

            final AnnotatedDeclaredType asSuperAdt = (AnnotatedDeclaredType) asSuperType;
            if (!resultAtd.getTypeArguments().isEmpty()
                    && !asSuperAdt.getTypeArguments().isEmpty()) {
                final AnnotatedTypeMirror sourceTypeArg = asSuperAdt.getTypeArguments().get(0);
                final AnnotatedTypeMirror resultTypeArg = resultAtd.getTypeArguments().get(0);
                resultTypeArg.clearAnnotations();
                if (resultTypeArg.getKind() == TypeKind.TYPEVAR) {
                    // Only change the upper bound of a type variable.
                    AnnotatedTypeVariable resultTypeArgTV = (AnnotatedTypeVariable) resultTypeArg;
                    resultTypeArgTV.getUpperBound().addAnnotations(sourceTypeArg.getAnnotations());
                } else {
                    resultTypeArg.addAnnotations(sourceTypeArg.getEffectiveAnnotations());
                }
                @SuppressWarnings("unchecked")
                T result = (T) resultAtd;
                return result;
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
        if (asSuperType == null
                || asSuperType.getKind() != TypeKind.DECLARED
                || originalSubtype.getKind() != TypeKind.DECLARED) {
            return;
        }

        final AnnotatedDeclaredType declaredAsSuper = (AnnotatedDeclaredType) asSuperType;
        final AnnotatedDeclaredType declaredSubtype = (AnnotatedDeclaredType) originalSubtype;

        if (!declaredAsSuper.wasRaw()
                || !declaredAsSuper.getTypeArguments().isEmpty()
                || declaredSubtype.getTypeArguments().isEmpty()) {
            return;
        }

        Set<Pair<Integer, Integer>> typeArgMap =
                TypeArgumentMapper.mapTypeArgumentIndices(
                        (TypeElement) declaredSubtype.getUnderlyingType().asElement(),
                        (TypeElement) declaredAsSuper.getUnderlyingType().asElement(),
                        types);

        if (typeArgMap.size() != declaredSubtype.getTypeArguments().size()) {
            return;
        }

        List<AnnotatedTypeMirror> newTypeArgs = new ArrayList<>();

        List<Pair<Integer, Integer>> orderedByDestination = new ArrayList<>(typeArgMap);
        Collections.sort(
                orderedByDestination,
                new Comparator<Pair<Integer, Integer>>() {
                    @Override
                    public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
                        return o1.second - o2.second;
                    }
                });

        final List<? extends AnnotatedTypeMirror> subTypeArgs = declaredSubtype.getTypeArguments();
        if (typeArgMap.size() == ((AnnotatedDeclaredType) supertype).getTypeArguments().size()) {
            for (Pair<Integer, Integer> mapping : orderedByDestination) {
                newTypeArgs.add(subTypeArgs.get(mapping.first).deepCopy());
            }
        }
        declaredAsSuper.setTypeArguments(newTypeArgs);
    }

    /** This method identifies wildcard types that are unbound. */
    public static boolean hasNoExplicitBound(final AnnotatedTypeMirror wildcard) {
        return ((Type.WildcardType) wildcard.getUnderlyingType()).isUnbound();
    }

    /**
     * This method identifies wildcard types that have an explicit super bound. NOTE:
     * Type.WildcardType.isSuperBound will return true for BOTH unbound and super bound wildcards
     * which necessitates this method
     */
    public static boolean hasExplicitSuperBound(final AnnotatedTypeMirror wildcard) {
        final Type.WildcardType wildcardType = (Type.WildcardType) wildcard.getUnderlyingType();
        return wildcardType.isSuperBound()
                && !((WildcardType) wildcard.getUnderlyingType()).isUnbound();
    }

    /**
     * This method identifies wildcard types that have an explicit extends bound. NOTE:
     * Type.WildcardType.isExtendsBound will return true for BOTH unbound and extends bound
     * wildcards which necessitates this method
     */
    public static boolean hasExplicitExtendsBound(final AnnotatedTypeMirror wildcard) {
        final Type.WildcardType wildcardType = (Type.WildcardType) wildcard.getUnderlyingType();
        return wildcardType.isExtendsBound()
                && !((WildcardType) wildcard.getUnderlyingType()).isUnbound();
    }

    /**
     * Return the base type of type or any of its outer types that starts with the given type. If
     * none exists, return null.
     *
     * @param type a type
     * @param superType a type
     */
    private static AnnotatedTypeMirror asOuterSuper(
            Types types,
            AnnotatedTypeFactory atypeFactory,
            AnnotatedTypeMirror type,
            AnnotatedTypeMirror superType) {
        if (type.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType dt = (AnnotatedDeclaredType) type;
            AnnotatedDeclaredType enclosingType = dt;
            TypeMirror superTypeMirror = types.erasure(superType.getUnderlyingType());
            while (enclosingType != null) {
                TypeMirror enclosingTypeMirror = types.erasure(enclosingType.getUnderlyingType());
                if (types.isSubtype(enclosingTypeMirror, superTypeMirror)) {
                    dt = enclosingType;
                    break;
                }
                enclosingType = enclosingType.getEnclosingType();
            }
            if (enclosingType == null) {
                // TODO: https://github.com/typetools/checker-framework/issues/724
                // testcase javacheck -processor nullness  src/java/util/AbstractMap.java
                //                SourceChecker checker =  atypeFactory.getContext().getChecker();
                //                String msg = (String.format("OuterAsSuper did not find outer
                // class. type: %s superType: %s", type, superType));
                //                checker.message(Kind.WARNING, msg);
                return superType;
            }
            return asSuper(atypeFactory, dt, superType);
        }
        return asSuper(atypeFactory, type, superType);
    }

    /**
     * Specialization of {@link #asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror,
     * Element)} with more precise return type.
     *
     * @see #asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror, Element)
     * @param types the Types instance to use
     * @param atypeFactory the type factory to use
     * @param t the receiver type
     * @param elem the element that should be viewed as member of t
     * @return the type of elem as member of t
     */
    public static AnnotatedExecutableType asMemberOf(
            Types types,
            AnnotatedTypeFactory atypeFactory,
            AnnotatedTypeMirror t,
            ExecutableElement elem) {
        return (AnnotatedExecutableType) asMemberOf(types, atypeFactory, t, (Element) elem);
    }

    /**
     * Specialization of {@link #asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror,
     * Element, AnnotatedTypeMirror)} with more precise return type.
     *
     * @see #asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror, Element,
     *     AnnotatedTypeMirror)
     * @param types the Types instance to use
     * @param atypeFactory the type factory to use
     * @param t the receiver type
     * @param elem the element that should be viewed as member of t
     * @param type unsubstituted type of member
     * @return the type of member as member of of, with initial type memberType; can be an alias to
     *     memberType
     */
    public static AnnotatedExecutableType asMemberOf(
            Types types,
            AnnotatedTypeFactory atypeFactory,
            AnnotatedTypeMirror t,
            ExecutableElement elem,
            AnnotatedExecutableType type) {
        return (AnnotatedExecutableType) asMemberOf(types, atypeFactory, t, (Element) elem, type);
    }

    /**
     * Returns the type of an element when that element is viewed as a member of, or otherwise
     * directly contained by, a given type.
     *
     * <p>For example, when viewed as a member of the parameterized type {@code Set<@NonNull
     * String>}, the {@code Set.add} method is an {@code ExecutableType} whose parameter is of type
     * {@code @NonNull String}.
     *
     * <p>The result is customized according to the type system semantics, according to {@link
     * AnnotatedTypeFactory#postAsMemberOf(AnnotatedTypeMirror, AnnotatedTypeMirror, Element)}.
     *
     * <p>Note that this method does not currently return (top level) captured types for type
     * parameters, parameters, and return types. Instead, the original wildcard is returned, or
     * sometimes inferring type arguments will create a wildcard type which is returned. The bounds
     * of an inferred wildcard may itself have captures.
     *
     * <p>To prevent unsoundness, the rest of the checker framework must expect wildcard in places
     * where captures should appear (like type arguments). This should just involve the bounds of
     * the wildcard where the bounds of the capture would have been used.
     *
     * @param types the Types instance to use
     * @param atypeFactory the type factory to use
     * @param t the receiver type
     * @param elem the element that should be viewed as member of t
     * @return the type of elem as member of t
     */
    public static AnnotatedTypeMirror asMemberOf(
            Types types, AnnotatedTypeFactory atypeFactory, AnnotatedTypeMirror t, Element elem) {
        final AnnotatedTypeMirror memberType = atypeFactory.getAnnotatedType(elem);
        return asMemberOf(types, atypeFactory, t, elem, memberType);
    }

    /**
     * Returns the type of an element when that element is viewed as a member of, or otherwise
     * directly contained by, a given type. An initial type for the member is provided, to allow for
     * earlier changes to the declared type of elem. For example, polymorphic qualifiers must be
     * substituted before type variables are substituted.
     *
     * @param types the Types instance to use
     * @param atypeFactory the type factory to use
     * @param t the receiver type
     * @param elem the element that should be viewed as member of t
     * @param elemType unsubstituted type of elem
     * @return the type of elem as member of t
     * @see #asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror, Element)
     */
    public static AnnotatedTypeMirror asMemberOf(
            Types types,
            AnnotatedTypeFactory atypeFactory,
            AnnotatedTypeMirror t,
            Element elem,
            AnnotatedTypeMirror elemType) {
        // asMemberOf is only for fields, variables, and methods!
        // Otherwise, simply use fromElement.
        switch (elem.getKind()) {
            case PACKAGE:
            case INSTANCE_INIT:
            case OTHER:
            case STATIC_INIT:
            case TYPE_PARAMETER:
                return elemType;
            default:
                AnnotatedTypeMirror res = asMemberOfImpl(types, atypeFactory, t, elem, elemType);
                if (!ElementUtils.isStatic(elem)) {
                    atypeFactory.postAsMemberOf(res, t, elem);
                }
                return res;
        }
    }

    /**
     * Helper for {@link AnnotatedTypes#asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror,
     * Element)}.
     *
     * @param types the Types instance to use
     * @param atypeFactory the type factory to use
     * @param of the receiver type
     * @param member the element that should be viewed as member of of
     * @param memberType unsubstituted type of member
     * @return the type of member as member of of, with initial type memberType; can be an alias to
     *     memberType
     */
    private static AnnotatedTypeMirror asMemberOfImpl(
            final Types types,
            final AnnotatedTypeFactory atypeFactory,
            final AnnotatedTypeMirror of,
            final Element member,
            final AnnotatedTypeMirror memberType) {
        if (ElementUtils.isStatic(member)) {
            return memberType;
        }

        switch (of.getKind()) {
            case ARRAY:
                // Method references like String[]::clone should have a return type of String[]
                // rather than Object
                if (SyntheticArrays.isArrayClone(of, member)) {
                    return SyntheticArrays.replaceReturnType(member, (AnnotatedArrayType) of);
                }
                return memberType;
            case TYPEVAR:
                return asMemberOf(
                        types,
                        atypeFactory,
                        ((AnnotatedTypeVariable) of).getUpperBound(),
                        member,
                        memberType);
            case WILDCARD:
                if (((AnnotatedWildcardType) of).isUninferredTypeArgument()) {
                    return substituteUninferredTypeArgs(atypeFactory, member, memberType);
                }
                return asMemberOf(
                        types,
                        atypeFactory,
                        ((AnnotatedWildcardType) of).getExtendsBound().deepCopy(),
                        member,
                        memberType);
            case INTERSECTION:
                AnnotatedTypeMirror iter = memberType;
                for (AnnotatedDeclaredType superType :
                        ((AnnotatedIntersectionType) of).directSuperTypes()) {
                    iter = substituteTypeVariables(types, atypeFactory, superType, member, iter);
                }
                return iter;
            case UNION:
            case DECLARED:
                return substituteTypeVariables(types, atypeFactory, of, member, memberType);
            default:
                throw new BugInCF("asMemberOf called on unexpected type.%nt: %s", of);
        }
    }

    private static AnnotatedTypeMirror substituteTypeVariables(
            Types types,
            AnnotatedTypeFactory atypeFactory,
            AnnotatedTypeMirror of,
            Element member,
            AnnotatedTypeMirror memberType) {

        // Basic Algorithm:
        // 1. Find the enclosingClassOfMember of the element
        // 2. Find the base type of enclosingClassOfMember (e.g. type of enclosingClassOfMember as
        //      supertype of passed type)
        // 3. Substitute for type variables if any exist
        TypeElement enclosingClassOfMember = ElementUtils.enclosingClass(member);
        final Map<TypeVariable, AnnotatedTypeMirror> mappings = new HashMap<>();

        // Look for all enclosing classes that have type variables
        // and collect type to be substituted for those type variables
        while (enclosingClassOfMember != null) {
            addTypeVarMappings(types, atypeFactory, of, enclosingClassOfMember, mappings);
            enclosingClassOfMember =
                    ElementUtils.enclosingClass(enclosingClassOfMember.getEnclosingElement());
        }

        if (!mappings.isEmpty()) {
            memberType = atypeFactory.getTypeVarSubstitutor().substitute(mappings, memberType);
        }

        return memberType;
    }

    private static void addTypeVarMappings(
            Types types,
            AnnotatedTypeFactory atypeFactory,
            AnnotatedTypeMirror t,
            TypeElement enclosingClassOfElem,
            Map<TypeVariable, AnnotatedTypeMirror> mappings) {
        if (enclosingClassOfElem.getTypeParameters().isEmpty()) {
            return;
        }
        AnnotatedDeclaredType enclosingType = atypeFactory.getAnnotatedType(enclosingClassOfElem);
        AnnotatedDeclaredType base =
                (AnnotatedDeclaredType) asOuterSuper(types, atypeFactory, t, enclosingType);

        final List<AnnotatedTypeVariable> ownerParams =
                new ArrayList<>(enclosingType.getTypeArguments().size());
        for (final AnnotatedTypeMirror typeParam : enclosingType.getTypeArguments()) {
            if (typeParam.getKind() != TypeKind.TYPEVAR) {
                throw new BugInCF(
                        UtilPlume.joinLines(
                                "Type arguments of a declaration should be type variables",
                                "enclosingClassOfElem=" + enclosingClassOfElem,
                                "enclosingType=" + enclosingType,
                                "typeMirror=" + t));
            }
            ownerParams.add((AnnotatedTypeVariable) typeParam);
        }

        List<AnnotatedTypeMirror> baseParams = base.getTypeArguments();
        if (ownerParams.size() != baseParams.size() && !base.wasRaw()) {
            throw new BugInCF(
                    UtilPlume.joinLines(
                            "Unexpected number of parameters.",
                            "enclosingType=" + enclosingType,
                            "baseType=" + base));
        }
        if (!ownerParams.isEmpty() && baseParams.isEmpty() && base.wasRaw()) {
            List<AnnotatedTypeMirror> newBaseParams = new ArrayList<>();
            for (AnnotatedTypeVariable arg : ownerParams) {
                // If base type was raw and the type arguments are missing,
                // set them to the erased type of the type variable.
                // (which is the erased type of the upper bound.)
                newBaseParams.add(arg.getErased());
            }
            baseParams = newBaseParams;
        }

        for (int i = 0; i < ownerParams.size(); ++i) {
            mappings.put(ownerParams.get(i).getUnderlyingType(), baseParams.get(i));
        }
    }

    /** Substitutes uninferred type arguments for type variables in {@code memberType}. */
    private static AnnotatedTypeMirror substituteUninferredTypeArgs(
            AnnotatedTypeFactory atypeFactory, Element member, AnnotatedTypeMirror memberType) {
        TypeElement enclosingClassOfMember = ElementUtils.enclosingClass(member);
        final Map<TypeVariable, AnnotatedTypeMirror> mappings = new HashMap<>();

        while (enclosingClassOfMember != null) {
            if (!enclosingClassOfMember.getTypeParameters().isEmpty()) {
                AnnotatedDeclaredType enclosingType =
                        atypeFactory.getAnnotatedType(enclosingClassOfMember);
                for (final AnnotatedTypeMirror type : enclosingType.getTypeArguments()) {
                    AnnotatedTypeVariable typeParameter = (AnnotatedTypeVariable) type;
                    mappings.put(
                            typeParameter.getUnderlyingType(),
                            atypeFactory.getUninferredWildcardType(typeParameter));
                }
            }
            enclosingClassOfMember =
                    ElementUtils.enclosingClass(enclosingClassOfMember.getEnclosingElement());
        }

        if (!mappings.isEmpty()) {
            return atypeFactory.getTypeVarSubstitutor().substitute(mappings, memberType);
        }

        return memberType;
    }

    /**
     * Returns the iterated type of the passed iterable type, and throws {@link
     * IllegalArgumentException} if the passed type is not iterable.
     *
     * <p>The iterated type is the component type of an array, and the type argument of {@link
     * Iterable} for declared types.
     *
     * @param iterableType the iterable type (either array or declared)
     * @return the types of elements in the iterable type
     */
    public static AnnotatedTypeMirror getIteratedType(
            ProcessingEnvironment processingEnv,
            AnnotatedTypeFactory atypeFactory,
            AnnotatedTypeMirror iterableType) {
        if (iterableType.getKind() == TypeKind.ARRAY) {
            return ((AnnotatedArrayType) iterableType).getComponentType();
        }

        // For type variables and wildcards take the effective upper bound.
        if (iterableType.getKind() == TypeKind.WILDCARD) {
            return getIteratedType(
                    processingEnv,
                    atypeFactory,
                    ((AnnotatedWildcardType) iterableType).getExtendsBound().deepCopy());
        }
        if (iterableType.getKind() == TypeKind.TYPEVAR) {
            return getIteratedType(
                    processingEnv,
                    atypeFactory,
                    ((AnnotatedTypeVariable) iterableType).getUpperBound());
        }

        if (iterableType.getKind() != TypeKind.DECLARED) {
            throw new BugInCF("AnnotatedTypes.getIteratedType: not iterable type: " + iterableType);
        }

        TypeElement iterableElement =
                processingEnv.getElementUtils().getTypeElement("java.lang.Iterable");
        AnnotatedDeclaredType iterableElmType = atypeFactory.getAnnotatedType(iterableElement);
        AnnotatedDeclaredType dt = asSuper(atypeFactory, iterableType, iterableElmType);
        if (dt.getTypeArguments().isEmpty()) {
            TypeElement e = processingEnv.getElementUtils().getTypeElement("java.lang.Object");
            AnnotatedDeclaredType t = atypeFactory.getAnnotatedType(e);
            return t;
        } else {
            return dt.getTypeArguments().get(0);
        }
    }

    /**
     * Returns all the supertypes (direct or indirect) of the given declared type.
     *
     * @param type a declared type
     * @return all the supertypes of the given type
     */
    public static Set<AnnotatedDeclaredType> getSuperTypes(AnnotatedDeclaredType type) {

        Set<AnnotatedDeclaredType> supertypes = new LinkedHashSet<>();
        if (type == null) {
            return supertypes;
        }

        // Set up a stack containing the type mirror of subtype, which
        // is our starting point.
        Deque<AnnotatedDeclaredType> stack = new ArrayDeque<>();
        stack.push(type);

        while (!stack.isEmpty()) {
            AnnotatedDeclaredType current = stack.pop();

            // For each direct supertype of the current type, if it
            // hasn't already been visited, push it onto the stack and
            // add it to our supertypes set.
            for (AnnotatedDeclaredType supertype : current.directSuperTypes()) {
                if (!supertypes.contains(supertype)) {
                    stack.push(supertype);
                    supertypes.add(supertype);
                }
            }
        }

        return Collections.unmodifiableSet(supertypes);
    }

    /**
     * Given a method, return the methods that it overrides.
     *
     * @param method the overriding method
     * @return a map from types to methods that {@code method} overrides
     */
    public static Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods(
            Elements elements, AnnotatedTypeFactory atypeFactory, ExecutableElement method) {
        final TypeElement elem = (TypeElement) method.getEnclosingElement();
        final AnnotatedDeclaredType type = atypeFactory.getAnnotatedType(elem);
        final Collection<AnnotatedDeclaredType> supertypes = getSuperTypes(type);
        return overriddenMethods(elements, method, supertypes);
    }

    /**
     * Given a method and all supertypes (recursively) of the method's containing class, returns the
     * methods that the method overrides.
     *
     * @param method the overriding method
     * @param supertypes the set of supertypes to check for methods that are overridden by {@code
     *     method}
     * @return a map from types to methods that {@code method} overrides
     */
    public static Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods(
            Elements elements,
            ExecutableElement method,
            Collection<AnnotatedDeclaredType> supertypes) {

        Map<AnnotatedDeclaredType, ExecutableElement> overrides = new LinkedHashMap<>();

        for (AnnotatedDeclaredType supertype : supertypes) {
            @Nullable TypeElement superElement = (TypeElement) supertype.getUnderlyingType().asElement();
            assert superElement != null; /*nninvariant*/
            // For all method in the supertype, add it to the set if
            // it overrides the given method.
            for (ExecutableElement supermethod :
                    ElementFilter.methodsIn(superElement.getEnclosedElements())) {
                if (elements.overrides(method, supermethod, superElement)) {
                    overrides.put(supertype, supermethod);
                    break;
                }
            }
        }

        return Collections.unmodifiableMap(overrides);
    }

    /**
     * Given a method or constructor invocation, return a mapping of the type variables to their
     * type arguments, if any exist.
     *
     * <p>It uses the method or constructor invocation type arguments if they were specified and
     * otherwise it infers them based on the passed arguments or the return type context, according
     * to JLS 15.12.2.
     *
     * @param atypeFactory the annotated type factory
     * @param expr the method or constructor invocation tree; the passed argument has to be a
     *     subtype of MethodInvocationTree or NewClassTree
     * @param elt the element corresponding to the tree
     * @param preType the (partially annotated) type corresponding to the tree - the result of
     *     AnnotatedTypes.asMemberOf with the receiver and elt
     * @return the mapping of the type variables to type arguments for this method or constructor
     *     invocation
     */
    public static Map<TypeVariable, AnnotatedTypeMirror> findTypeArguments(
            final ProcessingEnvironment processingEnv,
            final AnnotatedTypeFactory atypeFactory,
            final ExpressionTree expr,
            final ExecutableElement elt,
            final AnnotatedExecutableType preType) {

        // Is the method a generic method?
        if (elt.getTypeParameters().isEmpty()) {
            return Collections.emptyMap();
        }

        List<? extends Tree> targs;
        if (expr instanceof MethodInvocationTree) {
            targs = ((MethodInvocationTree) expr).getTypeArguments();
        } else if (expr instanceof NewClassTree) {
            targs = ((NewClassTree) expr).getTypeArguments();
        } else if (expr instanceof MemberReferenceTree) {
            targs = ((MemberReferenceTree) expr).getTypeArguments();
            if (targs == null) {
                // TODO: Add type argument inference as part of fix for #979
                return new HashMap<>();
            }
        } else {
            // This case should never happen.
            throw new BugInCF("AnnotatedTypes.findTypeArguments: unexpected tree: " + expr);
        }

        // Has the user supplied type arguments?
        if (!targs.isEmpty()) {
            List<? extends AnnotatedTypeVariable> tvars = preType.getTypeVariables();

            Map<TypeVariable, AnnotatedTypeMirror> typeArguments = new HashMap<>();
            for (int i = 0; i < elt.getTypeParameters().size(); ++i) {
                AnnotatedTypeVariable typeVar = tvars.get(i);
                AnnotatedTypeMirror typeArg =
                        atypeFactory.getAnnotatedTypeFromTypeTree(targs.get(i));
                // TODO: the call to getTypeParameterDeclaration shouldn't be necessary - typeVar
                // already should be a declaration.
                typeArguments.put(typeVar.getUnderlyingType(), typeArg);
            }
            return typeArguments;
        } else {
            return atypeFactory
                    .getTypeArgumentInference()
                    .inferTypeArgs(atypeFactory, expr, elt, preType);
        }
    }

    /**
     * Returns the lub of two annotated types.
     *
     * @param atypeFactory AnnotatedTypeFactory
     * @param type1 annotated type
     * @param type2 annotated type
     * @return the lub of type1 and type2
     */
    public static AnnotatedTypeMirror leastUpperBound(
            AnnotatedTypeFactory atypeFactory,
            AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2) {
        TypeMirror lub =
                TypesUtils.leastUpperBound(
                        type1.getUnderlyingType(),
                        type2.getUnderlyingType(),
                        atypeFactory.getProcessingEnv());
        return leastUpperBound(atypeFactory, type1, type2, lub);
    }

    /**
     * Returns the lub, whose underlying type is {@code lubTypeMirror} of two annotated types.
     *
     * @param atypeFactory AnnotatedTypeFactory
     * @param type1 annotated type whose underlying type must be a subtype or convertible to
     *     lubTypeMirror
     * @param type2 annotated type whose underlying type must be a subtype or convertible to
     *     lubTypeMirror
     * @param lubTypeMirror underlying type of the returned lub
     * @return the lub of type1 and type2 with underlying type lubTypeMirror
     */
    public static AnnotatedTypeMirror leastUpperBound(
            AnnotatedTypeFactory atypeFactory,
            AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2,
            TypeMirror lubTypeMirror) {
        return new AtmLubVisitor(atypeFactory).lub(type1, type2, lubTypeMirror);
    }

    /**
     * Returns the method parameters for the invoked method, with the same number of arguments
     * passed in the methodInvocation tree.
     *
     * <p>If the invoked method is not a vararg method or it is a vararg method but the invocation
     * passes an array to the vararg parameter, it would simply return the method parameters.
     *
     * <p>Otherwise, it would return the list of parameters as if the vararg is expanded to match
     * the size of the passed arguments.
     *
     * @param method the method's type
     * @param args the arguments to the method invocation
     * @return the types that the method invocation arguments need to be subtype of
     */
    public static List<AnnotatedTypeMirror> expandVarArgs(
            AnnotatedTypeFactory atypeFactory,
            AnnotatedExecutableType method,
            List<? extends ExpressionTree> args) {
        List<AnnotatedTypeMirror> parameters = method.getParameterTypes();
        if (!method.getElement().isVarArgs()) {
            return parameters;
        }

        AnnotatedArrayType varargs = (AnnotatedArrayType) parameters.get(parameters.size() - 1);

        if (parameters.size() == args.size()) {
            // Check if one sent an element or an array
            AnnotatedTypeMirror lastArg = atypeFactory.getAnnotatedType(args.get(args.size() - 1));
            if (lastArg.getKind() == TypeKind.ARRAY
                    && getArrayDepth(varargs) == getArrayDepth((AnnotatedArrayType) lastArg)) {
                return parameters;
            }
        }

        parameters = new ArrayList<>(parameters.subList(0, parameters.size() - 1));
        for (int i = args.size() - parameters.size(); i > 0; --i) {
            parameters.add(varargs.getComponentType().deepCopy());
        }

        return parameters;
    }

    public static List<AnnotatedTypeMirror> expandVarArgsFromTypes(
            AnnotatedExecutableType method, List<AnnotatedTypeMirror> args) {
        List<AnnotatedTypeMirror> parameters = method.getParameterTypes();
        if (!method.getElement().isVarArgs()) {
            return parameters;
        }

        AnnotatedArrayType varargs = (AnnotatedArrayType) parameters.get(parameters.size() - 1);

        if (parameters.size() == args.size()) {
            // Check if one sent an element or an array
            AnnotatedTypeMirror lastArg = args.get(args.size() - 1);
            if (lastArg.getKind() == TypeKind.ARRAY
                    && (getArrayDepth(varargs) == getArrayDepth((AnnotatedArrayType) lastArg)
                            // If the array depths don't match, but the component type of the vararg
                            // is a type variable, then that type variable might later be
                            // substituted for an array.
                            || varargs.getComponentType().getKind() == TypeKind.TYPEVAR)) {
                return parameters;
            }
        }

        parameters = new ArrayList<>(parameters.subList(0, parameters.size() - 1));
        for (int i = args.size() - parameters.size(); i > 0; --i) {
            parameters.add(varargs.getComponentType());
        }

        return parameters;
    }

    /**
     * Given an AnnotatedExecutableType of a method or constructor declaration, get the parameter
     * type expect at the indexth position (unwrapping var args if necessary).
     *
     * @param methodType AnnotatedExecutableType of method or constructor containing parameter to
     *     return
     * @param index position of parameter type to return
     * @return if that parameter is a varArgs, return the component of the var args and NOT the
     *     array type. Otherwise, return the exact type of the parameter in the index position.
     */
    public static AnnotatedTypeMirror getAnnotatedTypeMirrorOfParameter(
            AnnotatedExecutableType methodType, int index) {
        List<AnnotatedTypeMirror> parameterTypes = methodType.getParameterTypes();
        boolean hasVarArg = methodType.getElement().isVarArgs();

        final int lastIndex = parameterTypes.size() - 1;
        final AnnotatedTypeMirror lastType = parameterTypes.get(lastIndex);
        final boolean parameterBeforeVarargs = index < lastIndex;
        if (!parameterBeforeVarargs && lastType instanceof AnnotatedArrayType) {
            final AnnotatedArrayType arrayType = (AnnotatedArrayType) lastType;
            if (hasVarArg) {
                return arrayType.getComponentType();
            }
        }
        return parameterTypes.get(index);
    }

    /**
     * Return a list of the AnnotatedTypeMirror of the passed expression trees, in the same order as
     * the trees.
     *
     * @param paramTypes the parameter types to use as assignment context
     * @param trees the AST nodes
     * @return a list with the AnnotatedTypeMirror of each tree in trees
     */
    public static List<AnnotatedTypeMirror> getAnnotatedTypes(
            AnnotatedTypeFactory atypeFactory,
            List<AnnotatedTypeMirror> paramTypes,
            List<? extends ExpressionTree> trees) {
        assert paramTypes.size() == trees.size()
                : "AnnotatedTypes.getAnnotatedTypes: size mismatch! "
                        + "Parameter types: "
                        + paramTypes
                        + " Arguments: "
                        + trees;
        List<AnnotatedTypeMirror> types = new ArrayList<>();
        Pair<Tree, AnnotatedTypeMirror> preAssCtxt =
                atypeFactory.getVisitorState().getAssignmentContext();

        try {
            for (int i = 0; i < trees.size(); ++i) {
                AnnotatedTypeMirror param = paramTypes.get(i);
                atypeFactory.getVisitorState().setAssignmentContext(Pair.of((Tree) null, param));
                ExpressionTree arg = trees.get(i);
                types.add(atypeFactory.getAnnotatedType(arg));
            }
        } finally {
            atypeFactory.getVisitorState().setAssignmentContext(preAssCtxt);
        }
        return types;
    }

    /**
     * Returns the depth of the array type of the provided array.
     *
     * @param array the type of the array
     * @return the depth of the provided array
     */
    public static int getArrayDepth(AnnotatedArrayType array) {
        int counter = 0;
        AnnotatedTypeMirror type = array;
        while (type.getKind() == TypeKind.ARRAY) {
            counter++;
            type = ((AnnotatedArrayType) type).getComponentType();
        }
        return counter;
    }

    // The innermost *array* type.
    public static AnnotatedTypeMirror innerMostType(AnnotatedTypeMirror t) {
        AnnotatedTypeMirror inner = t;
        while (inner.getKind() == TypeKind.ARRAY) {
            inner = ((AnnotatedArrayType) inner).getComponentType();
        }
        return inner;
    }

    /**
     * Checks whether type contains the given modifier, also recursively in type arguments and
     * arrays. This method might be easier to implement directly as instance method in
     * AnnotatedTypeMirror; it corresponds to a "deep" version of {@link
     * AnnotatedTypeMirror#hasAnnotation(AnnotationMirror)}.
     *
     * @param type the type to search
     * @param modifier the modifier to search for
     * @return whether the type contains the modifier
     */
    public static boolean containsModifier(AnnotatedTypeMirror type, AnnotationMirror modifier) {
        return containsModifierImpl(type, modifier, new ArrayList<>());
    }

    /*
     * For type variables we might hit the same type again. We keep a list of visited types.
     */
    private static boolean containsModifierImpl(
            AnnotatedTypeMirror type,
            AnnotationMirror modifier,
            List<AnnotatedTypeMirror> visited) {
        boolean found = type.hasAnnotation(modifier);
        boolean vis = visited.contains(type);
        visited.add(type);

        if (!found && !vis) {
            if (type.getKind() == TypeKind.DECLARED) {
                AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
                for (AnnotatedTypeMirror typeMirror : declaredType.getTypeArguments()) {
                    found |= containsModifierImpl(typeMirror, modifier, visited);
                    if (found) {
                        break;
                    }
                }
            } else if (type.getKind() == TypeKind.ARRAY) {
                AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
                found = containsModifierImpl(arrayType.getComponentType(), modifier, visited);
            } else if (type.getKind() == TypeKind.TYPEVAR) {
                AnnotatedTypeVariable atv = (AnnotatedTypeVariable) type;
                if (atv.getUpperBound() != null) {
                    found = containsModifierImpl(atv.getUpperBound(), modifier, visited);
                }
                if (!found && atv.getLowerBound() != null) {
                    found = containsModifierImpl(atv.getLowerBound(), modifier, visited);
                }
            } else if (type.getKind() == TypeKind.WILDCARD) {
                AnnotatedWildcardType awc = (AnnotatedWildcardType) type;
                if (awc.getExtendsBound() != null) {
                    found = containsModifierImpl(awc.getExtendsBound(), modifier, visited);
                }
                if (!found && awc.getSuperBound() != null) {
                    found = containsModifierImpl(awc.getSuperBound(), modifier, visited);
                }
            }
        }

        return found;
    }

    /** java.lang.annotation.Annotation.class canonical name. */
    private static String annotationClassName =
            java.lang.annotation.Annotation.class.getCanonicalName();

    /**
     * Returns true if the underlying type of this atm is a java.lang.annotation.Annotation.
     *
     * @return true if the underlying type of this atm is a java.lang.annotation.Annotation
     */
    public static boolean isJavaLangAnnotation(final AnnotatedTypeMirror atm) {
        return TypesUtils.isDeclaredOfName(atm.getUnderlyingType(), annotationClassName);
    }

    /**
     * Returns true if atm is an Annotation interface, i.e. an implementation of
     * java.lang.annotation.Annotation. Given {@code @interface MyAnno}, a call to {@code
     * implementsAnnotation} returns true when called on an AnnotatedDeclaredType representing a use
     * of MyAnno.
     *
     * @return true if atm is an Annotation interface
     */
    public static boolean implementsAnnotation(final AnnotatedTypeMirror atm) {
        if (atm.getKind() != TypeKind.DECLARED) {
            return false;
        }
        final AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
                (AnnotatedTypeMirror.AnnotatedDeclaredType) atm;

        Symbol.ClassSymbol classSymbol =
                (Symbol.ClassSymbol) declaredType.getUnderlyingType().asElement();
        for (final Type iface : classSymbol.getInterfaces()) {
            if (TypesUtils.isDeclaredOfName(iface, annotationClassName)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isEnum(final AnnotatedTypeMirror typeMirror) {
        if (typeMirror.getKind() == TypeKind.DECLARED) {
            final AnnotatedDeclaredType adt = (AnnotatedDeclaredType) typeMirror;
            return TypesUtils.isDeclaredOfName(
                    adt.getUnderlyingType(), java.lang.Enum.class.getName());
        }

        return false;
    }

    public static boolean isDeclarationOfJavaLangEnum(
            final Types types, final Elements elements, final AnnotatedTypeMirror typeMirror) {
        if (isEnum(typeMirror)) {
            return elements.getTypeElement("java.lang.Enum")
                    .equals(((AnnotatedDeclaredType) typeMirror).getUnderlyingType().asElement());
        }

        return false;
    }

    /**
     * Returns true if the typeVar1 and typeVar2 are two uses of the same type variable.
     *
     * @return true if the typeVar1 and typeVar2 are two uses of the same type variable
     */
    public static boolean haveSameDeclaration(
            Types types,
            final AnnotatedTypeVariable typeVar1,
            final AnnotatedTypeVariable typeVar2) {
        return types.isSameType(typeVar1.getUnderlyingType(), typeVar2.getUnderlyingType());
    }

    /**
     * When overriding a method, you must include the same number of type parameters as the base
     * method. By index, these parameters are considered equivalent to the type parameters of the
     * overridden method. Necessary conditions: Both type variables are defined in methods One of
     * the two methods overrides the other Within their method declaration, both types have the same
     * type parameter index
     *
     * @return returns true if type1 and type2 are corresponding type variables (that is, either one
     *     "overrides" the other)
     */
    public static boolean areCorrespondingTypeVariables(
            Elements elements, AnnotatedTypeVariable type1, AnnotatedTypeVariable type2) {
        final TypeParameterElement type1ParamElem =
                (TypeParameterElement) type1.getUnderlyingType().asElement();
        final TypeParameterElement type2ParamElem =
                (TypeParameterElement) type2.getUnderlyingType().asElement();

        if (type1ParamElem.getGenericElement() instanceof ExecutableElement
                && type2ParamElem.getGenericElement() instanceof ExecutableElement) {
            final ExecutableElement type1Executable =
                    (ExecutableElement) type1ParamElem.getGenericElement();
            final ExecutableElement type2Executable =
                    (ExecutableElement) type2ParamElem.getGenericElement();

            final TypeElement type1Class = (TypeElement) type1Executable.getEnclosingElement();
            final TypeElement type2Class = (TypeElement) type2Executable.getEnclosingElement();

            boolean methodIsOverriden =
                    elements.overrides(type1Executable, type2Executable, type1Class)
                            || elements.overrides(type2Executable, type1Executable, type2Class);
            if (methodIsOverriden) {
                boolean haveSameIndex =
                        type1Executable.getTypeParameters().indexOf(type1ParamElem)
                                == type2Executable.getTypeParameters().indexOf(type2ParamElem);
                return haveSameIndex;
            }
        }

        return false;
    }

    /**
     * When comparing types against the bounds of a type variable, we may encounter other type
     * variables, wildcards, and intersections in those bounds. This method traverses the bounds
     * until it finds a concrete type from which it can pull an annotation.
     *
     * @param top the top of the hierarchy for which you are searching
     * @return the AnnotationMirror that represents the type of toSearch in the hierarchy of top
     */
    public static AnnotationMirror findEffectiveAnnotationInHierarchy(
            final QualifierHierarchy qualifierHierarchy,
            final AnnotatedTypeMirror toSearch,
            final AnnotationMirror top) {
        return findEffectiveAnnotationInHierarchy(qualifierHierarchy, toSearch, top, false);
    }

    /**
     * When comparing types against the bounds of a type variable, we may encounter other type
     * variables, wildcards, and intersections in those bounds. This method traverses the bounds
     * until it finds a concrete type from which it can pull an annotation.
     *
     * @param top the top of the hierarchy for which you are searching
     * @param canBeEmpty whether or not the effective type can have NO annotation in the hierarchy
     *     specified by top If this param is false, an exception will be thrown if no annotation is
     *     found Otherwise the result is null
     * @return the AnnotationMirror that represents the type of toSearch in the hierarchy of top
     */
    public static AnnotationMirror findEffectiveAnnotationInHierarchy(
            final QualifierHierarchy qualifierHierarchy,
            final AnnotatedTypeMirror toSearch,
            final AnnotationMirror top,
            final boolean canBeEmpty) {
        AnnotatedTypeMirror source = toSearch;
        while (source.getAnnotationInHierarchy(top) == null) {

            switch (source.getKind()) {
                case TYPEVAR:
                    source = ((AnnotatedTypeVariable) source).getUpperBound();
                    break;

                case WILDCARD:
                    source = ((AnnotatedWildcardType) source).getExtendsBound();
                    break;

                case INTERSECTION:
                    // if there are multiple conflicting annotations, choose the lowest
                    final AnnotationMirror glb =
                            glbOfBoundsInHierarchy(
                                    (AnnotatedIntersectionType) source, top, qualifierHierarchy);

                    if (glb == null) {
                        throw new BugInCF(
                                "AnnotatedIntersectionType has no annotation in hierarchy "
                                        + "on any of its supertypes."
                                        + System.lineSeparator()
                                        + "intersectionType="
                                        + source);
                    }
                    return glb;

                default:
                    if (canBeEmpty) {
                        return null;
                    }

                    throw new BugInCF(
                            UtilPlume.joinLines(
                                    "Unexpected AnnotatedTypeMirror with no primary annotation.",
                                    "toSearch=" + toSearch,
                                    "top=" + top,
                                    "source=" + source));
            }
        }

        return source.getAnnotationInHierarchy(top);
    }

    /**
     * When comparing types against the bounds of a type variable, we may encounter other type
     * variables, wildcards, and intersections in those bounds. This method traverses the lower
     * bounds until it finds a concrete type from which it can pull an annotation. This occurs for
     * every hierarchy in QualifierHierarchy
     *
     * @return the set of effective annotation mirrors in all hierarchies
     */
    public static Set<AnnotationMirror> findEffectiveLowerBoundAnnotations(
            final QualifierHierarchy qualifierHierarchy, final AnnotatedTypeMirror toSearch) {
        AnnotatedTypeMirror source = toSearch;
        TypeKind kind = source.getKind();
        while (kind == TypeKind.TYPEVAR
                || kind == TypeKind.WILDCARD
                || kind == TypeKind.INTERSECTION) {

            switch (source.getKind()) {
                case TYPEVAR:
                    source = ((AnnotatedTypeVariable) source).getLowerBound();
                    break;

                case WILDCARD:
                    source = ((AnnotatedWildcardType) source).getSuperBound();
                    break;

                case INTERSECTION:
                    // if there are multiple conflicting annotations, choose the lowest
                    final Set<AnnotationMirror> glb =
                            glbOfBounds((AnnotatedIntersectionType) source, qualifierHierarchy);
                    return glb;

                default:
                    throw new BugInCF(
                            "Unexpected AnnotatedTypeMirror with no primary annotation;"
                                    + " toSearch="
                                    + toSearch
                                    + " source="
                                    + source);
            }

            kind = source.getKind();
        }

        return source.getAnnotations();
    }

    /**
     * When comparing types against the bounds of a type variable, we may encounter other type
     * variables, wildcards, and intersections in those bounds. This method traverses the bounds
     * until it finds a concrete type from which it can pull an annotation. This occurs for every
     * hierarchy in QualifierHierarchy
     *
     * @return the set of effective annotation mirrors in all hierarchies
     */
    public static Set<AnnotationMirror> findEffectiveAnnotations(
            final QualifierHierarchy qualifierHierarchy, final AnnotatedTypeMirror toSearch) {
        AnnotatedTypeMirror source = toSearch;
        TypeKind kind = source.getKind();
        while (kind == TypeKind.TYPEVAR
                || kind == TypeKind.WILDCARD
                || kind == TypeKind.INTERSECTION) {

            switch (source.getKind()) {
                case TYPEVAR:
                    source = ((AnnotatedTypeVariable) source).getUpperBound();
                    break;

                case WILDCARD:
                    source = ((AnnotatedWildcardType) source).getExtendsBound();
                    break;

                case INTERSECTION:
                    // if there are multiple conflicting annotations, choose the lowest
                    final Set<AnnotationMirror> glb =
                            glbOfBounds((AnnotatedIntersectionType) source, qualifierHierarchy);
                    return glb;

                default:
                    throw new BugInCF(
                            "Unexpected AnnotatedTypeMirror with no primary annotation;"
                                    + " toSearch="
                                    + toSearch
                                    + " source="
                                    + source);
            }

            kind = source.getKind();
        }

        return source.getAnnotations();
    }

    private static AnnotationMirror glbOfBoundsInHierarchy(
            final AnnotatedIntersectionType isect,
            final AnnotationMirror top,
            final QualifierHierarchy qualifierHierarchy) {
        AnnotationMirror anno = isect.getAnnotationInHierarchy(top);
        for (final AnnotatedTypeMirror supertype : isect.directSuperTypes()) {
            final AnnotationMirror superAnno = supertype.getAnnotationInHierarchy(top);
            if (superAnno != null
                    && (anno == null || qualifierHierarchy.isSubtype(superAnno, anno))) {
                anno = superAnno;
            }
        }

        return anno;
    }

    /**
     * Gets the lowest primary annotation of all bounds in the intersection.
     *
     * @param isect the intersection for which we are glbing bounds
     * @param qualifierHierarchy the qualifier used to get the hierarchies in which to glb
     * @return a set of annotations representing the glb of the intersection's bounds
     */
    public static Set<AnnotationMirror> glbOfBounds(
            final AnnotatedIntersectionType isect, final QualifierHierarchy qualifierHierarchy) {
        Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        for (final AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
            final AnnotationMirror glbAnno = glbOfBoundsInHierarchy(isect, top, qualifierHierarchy);
            if (glbAnno != null) {
                result.add(glbAnno);
            }
        }

        return result;
    }

    // For Wildcards, isSuperBound and isExtendsBound will return true if isUnbound does.

    public static boolean isExplicitlySuperBounded(final AnnotatedWildcardType wildcardType) {
        return ((Type.WildcardType) wildcardType.getUnderlyingType()).isSuperBound()
                && !((Type.WildcardType) wildcardType.getUnderlyingType()).isUnbound();
    }

    /** Returns true if wildcard type was explicitly unbounded. */
    public static boolean isExplicitlyExtendsBounded(final AnnotatedWildcardType wildcardType) {
        return ((Type.WildcardType) wildcardType.getUnderlyingType()).isExtendsBound()
                && !((Type.WildcardType) wildcardType.getUnderlyingType()).isUnbound();
    }

    /** Returns true if this type is super bounded or unbounded. */
    public static boolean isUnboundedOrSuperBounded(final AnnotatedWildcardType wildcardType) {
        return ((Type.WildcardType) wildcardType.getUnderlyingType()).isSuperBound();
    }

    /** Returns true if this type is extends bounded or unbounded. */
    public static boolean isUnboundedOrExtendsBounded(final AnnotatedWildcardType wildcardType) {
        return ((Type.WildcardType) wildcardType.getUnderlyingType()).isExtendsBound();
    }

    /**
     * Copies explicit annotations and annotations resulting from resolution of polymorphic
     * qualifiers from {@code constructor} to {@code returnType}. If {@code returnType} has an
     * annotation in the same hierarchy of an annotation to be copied, that annotation is not
     * copied.
     *
     * @param atypeFactory type factory
     * @param returnType return type to copy annotations to
     * @param constructor the ATM for the constructor
     */
    public static void copyOnlyExplicitConstructorAnnotations(
            AnnotatedTypeFactory atypeFactory,
            AnnotatedDeclaredType returnType,
            AnnotatedExecutableType constructor) {

        // TODO: There will be a nicer way to access this in 308 soon.
        List<Attribute.TypeCompound> decall =
                ((Symbol) constructor.getElement()).getRawTypeAttributes();
        Set<AnnotationMirror> decret = AnnotationUtils.createAnnotationSet();
        for (Attribute.TypeCompound da : decall) {
            if (da.position.type == com.sun.tools.javac.code.TargetType.METHOD_RETURN) {
                decret.add(da);
            }
        }

        // Collect all polymorphic qualifiers; we should substitute them.
        Set<AnnotationMirror> polys = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror anno : returnType.getAnnotations()) {
            if (atypeFactory.getQualifierHierarchy().isPolymorphicQualifier(anno)) {
                polys.add(anno);
            }
        }

        for (AnnotationMirror cta : constructor.getReturnType().getAnnotations()) {
            AnnotationMirror ctatop = atypeFactory.getQualifierHierarchy().getTopAnnotation(cta);
            if (returnType.isAnnotatedInHierarchy(cta)) {
                continue;
            }
            if (atypeFactory.isSupportedQualifier(cta) && !returnType.isAnnotatedInHierarchy(cta)) {
                for (AnnotationMirror fromDecl : decret) {
                    if (atypeFactory.isSupportedQualifier(fromDecl)
                            && AnnotationUtils.areSame(
                                    ctatop,
                                    atypeFactory
                                            .getQualifierHierarchy()
                                            .getTopAnnotation(fromDecl))) {
                        returnType.addAnnotation(cta);
                        break;
                    }
                }
            }

            // Go through the polymorphic qualifiers and see whether
            // there is anything left to replace.
            for (AnnotationMirror pa : polys) {
                if (AnnotationUtils.areSame(
                        ctatop, atypeFactory.getQualifierHierarchy().getTopAnnotation(pa))) {
                    returnType.replaceAnnotation(cta);
                    break;
                }
            }
        }
    }

    /**
     * Add all the annotations in {@code declaredType} to {@code annotatedDeclaredType}.
     *
     * <p>(The {@code TypeMirror} returned by {@code annotatedDeclaredType#getUnderlyingType} may
     * have not have all the annotations on the type, so allow the user to specify a different one.)
     *
     * @param annotatedDeclaredType annotated type to which annotations are added
     * @param declaredType TypeMirror that may have annotations
     */
    public static void applyAnnotationsFromDeclaredType(
            AnnotatedDeclaredType annotatedDeclaredType, DeclaredType declaredType) {
        TypeMirror underlyingTypeMirror = declaredType;
        while (annotatedDeclaredType != null) {
            List<? extends AnnotationMirror> annosOnTypeMirror =
                    underlyingTypeMirror.getAnnotationMirrors();
            annotatedDeclaredType.addAnnotations(annosOnTypeMirror);
            annotatedDeclaredType = annotatedDeclaredType.getEnclosingType();
            underlyingTypeMirror = ((DeclaredType) underlyingTypeMirror).getEnclosingType();
        }
    }
}
