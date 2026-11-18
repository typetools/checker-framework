package org.checkerframework.framework.util;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Computes the upward and downward projections of annotated types, as defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.10.5">JLS 4.10.5</a>.
 * The restricted type variables are either every captured type variable, including the fresh type
 * variables that type inference creates, or a set of type variables that the caller passes.
 *
 * <p>JLS 4.10.5 defines projection only for Java types. This class chooses the annotations of the
 * result as follows:
 *
 * <ul>
 *   <li>The projection of a restricted type variable that has primary annotations has those
 *       annotations, not the annotations of the projection of its bound.
 *   <li>A wildcard whose Java type is {@code ?} or {@code ? super L} has the upper bound {@code
 *       Object}, with the primary annotations of the projection of the type argument or wildcard
 *       bound that the wildcard replaces. Those annotations can be more precise than the
 *       annotations of the declared bound of the type parameter.
 *   <li>A wildcard with no lower bound has the null type, with the bottom annotations, as its lower
 *       bound.
 *   <li>The conditions of JLS 4.10.5 that compare types, such as whether a declared bound is a
 *       subtype of U, compare Java types and ignore annotations.
 * </ul>
 *
 * <p>This class also differs from JLS 4.10.5 in two ways:
 *
 * <ul>
 *   <li>The upward projection of a restricted type variable is the upward projection of its upper
 *       bound, which can mention the type variable itself, as in {@code T extends Comparable<T>}.
 *       JLS 4.10.5 does not say how this recursion ends. In this class, the upward projection of a
 *       type variable that is reached again while its own upward projection is being computed is
 *       the erasure of its leftmost bound.
 *   <li>Where JLS 4.10.5 gives {@code ? extends U} and U is an intersection type, this class gives
 *       {@code ?}, because the bound of a wildcard cannot be an intersection type.
 * </ul>
 */
public final class AnnotatedTypeProjection {

  /** The type factory. */
  private final AnnotatedTypeFactory atypeFactory;

  /** The type utilities. */
  private final Types types;

  /** The javac type utilities. */
  private final com.sun.tools.javac.code.Types javacTypes;

  /** The restricted type variables, or null if every captured type variable is restricted. */
  private final @Nullable Collection<TypeVariable> restricted;

  /**
   * The restricted type variables whose upward projection is being computed. Projecting one of
   * these again would not terminate, because its bound mentions itself.
   */
  private final List<TypeVariable> typeVariablesBeingProjected = new ArrayList<>();

  /**
   * Creates an {@code AnnotatedTypeProjection}.
   *
   * @param atypeFactory the type factory
   * @param restricted the restricted type variables, or null if every captured type variable is
   *     restricted
   */
  private AnnotatedTypeProjection(
      AnnotatedTypeFactory atypeFactory, @Nullable Collection<TypeVariable> restricted) {
    this.atypeFactory = atypeFactory;
    this.types = atypeFactory.types;
    this.javacTypes =
        com.sun.tools.javac.code.Types.instance(
            ((JavacProcessingEnvironment) atypeFactory.getProcessingEnv()).getContext());
    this.restricted = restricted;
  }

  /**
   * Returns the upward projection of {@code type} with respect to all captured type variables: a
   * supertype of {@code type} that does not mention any captured type variable.
   *
   * @param atypeFactory the type factory
   * @param type the type to project; not a wildcard
   * @return the upward projection of {@code type}
   */
  public static AnnotatedTypeMirror upward(
      AnnotatedTypeFactory atypeFactory, AnnotatedTypeMirror type) {
    return new AnnotatedTypeProjection(atypeFactory, null).upward(type);
  }

  /**
   * Returns the upward projection of {@code type} with respect to the type variables in {@code
   * restricted}: a supertype of {@code type} that does not mention any of them.
   *
   * @param atypeFactory the type factory
   * @param type the type to project; not a wildcard
   * @param restricted the restricted type variables
   * @return the upward projection of {@code type}
   */
  public static AnnotatedTypeMirror upward(
      AnnotatedTypeFactory atypeFactory,
      AnnotatedTypeMirror type,
      Collection<TypeVariable> restricted) {
    return new AnnotatedTypeProjection(atypeFactory, restricted).upward(type);
  }

  /**
   * Returns the upward projection of {@code type}.
   *
   * @param type the type to project; not a wildcard
   * @return the upward projection of {@code type}
   */
  private AnnotatedTypeMirror upward(AnnotatedTypeMirror type) {
    if (!mentionsRestricted(type.getUnderlyingType())) {
      return type.deepCopy();
    }
    switch (type.getKind()) {
      case TYPEVAR -> {
        AnnotatedTypeVariable typeVariable = (AnnotatedTypeVariable) type;
        TypeVariable javaType = typeVariable.getUnderlyingType();
        AnnotatedTypeMirror result;
        if (isUpwardProjectionInProgress(javaType)) {
          // The erasure of a type variable is the erasure of its leftmost bound (JLS 4.6).
          AnnotatedTypeMirror leftmostBound = typeVariable.getUpperBound();
          if (leftmostBound.getKind() == TypeKind.INTERSECTION) {
            leftmostBound = ((AnnotatedIntersectionType) leftmostBound).getBounds().get(0);
          }
          result = leftmostBound.getErased();
        } else {
          typeVariablesBeingProjected.add(javaType);
          result = upward(typeVariable.getUpperBound());
          typeVariablesBeingProjected.remove(typeVariablesBeingProjected.size() - 1);
        }
        if (!typeVariable.getPrimaryAnnotations().isEmpty()) {
          result.replaceAnnotations(typeVariable.getPrimaryAnnotations());
        }
        return result;
      }
      case DECLARED -> {
        AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
        List<AnnotatedTypeMirror> typeArgs = new ArrayList<>();
        for (int i = 0; i < declaredType.getTypeArguments().size(); i++) {
          typeArgs.add(upwardTypeArgument(declaredType, i));
        }
        AnnotatedDeclaredType enclosingType = declaredType.getEnclosingType();
        return withEnclosingTypeAndTypeArguments(
            declaredType,
            enclosingType == null ? null : (AnnotatedDeclaredType) upward(enclosingType),
            typeArgs);
      }
      case ARRAY -> {
        AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
        return withComponentType(arrayType, upward(arrayType.getComponentType()));
      }
      case INTERSECTION -> {
        List<AnnotatedTypeMirror> bounds = new ArrayList<>();
        for (AnnotatedTypeMirror bound : ((AnnotatedIntersectionType) type).getBounds()) {
          bounds.add(upward(bound));
        }
        return withBounds((AnnotatedIntersectionType) type, bounds);
      }
      // A wildcard is a type argument, not a type; upwardTypeArgument projects wildcards.
      case WILDCARD -> throw new BugInCF("Upward projection of a wildcard: %s", type);
      default -> throw new BugInCF("Upward projection of a %s: %s", type.getKind(), type);
    }
  }

  /**
   * Returns the type argument of the upward projection of {@code type} at index {@code
   * typeArgIndex}.
   *
   * @param type a parameterized type
   * @param typeArgIndex the index of a type argument of {@code type}
   * @return the type argument of the upward projection of {@code type} at index {@code
   *     typeArgIndex}
   */
  private AnnotatedTypeMirror upwardTypeArgument(AnnotatedDeclaredType type, int typeArgIndex) {
    AnnotatedTypeMirror typeArg = type.getTypeArguments().get(typeArgIndex);
    if (!mentionsRestricted(typeArg.getUnderlyingType())) {
      return typeArg.deepCopy();
    }
    if (typeArg.getKind() == TypeKind.WILDCARD) {
      AnnotatedWildcardType wildcard = (AnnotatedWildcardType) typeArg;
      AnnotatedTypeMirror upperBound = upward(wildcard.getExtendsBound());
      if (wildcard.getUnderlyingType().getSuperBound() == null) {
        return createWildcard(upperBound, null, true);
      }
      return createWildcard(upperBound, downward(wildcard.getSuperBound()), false);
    }

    AnnotatedTypeMirror projectedTypeArg = upward(typeArg);
    TypeElement classOrInterface = (TypeElement) type.getUnderlyingType().asElement();
    TypeParameterElement typeParameter = classOrInterface.getTypeParameters().get(typeArgIndex);
    TypeMirror declaredBound = ((TypeVariable) typeParameter.asType()).getUpperBound();
    if (!TypesUtils.isObject(projectedTypeArg.getUnderlyingType())
        && (mentionsTypeParameter(declaredBound, classOrInterface)
            || !types.isSubtype(declaredBound, projectedTypeArg.getUnderlyingType()))) {
      return createWildcard(projectedTypeArg, null, true);
    }
    return createWildcard(projectedTypeArg, downward(typeArg), false);
  }

  /**
   * Returns the downward projection of {@code type} with respect to the restricted type variables,
   * or null if it is undefined.
   *
   * @param type a type
   * @return the downward projection of {@code type}, or null if it is undefined
   */
  private @Nullable AnnotatedTypeMirror downward(AnnotatedTypeMirror type) {
    if (!mentionsRestricted(type.getUnderlyingType())) {
      return type.deepCopy();
    }
    switch (type.getKind()) {
      case TYPEVAR -> {
        AnnotatedTypeVariable typeVariable = (AnnotatedTypeVariable) type;
        AnnotatedTypeMirror lowerBound = typeVariable.getLowerBound();
        if (lowerBound.getKind() == TypeKind.NULL) {
          return null;
        }
        AnnotatedTypeMirror result = downward(lowerBound);
        if (result != null && !typeVariable.getPrimaryAnnotations().isEmpty()) {
          result.replaceAnnotations(typeVariable.getPrimaryAnnotations());
        }
        return result;
      }
      case DECLARED -> {
        AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
        AnnotatedDeclaredType enclosingType = declaredType.getEnclosingType();
        AnnotatedDeclaredType projectedEnclosingType = null;
        if (enclosingType != null) {
          projectedEnclosingType = (AnnotatedDeclaredType) downward(enclosingType);
          if (projectedEnclosingType == null) {
            return null;
          }
        }
        List<AnnotatedTypeMirror> typeArgs = new ArrayList<>();
        for (AnnotatedTypeMirror typeArg : declaredType.getTypeArguments()) {
          if (!mentionsRestricted(typeArg.getUnderlyingType())) {
            typeArgs.add(typeArg.deepCopy());
          } else if (typeArg.getKind() != TypeKind.WILDCARD) {
            return null;
          } else {
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) typeArg;
            if (wildcard.getUnderlyingType().getSuperBound() == null) {
              AnnotatedTypeMirror upperBound = downward(wildcard.getExtendsBound());
              if (upperBound == null) {
                return null;
              }
              typeArgs.add(createWildcard(upperBound, null, true));
            } else {
              typeArgs.add(
                  createWildcard(
                      upward(wildcard.getExtendsBound()), upward(wildcard.getSuperBound()), false));
            }
          }
        }
        return withEnclosingTypeAndTypeArguments(declaredType, projectedEnclosingType, typeArgs);
      }
      case ARRAY -> {
        AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
        AnnotatedTypeMirror projectedComponentType = downward(arrayType.getComponentType());
        return projectedComponentType == null
            ? null
            : withComponentType(arrayType, projectedComponentType);
      }
      case INTERSECTION -> {
        List<AnnotatedTypeMirror> bounds = new ArrayList<>();
        for (AnnotatedTypeMirror bound : ((AnnotatedIntersectionType) type).getBounds()) {
          AnnotatedTypeMirror projectedBound = downward(bound);
          if (projectedBound == null) {
            return null;
          }
          bounds.add(projectedBound);
        }
        return withBounds((AnnotatedIntersectionType) type, bounds);
      }
      default -> throw new BugInCF("Downward projection of a %s: %s", type.getKind(), type);
    }
  }

  /**
   * Creates a wildcard. Its Java type is {@code ? super lowerBound} if {@code lowerBound} is
   * non-null, {@code ? extends upperBound} if {@code preferExtendsBound} is true and {@code
   * upperBound} is not an intersection type, and {@code ?} otherwise. Its annotated upper bound is
   * {@code upperBound} if its Java type is {@code ? extends upperBound}, and otherwise {@code
   * Object} with the qualifiers of {@code upperBound}.
   *
   * @param upperBound the upper bound of the wildcard
   * @param lowerBound the lower bound of the wildcard, or null if it has none
   * @param preferExtendsBound true if the Java type of the wildcard should be {@code ? extends
   *     upperBound}; ignored if {@code lowerBound} is non-null
   * @return a wildcard with the given bounds
   */
  private AnnotatedWildcardType createWildcard(
      AnnotatedTypeMirror upperBound,
      @Nullable AnnotatedTypeMirror lowerBound,
      boolean preferExtendsBound) {
    // A wildcard's bound cannot be an intersection type, so `?` stands in for `? extends A & B`.
    boolean hasExtendsBound =
        lowerBound == null && preferExtendsBound && upperBound.getKind() != TypeKind.INTERSECTION;
    TypeMirror javaType =
        TypesUtils.createWildcard(
            lowerBound == null ? null : lowerBound.getUnderlyingType(),
            hasExtendsBound ? upperBound.getUnderlyingType() : null,
            types);
    AnnotatedWildcardType wildcard =
        (AnnotatedWildcardType) AnnotatedTypeMirror.createType(javaType, atypeFactory, false);
    if (lowerBound == null) {
      lowerBound = AnnotatedTypeMirror.createType(types.getNullType(), atypeFactory, false);
      lowerBound.addAnnotations(atypeFactory.getQualifierHierarchy().getBottomAnnotations());
    }
    if (hasExtendsBound) {
      wildcard.setExtendsBound(upperBound);
    } else {
      // See the class Javadoc for why the Object bound has the qualifiers of `upperBound`.
      AnnotatedTypeMirror objectUpperBound =
          AnnotatedTypeMirror.createType(
              TypesUtils.getObjectTypeMirror(atypeFactory.getProcessingEnv()), atypeFactory, false);
      objectUpperBound.addAnnotations(upperBound.getAnnotations());
      wildcard.setExtendsBound(objectUpperBound);
    }
    wildcard.setSuperBound(lowerBound);
    return wildcard;
  }

  /**
   * Returns a copy of {@code type} whose enclosing type is {@code enclosingType} and whose type
   * arguments are {@code typeArgs}.
   *
   * @param type a declared type
   * @param enclosingType the new enclosing type, or null if {@code type} has none
   * @param typeArgs the new type arguments
   * @return a copy of {@code type} whose enclosing type is {@code enclosingType} and whose type
   *     arguments are {@code typeArgs}
   */
  private AnnotatedDeclaredType withEnclosingTypeAndTypeArguments(
      AnnotatedDeclaredType type,
      @Nullable AnnotatedDeclaredType enclosingType,
      List<AnnotatedTypeMirror> typeArgs) {
    DeclaredType javaType = type.getUnderlyingType();
    TypeMirror[] javaTypeArgs = new TypeMirror[typeArgs.size()];
    for (int i = 0; i < typeArgs.size(); i++) {
      javaTypeArgs[i] = typeArgs.get(i).getUnderlyingType();
    }
    TypeElement classOrInterface = (TypeElement) javaType.asElement();
    DeclaredType newJavaType =
        enclosingType == null
            ? types.getDeclaredType(classOrInterface, javaTypeArgs)
            : types.getDeclaredType(
                enclosingType.getUnderlyingType(), classOrInterface, javaTypeArgs);
    AnnotatedDeclaredType result =
        (AnnotatedDeclaredType) AnnotatedTypeMirror.createType(newJavaType, atypeFactory, false);
    result.addAnnotations(type.getPrimaryAnnotations());
    result.setTypeArguments(typeArgs);
    if (enclosingType != null) {
      result.setEnclosingType(enclosingType);
    }
    return result;
  }

  /**
   * Returns a copy of {@code type} whose bounds are {@code bounds}. A bound that is itself an
   * intersection type contributes its own bounds.
   *
   * @param type an intersection type
   * @param bounds the new bounds
   * @return a copy of {@code type} whose bounds are {@code bounds}
   */
  private AnnotatedIntersectionType withBounds(
      AnnotatedIntersectionType type, List<AnnotatedTypeMirror> bounds) {
    List<AnnotatedTypeMirror> flatBounds = new ArrayList<>();
    for (AnnotatedTypeMirror bound : bounds) {
      if (bound.getKind() == TypeKind.INTERSECTION) {
        flatBounds.addAll(((AnnotatedIntersectionType) bound).getBounds());
      } else {
        flatBounds.add(bound);
      }
    }
    com.sun.tools.javac.util.List<Type> javaBounds = com.sun.tools.javac.util.List.nil();
    for (int i = flatBounds.size() - 1; i >= 0; i--) {
      javaBounds = javaBounds.prepend((Type) flatBounds.get(i).getUnderlyingType());
    }
    AnnotatedIntersectionType result =
        (AnnotatedIntersectionType)
            AnnotatedTypeMirror.createType(
                javacTypes.makeIntersectionType(javaBounds), atypeFactory, false);
    result.addAnnotations(type.getPrimaryAnnotations());
    result.setBounds(flatBounds);
    return result;
  }

  /**
   * Returns a copy of {@code type} whose component type is {@code componentType}.
   *
   * @param type an array type
   * @param componentType the new component type
   * @return a copy of {@code type} whose component type is {@code componentType}
   */
  private AnnotatedArrayType withComponentType(
      AnnotatedArrayType type, AnnotatedTypeMirror componentType) {
    ArrayType javaType = types.getArrayType(componentType.getUnderlyingType());
    AnnotatedArrayType result =
        (AnnotatedArrayType) AnnotatedTypeMirror.createType(javaType, atypeFactory, false);
    result.addAnnotations(type.getPrimaryAnnotations());
    result.setComponentType(componentType);
    return result;
  }

  /**
   * Returns true if the upward projection of {@code typeVariable} is being computed.
   *
   * @param typeVariable a type variable
   * @return true if the upward projection of {@code typeVariable} is being computed
   */
  private boolean isUpwardProjectionInProgress(TypeVariable typeVariable) {
    return containsSame(typeVariablesBeingProjected, typeVariable);
  }

  /**
   * Returns true if {@code typeVariable} is restricted.
   *
   * @param typeVariable a type variable
   * @return true if {@code typeVariable} is restricted
   */
  private boolean isRestricted(TypeVariable typeVariable) {
    return restricted == null
        ? TypesUtils.isCapturedTypeVariable(typeVariable)
        : containsSame(restricted, typeVariable);
  }

  /**
   * Returns true if {@code typeVariables} contains a type variable that is the same as {@code
   * typeVariable}, according to {@link TypesUtils#areSame}.
   *
   * @param typeVariables a collection of type variables
   * @param typeVariable a type variable
   * @return true if {@code typeVariables} contains {@code typeVariable}
   */
  private static boolean containsSame(
      Collection<TypeVariable> typeVariables, TypeVariable typeVariable) {
    for (TypeVariable candidate : typeVariables) {
      if (TypesUtils.areSame(candidate, typeVariable)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if {@code type} mentions a restricted type variable.
   *
   * @param type a type
   * @return true if {@code type} mentions a restricted type variable
   */
  private boolean mentionsRestricted(TypeMirror type) {
    return mentionsTypeVariable(type, this::isRestricted);
  }

  /**
   * Returns true if {@code type} mentions a type parameter of {@code classOrInterface}.
   *
   * @param type a type
   * @param classOrInterface a class or interface
   * @return true if {@code type} mentions a type parameter of {@code classOrInterface}
   */
  private static boolean mentionsTypeParameter(TypeMirror type, TypeElement classOrInterface) {
    return mentionsTypeVariable(
        type,
        typeVariable -> typeVariable.asElement().getEnclosingElement().equals(classOrInterface));
  }

  /**
   * Returns true if {@code type} mentions a type variable for which {@code test} returns true.
   *
   * @param type a type
   * @param test a predicate on type variables
   * @return true if {@code type} mentions a type variable for which {@code test} returns true
   */
  private static boolean mentionsTypeVariable(TypeMirror type, Predicate<TypeVariable> test) {
    switch (type.getKind()) {
      case TYPEVAR -> {
        return test.test((TypeVariable) type);
      }
      case DECLARED -> {
        DeclaredType declaredType = (DeclaredType) type;
        for (TypeMirror typeArg : declaredType.getTypeArguments()) {
          if (mentionsTypeVariable(typeArg, test)) {
            return true;
          }
        }
        return mentionsTypeVariable(declaredType.getEnclosingType(), test);
      }
      case ARRAY -> {
        return mentionsTypeVariable(((ArrayType) type).getComponentType(), test);
      }
      case WILDCARD -> {
        WildcardType wildcard = (WildcardType) type;
        TypeMirror extendsBound = wildcard.getExtendsBound();
        TypeMirror superBound = wildcard.getSuperBound();
        return (extendsBound != null && mentionsTypeVariable(extendsBound, test))
            || (superBound != null && mentionsTypeVariable(superBound, test));
      }
      case INTERSECTION -> {
        for (TypeMirror bound : ((IntersectionType) type).getBounds()) {
          if (mentionsTypeVariable(bound, test)) {
            return true;
          }
        }
        return false;
      }
      default -> {
        return false;
      }
    }
  }
}
