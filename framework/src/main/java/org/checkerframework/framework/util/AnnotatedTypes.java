package org.checkerframework.framework.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.CanonicalName;
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
import org.checkerframework.framework.util.typeinference8.InferenceResult;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;
import org.plumelib.util.IPair;
import org.plumelib.util.StringsPlume;

/**
 * Utility methods for operating on {@code AnnotatedTypeMirror}. This class mimics the class {@link
 * Types}.
 */
public class AnnotatedTypes {
  /** Class cannot be instantiated. */
  private AnnotatedTypes() {
    throw new AssertionError("Class AnnotatedTypes cannot be instantiated.");
  }

  /** Implements {@code asSuper}. */
  private static @MonotonicNonNull AsSuperVisitor asSuperVisitor;

  /**
   * Copies annotations from {@code type} to a copy of {@code superType} where the type variables of
   * {@code superType} have been substituted. How the annotations are copied depends on the kinds of
   * AnnotatedTypeMirrors given. Generally, if {@code type} and {@code superType} are both declared
   * types, asSuper is called recursively on the direct super types, see {@link
   * AnnotatedTypeMirror#directSupertypes()}, of {@code type} until {@code type}'s erased Java type
   * is the same as {@code superType}'s erased super type. Then {@code type is returned}. For
   * compound types, asSuper is called recursively on components.
   *
   * <p>Preconditions:<br>
   * {@code superType} may have annotations, but they are ignored. <br>
   * {@code type} may not be an instanceof AnnotatedNullType, because if {@code superType} is a
   * compound type, the annotations on the component types are undefined.<br>
   * The underlying {@code type} (ie the Java type) of {@code type} should be a subtype (or the same
   * type) of the underlying type of {@code superType}. Except for these cases:
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
   *     type
   * @return {@code superType} with annotations copied from {@code type} and type variables
   *     substituted from {@code type}
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
   * @param <T> the type of supertype and return type
   * @param atypeFactory the type factory
   * @param subtype subtype to be transformed to supertype
   * @param supertype supertype that subtype is transformed to
   * @return subtype as an instance of supertype
   */
  public static <T extends AnnotatedTypeMirror> T castedAsSuper(
      AnnotatedTypeFactory atypeFactory, AnnotatedTypeMirror subtype, T supertype) {
    Types types = atypeFactory.getProcessingEnv().getTypeUtils();

    if (subtype.getKind() == TypeKind.NULL) {
      // Make a copy of the supertype so that if supertype is a composite type, the
      // returned type will be fully annotated.  (For example, if sub is @C null and super is
      // @A List<@B String>, then the returned type is @C List<@B String>.)
      @SuppressWarnings("unchecked")
      T copy = (T) supertype.deepCopy();
      copy.replaceAnnotations(subtype.getPrimaryAnnotations());
      return copy;
    }

    Elements elements = atypeFactory.getProcessingEnv().getElementUtils();
    if (supertype != null
        && AnnotatedTypes.isEnum(supertype)
        && AnnotatedTypes.isDeclarationOfJavaLangEnum(types, elements, supertype)) {
      // Don't return the asSuper result because it causes an infinite loop.
      @SuppressWarnings("unchecked")
      T result = (T) supertype.deepCopy();
      return result;
    }

    T asSuperType = AnnotatedTypes.asSuper(atypeFactory, subtype, supertype);

    fixUpRawTypes(subtype, asSuperType, supertype, types);
    return asSuperType;
  }

  /**
   * Some times we create type arguments for types that were raw. When we do an asSuper we lose
   * these arguments. If in the converted type (i.e. the subtype as super) is missing type arguments
   * AND those type arguments should come from the original subtype's type arguments then we copy
   * the original type arguments to the converted type. e.g. We have a type W, that "wasRaw" {@code
   * ArrayList<? extends Object>} When W is converted to type A, List, using asSuper it no longer
   * has its type argument. But since the type argument to List should be the same as that to
   * ArrayList we copy over the type argument of W to A. A becomes {@code List<? extends Object>}
   *
   * @param originalSubtype the subtype before being converted by asSuper
   * @param asSuperType he subtype after being converted by asSuper
   * @param supertype the supertype for which asSuperType should have the same underlying type
   * @param types the types utility
   */
  private static void fixUpRawTypes(
      AnnotatedTypeMirror originalSubtype,
      AnnotatedTypeMirror asSuperType,
      AnnotatedTypeMirror supertype,
      Types types) {
    if (asSuperType == null
        || asSuperType.getKind() != TypeKind.DECLARED
        || originalSubtype.getKind() != TypeKind.DECLARED) {
      return;
    }

    AnnotatedDeclaredType declaredAsSuper = (AnnotatedDeclaredType) asSuperType;
    AnnotatedDeclaredType declaredSubtype = (AnnotatedDeclaredType) originalSubtype;

    if (!declaredAsSuper.isUnderlyingTypeRaw()
        || !declaredAsSuper.getTypeArguments().isEmpty()
        || declaredSubtype.getTypeArguments().isEmpty()) {
      return;
    }

    Set<IPair<Integer, Integer>> typeArgMap =
        TypeArgumentMapper.mapTypeArgumentIndices(
            (TypeElement) declaredSubtype.getUnderlyingType().asElement(),
            (TypeElement) declaredAsSuper.getUnderlyingType().asElement(),
            types);

    if (typeArgMap.size() != declaredSubtype.getTypeArguments().size()) {
      return;
    }

    List<IPair<Integer, Integer>> orderedByDestination = new ArrayList<>(typeArgMap);
    orderedByDestination.sort(Comparator.comparingInt(o -> o.second));

    if (typeArgMap.size() == ((AnnotatedDeclaredType) supertype).getTypeArguments().size()) {
      List<? extends AnnotatedTypeMirror> subTypeArgs = declaredSubtype.getTypeArguments();
      List<AnnotatedTypeMirror> newTypeArgs =
          CollectionsPlume.mapList(
              mapping -> subTypeArgs.get(mapping.first).deepCopy(), orderedByDestination);
      declaredAsSuper.setTypeArguments(newTypeArgs);
    } else {
      declaredAsSuper.setTypeArguments(Collections.emptyList());
    }
  }

  /**
   * Returns the result of calling {@link #asSuper(AnnotatedTypeFactory, AnnotatedTypeMirror,
   * AnnotatedTypeMirror)} on {@code type} and {@code superType} or an enclosing type of {@code
   * type}.
   *
   * <p>If the underlying type of {@code type} is a subtype of the underlying type of {@code
   * superType}, then this method returns the result of calling {@code asSuper(atypeFactory, type,
   * superType)}.
   *
   * <p>If the underlying type of an enclosing of {@code type} is a subtype of the underlying type
   * of {@code superType}, then this method returns the result of calling {@code
   * asSuper(atypeFactory, type.getEnclosingType(), superType)}.
   *
   * <p>Otherwise, throws {@link BugInCF}.
   *
   * @param types types utils
   * @param atypeFactory the type factory
   * @param type a type
   * @param superType a supertype of {@code type} or a supertype of an enclosing type of {@code
   *     type}
   * @return {@code type} or an enclosing type of {@code type} as {@code superType}
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
        throw new BugInCF("Enclosing type not found: type: %s supertype: %s", dt, superType);
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
   * Specialization of {@link #asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror, Element,
   * AnnotatedTypeMirror)} with more precise return type.
   *
   * @see #asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror, Element,
   *     AnnotatedTypeMirror)
   * @param types the Types instance to use
   * @param atypeFactory the type factory to use
   * @param t the receiver type
   * @param elem the element that should be viewed as member of t
   * @param type unsubstituted type of member
   * @return the type of member as member of {@code t}, with initial type memberType; can be an
   *     alias to memberType
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
   * <p>For example, when viewed as a member of the parameterized type {@code Set<@NonNull String>},
   * the {@code Set.add} method is an {@code ExecutableType} whose parameter is of type
   * {@code @NonNull String}.
   *
   * <p>Before returning the result, this method adjusts it by calling {@link
   * AnnotatedTypeFactory#postAsMemberOf(AnnotatedTypeMirror, AnnotatedTypeMirror, Element)}.
   *
   * @param types the Types instance to use
   * @param atypeFactory the type factory to use
   * @param t the receiver type
   * @param elem the element that should be viewed as member of t
   * @return the type of elem as member of t
   */
  public static AnnotatedTypeMirror asMemberOf(
      Types types, AnnotatedTypeFactory atypeFactory, AnnotatedTypeMirror t, Element elem) {
    AnnotatedTypeMirror memberType = atypeFactory.getAnnotatedType(elem);
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
      @Nullable AnnotatedTypeMirror t,
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
        if (t == null || ElementUtils.isStatic(elem)) {
          return elemType;
        }
        AnnotatedTypeMirror res = asMemberOfImpl(types, atypeFactory, t, elem, elemType);
        atypeFactory.postAsMemberOf(res, t, elem);
        return res;
    }
  }

  /**
   * Helper for {@link AnnotatedTypes#asMemberOf(Types, AnnotatedTypeFactory, AnnotatedTypeMirror,
   * Element)}.
   *
   * @param types the Types instance to use
   * @param atypeFactory the type factory to use
   * @param receiverType the receiver type
   * @param member the element that should be viewed as member of receiverType
   * @param memberType unsubstituted type of member
   * @return the type of member as a member of receiverType; can be an alias to memberType
   */
  private static AnnotatedTypeMirror asMemberOfImpl(
      Types types,
      AnnotatedTypeFactory atypeFactory,
      AnnotatedTypeMirror receiverType,
      Element member,
      AnnotatedTypeMirror memberType) {
    switch (receiverType.getKind()) {
      case ARRAY:
        // Method references like String[]::clone should have a return type of String[]
        // rather than Object.
        if (SyntheticArrays.isArrayClone(receiverType, member)) {
          return SyntheticArrays.replaceReturnType(member, (AnnotatedArrayType) receiverType);
        }
        return memberType;
      case TYPEVAR:
        return asMemberOf(
            types,
            atypeFactory,
            atypeFactory.applyCaptureConversion(
                ((AnnotatedTypeVariable) receiverType).getUpperBound()),
            member,
            memberType);
      case WILDCARD:
        if (AnnotatedTypes.isTypeArgOfRawType(receiverType)) {
          return substituteTypeArgsFromRawTypes(atypeFactory, member, memberType);
        }
        return asMemberOf(
            types,
            atypeFactory,
            ((AnnotatedWildcardType) receiverType).getExtendsBound().deepCopy(),
            member,
            memberType);
      case INTERSECTION:
        AnnotatedTypeMirror result = memberType;
        TypeMirror enclosingElementType = member.getEnclosingElement().asType();
        for (AnnotatedTypeMirror bound : ((AnnotatedIntersectionType) receiverType).getBounds()) {
          if (TypesUtils.isErasedSubtype(bound.getUnderlyingType(), enclosingElementType, types)) {
            result =
                substituteTypeVariables(
                    types,
                    atypeFactory,
                    atypeFactory.applyCaptureConversion(bound),
                    member,
                    result);
          }
        }
        return result;
      case UNION:
        return substituteTypeVariables(types, atypeFactory, receiverType, member, memberType);
      case DECLARED:
        AnnotatedDeclaredType receiverTypeDT = (AnnotatedDeclaredType) receiverType;
        if (isRawCall(receiverTypeDT, member, types)) {
          return memberType.getErased();
        }
        return substituteTypeVariables(types, atypeFactory, receiverType, member, memberType);
      default:
        throw new BugInCF("asMemberOf called on unexpected type.%nt: %s", receiverType);
    }
  }

  /**
   * Is the call to {@code method} with {@code receiver} raw?
   *
   * @param receiver type of the receiver of the call
   * @param method the element of a method or constructor
   * @param types type utilities
   * @return true if the call to {@code method} with {@code receiver} raw
   */
  private static boolean isRawCall(AnnotatedDeclaredType receiver, Element method, Types types) {
    // Section 4.8, "Raw Types".
    // (https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.8)
    //
    // The type of a constructor (§8.8), instance method (8.4, 9.4), or non-static field
    // (8.3) of a raw type C that is not inherited from its superclasses or superinterfaces
    // is the raw type that corresponds to the erasure of its type in the generic declaration
    // corresponding to C.
    if (method.getEnclosingElement().equals(receiver.getUnderlyingType().asElement())) {
      return receiver.isUnderlyingTypeRaw();
    }

    // The below is checking for a super() call where the super type is a raw type.
    // See framework/tests/all-systems/RawSuper.java for an example.
    if ("<init>".contentEquals(method.getSimpleName())) {
      ExecutableElement constructor = (ExecutableElement) method;
      TypeMirror constructorClass = types.erasure(constructor.getEnclosingElement().asType());
      TypeMirror directSuper = types.directSupertypes(receiver.getUnderlyingType()).get(0);
      while (!types.isSameType(types.erasure(directSuper), constructorClass)
          && !TypesUtils.isObject(directSuper)) {
        directSuper = types.directSupertypes(directSuper).get(0);
      }
      if (directSuper.getKind() == TypeKind.DECLARED) {
        DeclaredType declaredType = (DeclaredType) directSuper;
        TypeElement typeelem = (TypeElement) declaredType.asElement();
        DeclaredType declty = (DeclaredType) typeelem.asType();
        return !declty.getTypeArguments().isEmpty() && declaredType.getTypeArguments().isEmpty();
      }
    }

    return false;
  }

  /**
   * Substitute type variables.
   *
   * @param types type utilities
   * @param atypeFactory the type factory
   * @param receiverType the type of the class that contains member (or a subtype of it)
   * @param member a type member, such as a method or field
   * @param memberType the type of {@code member}
   * @return {@code memberType}, substituted
   */
  private static AnnotatedTypeMirror substituteTypeVariables(
      Types types,
      AnnotatedTypeFactory atypeFactory,
      AnnotatedTypeMirror receiverType,
      Element member,
      AnnotatedTypeMirror memberType) {

    // Basic Algorithm:
    // 1. Find the enclosingClassOfMember of the element
    // 2. Find the base type of enclosingClassOfMember (e.g. type of enclosingClassOfMember as
    //      supertype of passed type)
    // 3. Substitute for type variables if any exist
    TypeElement enclosingClassOfMember = ElementUtils.enclosingTypeElement(member);
    DeclaredType enclosingType = (DeclaredType) enclosingClassOfMember.asType();
    Map<TypeVariable, AnnotatedTypeMirror> mappings = new HashMap<>();

    // Look for all enclosing types that have type variables
    // and collect type to be substituted for those type variables
    while (enclosingType != null) {
      TypeElement enclosingTypeElement = (TypeElement) enclosingType.asElement();
      addTypeVarMappings(types, atypeFactory, receiverType, enclosingTypeElement, mappings);
      if (enclosingType.getEnclosingType() != null
          && enclosingType.getEnclosingType().getKind() == TypeKind.DECLARED) {
        enclosingType = (DeclaredType) enclosingType.getEnclosingType();
      } else {
        enclosingType = null;
      }
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
    base = (AnnotatedDeclaredType) atypeFactory.applyCaptureConversion(base);

    List<AnnotatedTypeVariable> ownerParams =
        new ArrayList<>(enclosingType.getTypeArguments().size());
    for (AnnotatedTypeMirror typeParam : enclosingType.getTypeArguments()) {
      if (typeParam.getKind() != TypeKind.TYPEVAR) {
        throw new BugInCF(
            StringsPlume.joinLines(
                "Type arguments of a declaration should be type variables.",
                "  enclosingClassOfElem=" + enclosingClassOfElem,
                "  enclosingType=" + enclosingType,
                "  typeMirror=" + t));
      }
      ownerParams.add((AnnotatedTypeVariable) typeParam);
    }

    List<AnnotatedTypeMirror> baseParams = base.getTypeArguments();
    if (ownerParams.size() != baseParams.size() && !base.isUnderlyingTypeRaw()) {
      throw new BugInCF(
          StringsPlume.joinLines(
              "Unexpected number of parameters.",
              "enclosingType=" + enclosingType,
              "baseType=" + base));
    }
    if (!ownerParams.isEmpty() && baseParams.isEmpty() && base.isUnderlyingTypeRaw()) {
      // If base type was raw and the type arguments are missing, set them to the erased
      // type of the type variable (which is the erased type of the upper bound).
      baseParams = CollectionsPlume.mapList(AnnotatedTypeVariable::getErased, ownerParams);
    }

    for (int i = 0; i < ownerParams.size(); ++i) {
      mappings.put(ownerParams.get(i).getUnderlyingType(), baseParams.get(i).asUse());
    }
  }

  /**
   * Substitutes type arguments from raw types for type variables in {@code memberType}.
   *
   * @param atypeFactory the type factory
   * @param member the element with type {@code memberType}; used to obtain the enclosing type
   * @param memberType the type to side-effect
   * @return memberType, with type arguments substituted for type variables
   */
  private static AnnotatedTypeMirror substituteTypeArgsFromRawTypes(
      AnnotatedTypeFactory atypeFactory, Element member, AnnotatedTypeMirror memberType) {
    TypeElement enclosingClassOfMember = ElementUtils.enclosingTypeElement(member);
    Map<TypeVariable, AnnotatedTypeMirror> mappings = new HashMap<>();

    while (enclosingClassOfMember != null) {
      if (!enclosingClassOfMember.getTypeParameters().isEmpty()) {
        AnnotatedDeclaredType enclosingType = atypeFactory.getAnnotatedType(enclosingClassOfMember);
        AnnotatedDeclaredType erasedEnclosingType =
            atypeFactory.getAnnotatedType(enclosingClassOfMember);
        List<AnnotatedTypeMirror> typeArguments = enclosingType.getTypeArguments();
        for (int i = 0; i < typeArguments.size(); i++) {
          AnnotatedTypeMirror type = typeArguments.get(i);
          AnnotatedTypeMirror enclosedTypeArg = erasedEnclosingType.getTypeArguments().get(i);
          AnnotatedTypeVariable typeParameter = (AnnotatedTypeVariable) type;
          mappings.put(typeParameter.getUnderlyingType(), enclosedTypeArg);
        }
      }
      enclosingClassOfMember =
          ElementUtils.enclosingTypeElement(enclosingClassOfMember.getEnclosingElement());
    }

    if (!mappings.isEmpty()) {
      return atypeFactory.getTypeVarSubstitutor().substitute(mappings, memberType);
    }

    return memberType;
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
      for (AnnotatedDeclaredType supertype : current.directSupertypes()) {
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
    TypeElement elem = (TypeElement) method.getEnclosingElement();
    AnnotatedDeclaredType type = atypeFactory.getAnnotatedType(elem);
    Collection<AnnotatedDeclaredType> supertypes = getSuperTypes(type);
    return overriddenMethods(elements, method, supertypes);
  }

  /**
   * Given a method and all supertypes (recursively) of the method's enclosing class, returns the
   * methods that the method overrides.
   *
   * @param method the overriding method
   * @param supertypes the set of supertypes to check for methods that are overridden by {@code
   *     method}
   * @return a map from types to methods that {@code method} overrides
   */
  public static Map<AnnotatedDeclaredType, ExecutableElement> overriddenMethods(
      Elements elements, ExecutableElement method, Collection<AnnotatedDeclaredType> supertypes) {

    Map<AnnotatedDeclaredType, ExecutableElement> overrides = new LinkedHashMap<>();

    for (AnnotatedDeclaredType supertype : supertypes) {
      TypeElement superElement = (TypeElement) supertype.getUnderlyingType().asElement();
      assert superElement != null;
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
   * A pair of an empty map and false. Used in {@link #findTypeArguments(AnnotatedTypeFactory,
   * ExpressionTree, ExecutableElement, AnnotatedExecutableType, boolean)}.
   */
  private static final TypeArguments emptyFalsePair =
      new TypeArguments(Collections.emptyMap(), false, false);

  /**
   * Given a method or constructor invocation, return a mapping of the type variables to their type
   * arguments, if any exist.
   *
   * <p>It uses the method or constructor invocation type arguments if they were specified and
   * otherwise it infers them based on the passed arguments or the return type context, according to
   * JLS 15.12.2.
   *
   * @param atypeFactory the annotated type factory
   * @param expr the method or constructor invocation tree; the passed argument has to be a subtype
   *     of MethodInvocationTree or NewClassTree
   * @param elt the element corresponding to the tree
   * @param preType the (partially annotated) type corresponding to the tree - the result of
   *     AnnotatedTypes.asMemberOf with the receiver and elt
   * @param inferTypeArgs true if the type argument should be inferred
   * @return the mapping of type variables to type arguments for this method or constructor
   *     invocation, and whether unchecked conversion was required to infer the type arguments, and
   *     whether type argument inference crashed
   */
  public static TypeArguments findTypeArguments(
      AnnotatedTypeFactory atypeFactory,
      ExpressionTree expr,
      ExecutableElement elt,
      AnnotatedExecutableType preType,
      boolean inferTypeArgs) {

    if (!(expr instanceof MemberReferenceTree)
        && elt.getTypeParameters().isEmpty()
        && !TreeUtils.isDiamondTree(expr)) {
      return emptyFalsePair;
    }

    List<? extends Tree> targs;
    if (expr instanceof MethodInvocationTree) {
      targs = ((MethodInvocationTree) expr).getTypeArguments();
    } else if (expr instanceof NewClassTree) {
      targs = ((NewClassTree) expr).getTypeArguments();
    } else if (expr instanceof MemberReferenceTree) {
      MemberReferenceTree memRef = ((MemberReferenceTree) expr);
      if (inferTypeArgs && TreeUtils.needsTypeArgInference(memRef)) {
        InferenceResult inferenceResult =
            atypeFactory.getTypeArgumentInference().inferTypeArgs(atypeFactory, expr, preType);
        return new TypeArguments(
            inferenceResult.getTypeArgumentsForExpression(expr),
            inferenceResult.isUncheckedConversion(),
            inferenceResult.inferenceCrashed());
      }
      targs = memRef.getTypeArguments();
      if (memRef.getTypeArguments() == null) {
        return emptyFalsePair;
      }
    } else {
      // This case should never happen.
      throw new BugInCF("AnnotatedTypes.findTypeArguments: unexpected tree: " + expr);
    }

    if (preType.getReceiverType() != null) {
      DeclaredType receiverTypeMirror = preType.getReceiverType().getUnderlyingType();
      if (TypesUtils.isRaw(receiverTypeMirror)
          && elt.getEnclosingElement().equals(receiverTypeMirror.asElement())) {
        return emptyFalsePair;
      }
    }

    // Has the user supplied type arguments?
    if (!targs.isEmpty() && !TreeUtils.isDiamondTree(expr)) {
      List<? extends AnnotatedTypeVariable> tvars = preType.getTypeVariables();
      if (tvars.isEmpty()) {
        // This happens when the method is invoked with a raw receiver.
        return emptyFalsePair;
      }

      Map<TypeVariable, AnnotatedTypeMirror> typeArguments = new HashMap<>();
      for (int i = 0; i < elt.getTypeParameters().size(); ++i) {
        AnnotatedTypeVariable typeVar = tvars.get(i);
        AnnotatedTypeMirror typeArg = atypeFactory.getAnnotatedTypeFromTypeTree(targs.get(i));
        // TODO: the call to getTypeParameterDeclaration shouldn't be necessary - typeVar
        // already should be a declaration.
        typeArguments.put(typeVar.getUnderlyingType(), typeArg);
      }
      return new TypeArguments(typeArguments, false, false);
    } else {
      if (inferTypeArgs) {
        InferenceResult inferenceResult =
            atypeFactory.getTypeArgumentInference().inferTypeArgs(atypeFactory, expr, preType);
        return new TypeArguments(
            inferenceResult.getTypeArgumentsForExpression(expr),
            inferenceResult.isUncheckedConversion(),
            inferenceResult.inferenceCrashed());
      } else {
        return emptyFalsePair;
      }
    }
  }

  /**
   * Class representing type arguments for a method, constructor, or method reference expression.
   */
  public static class TypeArguments {

    /** A mapping from {@link TypeVariable} to its annotated type argument. */
    public final Map<TypeVariable, AnnotatedTypeMirror> typeArguments;

    /** True if unchecked conversion was needed for inference. */
    public final boolean uncheckedConversion;

    /** True if type argument inference crashed. */
    public final boolean inferenceCrash;

    /**
     * Creates a {@link TypeArguments} object.
     *
     * @param typeArguments a mapping from {@link TypeVariable} to its annotated type argument
     * @param uncheckedConversion true if unchecked conversion was needed for inference
     * @param inferenceCrash true if type argument inference crashed
     */
    public TypeArguments(
        Map<TypeVariable, AnnotatedTypeMirror> typeArguments,
        boolean uncheckedConversion,
        boolean inferenceCrash) {
      this.typeArguments = typeArguments;
      this.uncheckedConversion = uncheckedConversion;
      this.inferenceCrash = inferenceCrash;
    }
  }

  /**
   * Returns the lub of two annotated types.
   *
   * @param atypeFactory the type factory
   * @param type1 a type
   * @param type2 another type
   * @return the lub of {@code type1} and {@code type2}
   */
  public static AnnotatedTypeMirror leastUpperBound(
      AnnotatedTypeFactory atypeFactory, AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
    TypeMirror lub =
        TypesUtils.leastUpperBound(
            type1.getUnderlyingType(), type2.getUnderlyingType(), atypeFactory.getProcessingEnv());
    return leastUpperBound(atypeFactory, type1, type2, lub);
  }

  /**
   * Returns the lub, whose underlying type is {@code lubTypeMirror} of two annotated types.
   *
   * @param atypeFactory a type factory
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
   * Returns the "annotated greatest lower bound" of {@code type1} and {@code type2}.
   *
   * <p>Suppose that there is an expression e with annotated type T. The underlying type of T must
   * be the same as javac's type for e. (This is a requirement of the Checker Framework.) As a
   * corollary, when computing a glb of atype1 and atype2, it is required that
   * underlyingType(cfGLB(atype1, atype2) == glb(javacGLB(underlyingType(atype1),
   * underlyingType(atype2)). Because of this requirement, the return value of this method (the
   * "annotated GLB") may not be a subtype of one of the types.
   *
   * <p>The "annotated greatest lower bound" is defined as follows:
   *
   * <ol>
   *   <li>If the underlying type of {@code type1} and {@code type2} are the same, then return a
   *       copy of {@code type1} whose primary annotations are the greatest lower bound of the
   *       primary annotations on {@code type1} and {@code type2}.
   *   <li>If the underlying type of {@code type1} is a subtype of the underlying type of {@code
   *       type2}, then return a copy of {@code type1} whose primary annotations are the greatest
   *       lower bound of the primary annotations on {@code type1} and {@code type2}.
   *   <li>If the underlying type of {@code type1} is a supertype of the underlying type of {@code
   *       type2}, then return a copy of {@code type2} whose primary annotations are the greatest
   *       lower bound of the primary annotations on {@code type1} and {@code type2}.
   *   <li>If the underlying type of {@code type1} and {@code type2} are not in a subtyping
   *       relationship, then return an annotated intersection type whose bounds are {@code type1}
   *       and {@code type2}.
   * </ol>
   *
   * @param atypeFactory the AnnotatedTypeFactory
   * @param type1 annotated type
   * @param type2 annotated type
   * @return the annotated glb of type1 and type2
   */
  public static AnnotatedTypeMirror annotatedGLB(
      AnnotatedTypeFactory atypeFactory, AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
    TypeMirror tm1 = type1.getUnderlyingType();
    TypeMirror tm2 = type2.getUnderlyingType();
    TypeMirror glbJava = TypesUtils.greatestLowerBound(tm1, tm2, atypeFactory.getProcessingEnv());
    if (glbJava.getKind() == TypeKind.ERROR) {
      if (type1.getKind() == TypeKind.TYPEVAR) {
        return type1;
      }
      if (type2.getKind() == TypeKind.TYPEVAR) {
        return type2;
      }
      // I think the only way error happens is when one of the types is a typevarible, but
      // just in case, just return type1.
      return type1;
    }
    Types types = atypeFactory.types;
    QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();
    if (types.isSubtype(tm1, tm2)) {
      return glbSubtype(qualHierarchy, type1, type2);
    } else if (types.isSubtype(tm2, tm1)) {
      return glbSubtype(qualHierarchy, type2, type1);
    }

    if (types.isSameType(tm1, glbJava)) {
      return glbSubtype(qualHierarchy, type1, type2);
    } else if (types.isSameType(tm2, glbJava)) {
      return glbSubtype(qualHierarchy, type2, type1);
    }

    if (glbJava.getKind() != TypeKind.INTERSECTION) {
      // If one type isn't a subtype of the other, then GLB must be an intersection.
      throw new BugInCF(
          "AnnotatedTypes#annotatedGLB: expected intersection, got [%s] %s. "
              + "type1: %s, type2: %s",
          glbJava.getKind(), glbJava, type1, type2);
    }
    AnnotationMirrorSet set1 =
        AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, type1);
    AnnotationMirrorSet set2 =
        AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, type2);
    Set<? extends AnnotationMirror> glbAnno =
        qualHierarchy.greatestLowerBoundsShallow(set1, tm1, set2, tm2);

    AnnotatedIntersectionType glb =
        (AnnotatedIntersectionType) AnnotatedTypeMirror.createType(glbJava, atypeFactory, false);

    List<AnnotatedTypeMirror> newBounds = new ArrayList<>(2);
    for (AnnotatedTypeMirror bound : glb.getBounds()) {
      if (types.isSameType(bound.getUnderlyingType(), tm1)) {
        newBounds.add(type1.deepCopy());
      } else if (types.isSameType(bound.getUnderlyingType(), tm2)) {
        newBounds.add(type2.deepCopy());
      } else if (type1.getKind() == TypeKind.INTERSECTION) {
        AnnotatedIntersectionType intertype1 = (AnnotatedIntersectionType) type1;
        for (AnnotatedTypeMirror otherBound : intertype1.getBounds()) {
          if (types.isSameType(bound.getUnderlyingType(), otherBound.getUnderlyingType())) {
            newBounds.add(otherBound.deepCopy());
          }
        }
      } else if (type2.getKind() == TypeKind.INTERSECTION) {
        AnnotatedIntersectionType intertype2 = (AnnotatedIntersectionType) type2;
        for (AnnotatedTypeMirror otherBound : intertype2.getBounds()) {
          if (types.isSameType(bound.getUnderlyingType(), otherBound.getUnderlyingType())) {
            newBounds.add(otherBound.deepCopy());
          }
        }
      } else {
        throw new BugInCF(
            "Neither %s nor %s is one of the intersection bounds in %s. Bound: %s",
            type1, type2, bound, glb);
      }
    }

    glb.setBounds(newBounds);
    glb.addAnnotations(glbAnno);
    return glb;
  }

  /**
   * Returns the annotated greatest lower bound of {@code subtype} and {@code supertype}, where the
   * underlying Java types are in a subtyping relationship.
   *
   * <p>This handles cases 1, 2, and 3 mentioned in the Javadoc of {@link
   * #annotatedGLB(AnnotatedTypeFactory, AnnotatedTypeMirror, AnnotatedTypeMirror)}.
   *
   * @param qualHierarchy the qualifier hierarchy
   * @param subtype annotated type whose underlying type is a subtype of {@code supertype}
   * @param supertype annotated type whose underlying type is a supertype of {@code subtype}
   * @return the annotated greatest lower bound of {@code subtype} and {@code supertype}
   */
  private static AnnotatedTypeMirror glbSubtype(
      QualifierHierarchy qualHierarchy,
      AnnotatedTypeMirror subtype,
      AnnotatedTypeMirror supertype) {
    AnnotatedTypeMirror glb = subtype.deepCopy();
    glb.clearPrimaryAnnotations();

    TypeMirror subTM = subtype.getUnderlyingType();
    TypeMirror superTM = supertype.getUnderlyingType();
    for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
      AnnotationMirror subAnno = subtype.getPrimaryAnnotationInHierarchy(top);
      AnnotationMirror superAnno = supertype.getPrimaryAnnotationInHierarchy(top);
      if (subAnno != null && superAnno != null) {
        glb.addAnnotation(
            qualHierarchy.greatestLowerBoundShallow(subAnno, subTM, superAnno, superTM));
      } else if (subAnno == null && superAnno == null) {
        if (subtype.getKind() != TypeKind.TYPEVAR || supertype.getKind() != TypeKind.TYPEVAR) {
          throw new BugInCF(
              "Missing primary annotations: subtype: %s, supertype: %s", subtype, supertype);
        }
      } else if (subAnno == null) {
        if (subtype.getKind() != TypeKind.TYPEVAR) {
          throw new BugInCF("Missing primary annotations: subtype: %s", subtype);
        }
        AnnotationMirrorSet lb = findEffectiveLowerBoundAnnotations(qualHierarchy, subtype);
        AnnotationMirror lbAnno = qualHierarchy.findAnnotationInHierarchy(lb, top);
        if (lbAnno != null && !qualHierarchy.isSubtypeShallow(lbAnno, subTM, superAnno, superTM)) {
          // The superAnno is lower than the lower bound annotation, so add it.
          glb.addAnnotation(superAnno);
        } // else don't add any annotation.
      } else {
        throw new BugInCF("GLB: subtype: %s, supertype: %s", subtype, supertype);
      }
    }
    return glb;
  }

  /**
   * Returns the method parameters for the invoked method (or constructor), with the same number of
   * arguments as passed to the invocation tree.
   *
   * <p>This expands the parameters if the call uses varargs or contracts the parameters if the call
   * is to an anonymous class that extends a class with an enclosing type. If the call is neither of
   * these, then the parameters are returned unchanged. For example, String.format is declared to
   * take {@code (String, Object...)}. Given {@code String.format(a, b, c, d)}, this returns
   * (String, Object, Object, Object).
   *
   * @param atypeFactory the type factory to use for fetching annotated types
   * @param method the method or constructor's type
   * @param args the arguments to the method or constructor invocation
   * @param invok the method or constructor invocation
   * @return a list of the types that the invocation arguments need to be subtype of; has the same
   *     length as {@code args}
   */
  public static List<AnnotatedTypeMirror> adaptParameters(
      AnnotatedTypeFactory atypeFactory,
      AnnotatedExecutableType method,
      List<? extends ExpressionTree> args,
      Tree invok) {

    List<AnnotatedTypeMirror> parameters = method.getParameterTypes();

    // Handle anonymous constructors that extend a class with an enclosing type,
    // as in `new MyClass() { ... }`.
    if (method.getElement().getKind() == ElementKind.CONSTRUCTOR
        && method.getElement().getEnclosingElement().getSimpleName().contentEquals("")) {
      DeclaredType t =
          TypesUtils.getSuperClassOrInterface(
              method.getElement().getEnclosingElement().asType(), atypeFactory.types);
      if (t.getEnclosingType() != null) {
        if (!parameters.isEmpty()) {
          if (atypeFactory.types.isSameType(
              t.getEnclosingType(), parameters.get(0).getUnderlyingType())) {
            if (args.isEmpty()
                || !atypeFactory.types.isSameType(
                    TreeUtils.typeOf(args.get(0)), parameters.get(0).getUnderlyingType())) {
              parameters = parameters.subList(1, parameters.size());
            }
          }
        }
      }
    }

    // Handle vararg methods.
    if (!TreeUtils.isVarargsCall(invok)) {
      return parameters;
    }
    if (parameters.isEmpty()) {
      throw new BugInCF("isVarargsCall but parameters is empty: %s", invok);
    }

    AnnotatedTypeMirror lastParam = parameters.get(parameters.size() - 1);
    if (!(lastParam instanceof AnnotatedArrayType)) {
      throw new BugInCF(
          String.format(
              "for varargs call %s, last parameter %s is not an array", invok, lastParam));
    }
    AnnotatedArrayType varargs = (AnnotatedArrayType) lastParam;

    if (parameters.size() == args.size()) {
      // Check if one sent an element or an array
      AnnotatedTypeMirror lastArg = atypeFactory.getAnnotatedType(args.get(args.size() - 1));
      if (lastArg.getKind() == TypeKind.NULL
          || (lastArg.getKind() == TypeKind.ARRAY
              && getArrayDepth(varargs) == getArrayDepth((AnnotatedArrayType) lastArg))) {
        return parameters;
      }
    }

    parameters = new ArrayList<>(parameters.subList(0, parameters.size() - 1));
    for (int i = args.size() - parameters.size(); i > 0; --i) {
      parameters.add(varargs.getComponentType().deepCopy());
    }

    return parameters;
  }

  /**
   * Returns the method parameters for the invoked method, with the same number of formal parameters
   * as the arguments in the given list.
   *
   * @param method the method's type
   * @param args the types of the arguments at the call site
   * @return the method parameters, with varargs replaced by instances of its component type
   */
  public static List<AnnotatedTypeMirror> expandVarargsParametersFromTypes(
      AnnotatedExecutableType method, List<AnnotatedTypeMirror> args) {
    List<AnnotatedTypeMirror> parameters = method.getParameterTypes();
    if (!method.getElement().isVarArgs()) {
      return parameters;
    }

    AnnotatedArrayType varargs = (AnnotatedArrayType) parameters.get(parameters.size() - 1);

    if (parameters.size() == args.size()) {
      // Check if the client passed an element or an array.
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
   * Given an AnnotatedExecutableType of a method or constructor declaration, get the parameter type
   * expected at the indexth position (unwrapping varargs if necessary).
   *
   * @param methodType the type of a method or constructor containing the parameter to return
   * @param index position of the parameter type to return
   * @return the type of the parameter in the index position. If that parameter is a varArgs, return
   *     the component type of the varargs and NOT the array type.
   */
  public static AnnotatedTypeMirror getAnnotatedTypeMirrorOfParameter(
      AnnotatedExecutableType methodType, int index) {
    List<AnnotatedTypeMirror> parameterTypes = methodType.getParameterTypes();
    boolean hasVarargs = methodType.getElement().isVarArgs();

    int lastIndex = parameterTypes.size() - 1;
    AnnotatedTypeMirror lastType = parameterTypes.get(lastIndex);
    boolean parameterBeforeVarargs = index < lastIndex;
    if (!parameterBeforeVarargs && lastType instanceof AnnotatedArrayType) {
      AnnotatedArrayType arrayType = (AnnotatedArrayType) lastType;
      if (hasVarargs) {
        return arrayType.getComponentType();
      }
    }
    return parameterTypes.get(index);
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
   * Returns true if type contains the given modifier, also recursively in type arguments and
   * arrays. This method might be easier to implement directly as instance method in
   * AnnotatedTypeMirror; it corresponds to a "deep" version of {@link
   * AnnotatedTypeMirror#hasPrimaryAnnotation(AnnotationMirror)}.
   *
   * @param type the type to search
   * @param modifier the modifier to search for
   * @return true if the type contains the modifier
   */
  public static boolean containsModifier(AnnotatedTypeMirror type, AnnotationMirror modifier) {
    return containsModifierImpl(type, modifier, new ArrayList<>());
  }

  /*
   * For type variables we might hit the same type again. We keep a list of visited types.
   */
  private static boolean containsModifierImpl(
      AnnotatedTypeMirror type, AnnotationMirror modifier, List<AnnotatedTypeMirror> visited) {
    boolean found = type.hasPrimaryAnnotation(modifier);
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
  private static final @CanonicalName String annotationClassName =
      java.lang.annotation.Annotation.class.getCanonicalName();

  /**
   * Returns true if the underlying type of this atm is a java.lang.annotation.Annotation.
   *
   * @return true if the underlying type of this atm is a java.lang.annotation.Annotation
   */
  public static boolean isJavaLangAnnotation(AnnotatedTypeMirror atm) {
    return TypesUtils.isDeclaredOfName(atm.getUnderlyingType(), annotationClassName);
  }

  /**
   * Returns true if atm is an Annotation interface, i.e., an implementation of
   * java.lang.annotation.Annotation. Given {@code @interface MyAnno}, a call to {@code
   * implementsAnnotation} returns true when called on an AnnotatedDeclaredType representing a use
   * of MyAnno.
   *
   * @return true if atm is an Annotation interface
   */
  public static boolean implementsAnnotation(AnnotatedTypeMirror atm) {
    if (atm.getKind() != TypeKind.DECLARED) {
      return false;
    }
    AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
        (AnnotatedTypeMirror.AnnotatedDeclaredType) atm;

    Symbol.ClassSymbol classSymbol =
        (Symbol.ClassSymbol) declaredType.getUnderlyingType().asElement();
    for (Type iface : classSymbol.getInterfaces()) {
      if (TypesUtils.isDeclaredOfName(iface, annotationClassName)) {
        return true;
      }
    }

    return false;
  }

  public static boolean isEnum(AnnotatedTypeMirror typeMirror) {
    if (typeMirror.getKind() == TypeKind.DECLARED) {
      AnnotatedDeclaredType adt = (AnnotatedDeclaredType) typeMirror;
      return TypesUtils.isDeclaredOfName(adt.getUnderlyingType(), java.lang.Enum.class.getName());
    }

    return false;
  }

  public static boolean isDeclarationOfJavaLangEnum(
      Types types, Elements elements, AnnotatedTypeMirror typeMirror) {
    if (isEnum(typeMirror)) {
      return elements
          .getTypeElement(Enum.class.getCanonicalName())
          .equals(((AnnotatedDeclaredType) typeMirror).getUnderlyingType().asElement());
    }

    return false;
  }

  /**
   * Returns true if the typeVar1 and typeVar2 are two uses of the same type variable.
   *
   * @param types type utils
   * @param typeVar1 a type variable
   * @param typeVar2 a type variable
   * @return true if the typeVar1 and typeVar2 are two uses of the same type variable
   */
  @SuppressWarnings(
      "interning:not.interned" // This is an equals method but @EqualsMethod can't be used
  // because this method has 3 arguments.
  )
  public static boolean haveSameDeclaration(
      Types types, AnnotatedTypeVariable typeVar1, AnnotatedTypeVariable typeVar2) {

    if (typeVar1.getUnderlyingType() == typeVar2.getUnderlyingType()) {
      return true;
    }
    return types.isSameType(typeVar1.getUnderlyingType(), typeVar2.getUnderlyingType());
  }

  /**
   * When overriding a method, you must include the same number of type parameters as the base
   * method. By index, these parameters are considered equivalent to the type parameters of the
   * overridden method.
   *
   * <p>Necessary conditions:
   *
   * <ul>
   *   <li>Both type variables are defined in methods.
   *   <li>One of the two methods overrides the other.
   *   <li>Within their method declaration, both types have the same type parameter index.
   * </ul>
   *
   * @return true if type1 and type2 are corresponding type variables (that is, either one
   *     "overrides" the other)
   */
  public static boolean areCorrespondingTypeVariables(
      Elements elements, AnnotatedTypeVariable type1, AnnotatedTypeVariable type2) {
    TypeParameterElement type1ParamElem =
        (TypeParameterElement) type1.getUnderlyingType().asElement();
    TypeParameterElement type2ParamElem =
        (TypeParameterElement) type2.getUnderlyingType().asElement();

    if (type1ParamElem.getGenericElement() instanceof ExecutableElement
        && type2ParamElem.getGenericElement() instanceof ExecutableElement) {
      ExecutableElement type1Executable = (ExecutableElement) type1ParamElem.getGenericElement();
      ExecutableElement type2Executable = (ExecutableElement) type2ParamElem.getGenericElement();

      TypeElement type1Class = (TypeElement) type1Executable.getEnclosingElement();
      TypeElement type2Class = (TypeElement) type2Executable.getEnclosingElement();

      boolean methodIsOverridden =
          elements.overrides(type1Executable, type2Executable, type1Class)
              || elements.overrides(type2Executable, type1Executable, type2Class);
      if (methodIsOverridden) {
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
   * variables, wildcards, and intersections in those bounds. This method traverses the bounds until
   * it finds a concrete type from which it can pull an annotation.
   *
   * @param top the top of the hierarchy for which you are searching
   * @return the AnnotationMirror that represents the type of toSearch in the hierarchy of top
   */
  public static AnnotationMirror findEffectiveAnnotationInHierarchy(
      QualifierHierarchy qualHierarchy, AnnotatedTypeMirror toSearch, AnnotationMirror top) {
    return findEffectiveAnnotationInHierarchy(qualHierarchy, toSearch, top, false);
  }

  /**
   * When comparing types against the bounds of a type variable, we may encounter other type
   * variables, wildcards, and intersections in those bounds. This method traverses the bounds until
   * it finds a concrete type from which it can pull an annotation.
   *
   * @param top the top of the hierarchy for which you are searching
   * @param canBeEmpty true if the effective type can have NO annotation in the hierarchy specified
   *     by top. If this param is false, an exception will be thrown if no annotation is found.
   *     Otherwise the result is null.
   * @return the AnnotationMirror that represents the type of {@code toSearch} in the hierarchy of
   *     {@code top}
   */
  public static @Nullable AnnotationMirror findEffectiveAnnotationInHierarchy(
      QualifierHierarchy qualHierarchy,
      AnnotatedTypeMirror toSearch,
      AnnotationMirror top,
      boolean canBeEmpty) {
    AnnotatedTypeMirror source = toSearch;
    while (source.getPrimaryAnnotationInHierarchy(top) == null) {

      switch (source.getKind()) {
        case TYPEVAR:
          source = ((AnnotatedTypeVariable) source).getUpperBound();
          break;

        case WILDCARD:
          source = ((AnnotatedWildcardType) source).getExtendsBound();
          break;

        case INTERSECTION:
          // if there are multiple conflicting annotations, choose the lowest
          AnnotationMirror glb =
              glbOfBoundsInHierarchy((AnnotatedIntersectionType) source, top, qualHierarchy);

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
              StringsPlume.joinLines(
                  "Unexpected AnnotatedTypeMirror with no primary annotation.",
                  "toSearch=" + toSearch,
                  "top=" + top,
                  "source=" + source));
      }
    }

    return source.getPrimaryAnnotationInHierarchy(top);
  }

  /**
   * This method returns the effective annotation on the lower bound of a type, or on the type
   * itself if the type has no lower bound (it is not a type variable, wildcard, or intersection).
   *
   * @param qualHierarchy the qualifier hierarchy
   * @param toSearch the type whose lower bound to examine
   * @return the set of effective annotation mirrors in all hierarchies
   */
  public static AnnotationMirrorSet findEffectiveLowerBoundAnnotations(
      QualifierHierarchy qualHierarchy, AnnotatedTypeMirror toSearch) {
    AnnotatedTypeMirror source = toSearch;
    TypeKind kind = source.getKind();
    while (kind == TypeKind.TYPEVAR || kind == TypeKind.WILDCARD || kind == TypeKind.INTERSECTION) {

      switch (source.getKind()) {
        case TYPEVAR:
          source = ((AnnotatedTypeVariable) source).getLowerBound();
          break;

        case WILDCARD:
          source = ((AnnotatedWildcardType) source).getSuperBound();
          break;

        case INTERSECTION:
          // if there are multiple conflicting annotations, choose the lowest
          AnnotationMirrorSet glb = glbOfBounds((AnnotatedIntersectionType) source, qualHierarchy);
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

    return source.getPrimaryAnnotations();
  }

  /**
   * When comparing types against the bounds of a type variable, we may encounter other type
   * variables, wildcards, and intersections in those bounds. This method traverses the bounds until
   * it finds a concrete type from which it can pull an annotation. This occurs for every hierarchy
   * in QualifierHierarchy.
   *
   * @param qualHierarchy the qualifier hierarchy
   * @param toSearch the type whose effective annotations to determine
   * @return the set of effective annotation mirrors in all hierarchies
   */
  public static AnnotationMirrorSet findEffectiveAnnotations(
      QualifierHierarchy qualHierarchy, AnnotatedTypeMirror toSearch) {
    AnnotatedTypeMirror source = toSearch;
    TypeKind kind = source.getKind();
    while (kind == TypeKind.TYPEVAR || kind == TypeKind.WILDCARD || kind == TypeKind.INTERSECTION) {

      switch (source.getKind()) {
        case TYPEVAR:
          source = ((AnnotatedTypeVariable) source).getUpperBound();
          break;

        case WILDCARD:
          source = ((AnnotatedWildcardType) source).getExtendsBound();
          break;

        case INTERSECTION:
          // if there are multiple conflicting annotations, choose the lowest
          AnnotationMirrorSet glb = glbOfBounds((AnnotatedIntersectionType) source, qualHierarchy);
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

    return source.getPrimaryAnnotations();
  }

  private static AnnotationMirror glbOfBoundsInHierarchy(
      AnnotatedIntersectionType isect, AnnotationMirror top, QualifierHierarchy qualHierarchy) {
    AnnotationMirror anno = isect.getPrimaryAnnotationInHierarchy(top);
    for (AnnotatedTypeMirror bound : isect.getBounds()) {
      AnnotationMirror boundAnno = bound.getPrimaryAnnotationInHierarchy(top);
      if (boundAnno != null
          && (anno == null
              || qualHierarchy.isSubtypeShallow(
                  boundAnno, bound.getUnderlyingType(), anno, isect.getUnderlyingType()))) {
        anno = boundAnno;
      }
    }

    return anno;
  }

  /**
   * Gets the lowest primary annotation of all bounds in the intersection.
   *
   * @param isect the intersection for which we are glbing bounds
   * @param qualHierarchy the qualifier used to get the hierarchies in which to glb
   * @return a set of annotations representing the glb of the intersection's bounds
   */
  public static AnnotationMirrorSet glbOfBounds(
      AnnotatedIntersectionType isect, QualifierHierarchy qualHierarchy) {
    AnnotationMirrorSet result = new AnnotationMirrorSet();
    for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
      AnnotationMirror glbAnno = glbOfBoundsInHierarchy(isect, top, qualHierarchy);
      if (glbAnno != null) {
        result.add(glbAnno);
      }
    }

    return result;
  }

  // For Wildcards, isSuperBound() and isExtendsBound() will return true if isUnbound() does.
  // But don't use isUnbound(), because as of Java 18, it returns true for "? extends Object".

  /**
   * This method identifies wildcard types that are unbound.
   *
   * @param wildcard the type to check
   * @return true if the given card is an unbounded wildcard
   */
  public static boolean hasNoExplicitBound(AnnotatedTypeMirror wildcard) {
    return TypesUtils.hasNoExplicitBound(wildcard.getUnderlyingType());
  }

  /**
   * Returns true if wildcard type has an explicit super bound.
   *
   * @param wildcard the wildcard type to test
   * @return true if wildcard type is explicitly super bounded
   */
  public static boolean hasExplicitSuperBound(AnnotatedTypeMirror wildcard) {
    return TypesUtils.hasExplicitSuperBound(wildcard.getUnderlyingType());
  }

  /**
   * Returns true if wildcard type has an explicit extends bound.
   *
   * @param wildcard the wildcard type to test
   * @return true if wildcard type is explicitly extends bounded
   */
  public static boolean hasExplicitExtendsBound(AnnotatedTypeMirror wildcard) {
    return TypesUtils.hasExplicitExtendsBound(wildcard.getUnderlyingType());
  }

  /**
   * Returns true if this type is super bounded or unbounded.
   *
   * @param wildcard the wildcard type to test
   * @return true if this type is super bounded or unbounded
   */
  public static boolean isUnboundedOrSuperBounded(AnnotatedWildcardType wildcard) {
    return TypesUtils.isUnboundedOrSuperBounded(wildcard.getUnderlyingType());
  }

  /**
   * Returns true if this type is extends bounded or unbounded.
   *
   * @param wildcard the wildcard type to test
   * @return true if this type is extends bounded or unbounded
   */
  public static boolean isUnboundedOrExtendsBounded(AnnotatedWildcardType wildcard) {
    return TypesUtils.isUnboundedOrExtendsBounded(wildcard.getUnderlyingType());
  }

  /**
   * Copies explicit annotations and annotations resulting from resolution of polymorphic qualifiers
   * from {@code constructor} to {@code returnType}. If {@code returnType} has an annotation in the
   * same hierarchy of an annotation to be copied, that annotation is not copied.
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
    AnnotationMirrorSet decret = new AnnotationMirrorSet();
    for (Attribute.TypeCompound da : decall) {
      if (da.position.type == com.sun.tools.javac.code.TargetType.METHOD_RETURN) {
        decret.add(da);
      }
    }

    QualifierHierarchy qualHierarchy = atypeFactory.getQualifierHierarchy();

    // Collect all polymorphic qualifiers; we should substitute them.
    AnnotationMirrorSet polys = new AnnotationMirrorSet();
    for (AnnotationMirror anno : returnType.getPrimaryAnnotations()) {
      if (qualHierarchy.isPolymorphicQualifier(anno)) {
        polys.add(anno);
      }
    }

    for (AnnotationMirror cta : constructor.getReturnType().getPrimaryAnnotations()) {
      AnnotationMirror ctatop = qualHierarchy.getTopAnnotation(cta);
      if (returnType.hasPrimaryAnnotationInHierarchy(cta)) {
        continue;
      }
      if (atypeFactory.isSupportedQualifier(cta)
          && !returnType.hasPrimaryAnnotationInHierarchy(cta)) {
        for (AnnotationMirror fromDecl : decret) {
          if (atypeFactory.isSupportedQualifier(fromDecl)
              && AnnotationUtils.areSame(ctatop, qualHierarchy.getTopAnnotation(fromDecl))) {
            returnType.addAnnotation(cta);
            break;
          }
        }
      }

      // Go through the polymorphic qualifiers and see whether
      // there is anything left to replace.
      for (AnnotationMirror pa : polys) {
        if (AnnotationUtils.areSame(ctatop, qualHierarchy.getTopAnnotation(pa))) {
          returnType.replaceAnnotation(cta);
          break;
        }
      }
    }
  }

  /**
   * Add all the annotations in {@code declaredType} to {@code annotatedDeclaredType}.
   *
   * <p>(The {@code TypeMirror} returned by {@code annotatedDeclaredType#getUnderlyingType} may not
   * have all the annotations on the type, so allow the user to specify a different one.)
   *
   * @param annotatedDeclaredType annotated type to which annotations are added
   * @param declaredType a type that may have annotations
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

  /**
   * Returns true if {@code type} is a type argument to a type whose {@code #underlyingType} is raw.
   * The Checker Framework gives raw types wildcard type arguments so that the annotated type can be
   * used as if the annotated type was not raw.
   *
   * @param type an annotated type
   * @return true if this is a type argument to a type whose {@code #underlyingType} is raw
   */
  public static boolean isTypeArgOfRawType(AnnotatedTypeMirror type) {
    return type.getKind() == TypeKind.WILDCARD
        && ((AnnotatedWildcardType) type).isTypeArgOfRawType();
  }
}
