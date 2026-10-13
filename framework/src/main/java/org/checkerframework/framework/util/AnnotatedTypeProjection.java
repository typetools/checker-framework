package org.checkerframework.framework.util;

import java.util.ArrayList;
import java.util.List;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Computes the upward and downward projections of annotated types with respect to all captured type
 * variables, as defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.10.5">JLS 4.10.5</a>.
 * The restricted type variables are the captured type variables, including the fresh type variables
 * that type inference creates.
 */
public final class AnnotatedTypeProjection {

  /** The type factory. */
  private final AnnotatedTypeFactory atypeFactory;

  /** The type utilities. */
  private final Types types;

  /**
   * The restricted type variables whose upward projection is being computed. Projecting one of
   * these again would not terminate, because its bound mentions itself.
   */
  private final List<TypeVariable> inProgress = new ArrayList<>();

  /**
   * Creates an {@code AnnotatedTypeProjection}.
   *
   * @param atypeFactory the type factory
   */
  private AnnotatedTypeProjection(AnnotatedTypeFactory atypeFactory) {
    this.atypeFactory = atypeFactory;
    this.types = atypeFactory.types;
  }

  /**
   * Returns the upward projection of {@code type} with respect to all captured type variables: a
   * supertype of {@code type} that does not mention any captured type variable.
   *
   * @param atypeFactory the type factory
   * @param type a type
   * @return the upward projection of {@code type}
   */
  public static AnnotatedTypeMirror upward(
      AnnotatedTypeFactory atypeFactory, AnnotatedTypeMirror type) {
    return new AnnotatedTypeProjection(atypeFactory).upward(type);
  }

  /**
   * Returns the upward projection of {@code type}.
   *
   * @param type a type
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
        if (isInProgress(javaType)) {
          result = typeVariable.getUpperBound().getErased();
        } else {
          inProgress.add(javaType);
          result = upward(typeVariable.getUpperBound());
          inProgress.remove(inProgress.size() - 1);
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
        return withTypeArguments(declaredType, typeArgs);
      }
      case ARRAY -> {
        AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
        return withComponentType(arrayType, upward(arrayType.getComponentType()));
      }
      case INTERSECTION -> {
        // Approximate the projection of an intersection type by its erasure.
        return type.getErased();
      }
      default -> {
        return type.deepCopy();
      }
    }
  }

  /**
   * Returns the type argument of the upward projection of {@code type} at index {@code i}.
   *
   * @param type a parameterized type
   * @param i the index of a type argument of {@code type}
   * @return the type argument of the upward projection of {@code type} at index {@code i}
   */
  private AnnotatedTypeMirror upwardTypeArgument(AnnotatedDeclaredType type, int i) {
    AnnotatedTypeMirror typeArg = type.getTypeArguments().get(i);
    if (!mentionsRestricted(typeArg.getUnderlyingType())) {
      return typeArg.deepCopy();
    }
    if (typeArg.getKind() == TypeKind.WILDCARD) {
      AnnotatedWildcardType wildcard = (AnnotatedWildcardType) typeArg;
      AnnotatedTypeMirror upper = upward(wildcard.getExtendsBound());
      if (wildcard.getUnderlyingType().getSuperBound() == null) {
        return createWildcard(upper, null, true);
      }
      return createWildcard(upper, downward(wildcard.getSuperBound()), false);
    }

    AnnotatedTypeMirror upper = upward(typeArg);
    TypeElement element = (TypeElement) type.getUnderlyingType().asElement();
    TypeParameterElement typeParameter = element.getTypeParameters().get(i);
    TypeMirror declaredBound = ((TypeVariable) typeParameter.asType()).getUpperBound();
    if (!TypesUtils.isObject(upper.getUnderlyingType())
        && (mentionsTypeParameter(declaredBound, element)
            || !types.isSubtype(declaredBound, upper.getUnderlyingType()))) {
      return createWildcard(upper, null, true);
    }
    return createWildcard(upper, downward(typeArg), false);
  }

  /**
   * Returns the downward projection of {@code type} with respect to all captured type variables, or
   * null if it is undefined.
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
        List<AnnotatedTypeMirror> typeArgs = new ArrayList<>();
        for (AnnotatedTypeMirror typeArg : declaredType.getTypeArguments()) {
          if (!mentionsRestricted(typeArg.getUnderlyingType())) {
            typeArgs.add(typeArg.deepCopy());
          } else if (typeArg.getKind() != TypeKind.WILDCARD) {
            return null;
          } else {
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) typeArg;
            if (wildcard.getUnderlyingType().getSuperBound() == null) {
              AnnotatedTypeMirror upper = downward(wildcard.getExtendsBound());
              if (upper == null) {
                return null;
              }
              typeArgs.add(createWildcard(upper, null, true));
            } else {
              typeArgs.add(
                  createWildcard(
                      upward(wildcard.getExtendsBound()), upward(wildcard.getSuperBound()), false));
            }
          }
        }
        return withTypeArguments(declaredType, typeArgs);
      }
      case ARRAY -> {
        AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
        AnnotatedTypeMirror componentType = downward(arrayType.getComponentType());
        return componentType == null ? null : withComponentType(arrayType, componentType);
      }
      default -> {
        return null;
      }
    }
  }

  /**
   * Creates a wildcard whose annotated upper bound is {@code upper}. Its Java type is {@code ?
   * super lower} if {@code lower} is non-null, {@code ? extends upper} if {@code extendsUpper} is
   * true, and {@code ?} otherwise. The annotated upper bound of a wildcard whose Java type is not
   * {@code ? extends upper} is still {@code upper}, so that its qualifiers are not lost.
   *
   * @param upper the annotated upper bound of the wildcard
   * @param lower the lower bound of the wildcard, or null if it has none
   * @param extendsUpper true if the Java type of the wildcard is {@code ? extends upper}; ignored
   *     if {@code lower} is non-null
   * @return a wildcard with the given bounds
   */
  private AnnotatedWildcardType createWildcard(
      AnnotatedTypeMirror upper, @Nullable AnnotatedTypeMirror lower, boolean extendsUpper) {
    TypeMirror javaType =
        TypesUtils.createWildcard(
            lower == null ? null : lower.getUnderlyingType(),
            lower == null && extendsUpper ? upper.getUnderlyingType() : null,
            types);
    AnnotatedWildcardType wildcard =
        (AnnotatedWildcardType) AnnotatedTypeMirror.createType(javaType, atypeFactory, false);
    if (lower == null) {
      lower = AnnotatedTypeMirror.createType(types.getNullType(), atypeFactory, false);
      lower.addAnnotations(atypeFactory.getQualifierHierarchy().getBottomAnnotations());
    }
    wildcard.setExtendsBound(upper);
    wildcard.setSuperBound(lower);
    return wildcard;
  }

  /**
   * Returns a copy of {@code type} whose type arguments are {@code typeArgs}.
   *
   * @param type a declared type
   * @param typeArgs the new type arguments
   * @return a copy of {@code type} whose type arguments are {@code typeArgs}
   */
  private AnnotatedDeclaredType withTypeArguments(
      AnnotatedDeclaredType type, List<AnnotatedTypeMirror> typeArgs) {
    DeclaredType javaType = type.getUnderlyingType();
    TypeMirror[] javaTypeArgs = new TypeMirror[typeArgs.size()];
    for (int i = 0; i < typeArgs.size(); i++) {
      javaTypeArgs[i] = typeArgs.get(i).getUnderlyingType();
    }
    TypeElement element = (TypeElement) javaType.asElement();
    DeclaredType newJavaType =
        javaType.getEnclosingType().getKind() == TypeKind.DECLARED
            ? types.getDeclaredType(
                (DeclaredType) javaType.getEnclosingType(), element, javaTypeArgs)
            : types.getDeclaredType(element, javaTypeArgs);
    AnnotatedDeclaredType result =
        (AnnotatedDeclaredType) AnnotatedTypeMirror.createType(newJavaType, atypeFactory, false);
    result.addAnnotations(type.getPrimaryAnnotations());
    result.setTypeArguments(typeArgs);
    AnnotatedDeclaredType enclosingType = type.getEnclosingType();
    if (enclosingType != null) {
      result.setEnclosingType(enclosingType.deepCopy());
    }
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
  private boolean isInProgress(TypeVariable typeVariable) {
    for (TypeVariable t : inProgress) {
      if (TypesUtils.areSame(t, typeVariable)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if {@code type} mentions a captured type variable.
   *
   * @param type a type
   * @return true if {@code type} mentions a captured type variable
   */
  private static boolean mentionsRestricted(TypeMirror type) {
    switch (type.getKind()) {
      case TYPEVAR -> {
        return TypesUtils.isCapturedTypeVariable(type);
      }
      case DECLARED -> {
        DeclaredType declaredType = (DeclaredType) type;
        for (TypeMirror typeArg : declaredType.getTypeArguments()) {
          if (mentionsRestricted(typeArg)) {
            return true;
          }
        }
        return mentionsRestricted(declaredType.getEnclosingType());
      }
      case ARRAY -> {
        return mentionsRestricted(((ArrayType) type).getComponentType());
      }
      case WILDCARD -> {
        WildcardType wildcard = (WildcardType) type;
        return (wildcard.getExtendsBound() != null
                && mentionsRestricted(wildcard.getExtendsBound()))
            || (wildcard.getSuperBound() != null && mentionsRestricted(wildcard.getSuperBound()));
      }
      case INTERSECTION -> {
        for (TypeMirror bound : ((IntersectionType) type).getBounds()) {
          if (mentionsRestricted(bound)) {
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

  /**
   * Returns true if {@code type} mentions a type parameter of {@code element}.
   *
   * @param type a type
   * @param element a type declaration
   * @return true if {@code type} mentions a type parameter of {@code element}
   */
  private static boolean mentionsTypeParameter(TypeMirror type, TypeElement element) {
    switch (type.getKind()) {
      case TYPEVAR -> {
        return ((TypeVariable) type).asElement().getEnclosingElement().equals(element);
      }
      case DECLARED -> {
        for (TypeMirror typeArg : ((DeclaredType) type).getTypeArguments()) {
          if (mentionsTypeParameter(typeArg, element)) {
            return true;
          }
        }
        return false;
      }
      case ARRAY -> {
        return mentionsTypeParameter(((ArrayType) type).getComponentType(), element);
      }
      case WILDCARD -> {
        WildcardType wildcard = (WildcardType) type;
        return (wildcard.getExtendsBound() != null
                && mentionsTypeParameter(wildcard.getExtendsBound(), element))
            || (wildcard.getSuperBound() != null
                && mentionsTypeParameter(wildcard.getSuperBound(), element));
      }
      case INTERSECTION -> {
        for (TypeMirror bound : ((IntersectionType) type).getBounds()) {
          if (mentionsTypeParameter(bound, element)) {
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
