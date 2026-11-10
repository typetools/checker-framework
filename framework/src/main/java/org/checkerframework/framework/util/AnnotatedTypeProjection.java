package org.checkerframework.framework.util;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Computes the upward and downward projections of annotated types, as defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-4.html#jls-4.10.5">JLS 4.10.5</a>.
 * The restricted type variables are the captured type variables, including the fresh type variables
 * that type inference creates, except for any that are explicitly left unrestricted.
 */
public final class AnnotatedTypeProjection {

  /** The type factory. */
  private final AnnotatedTypeFactory atypeFactory;

  /** The type utilities. */
  private final Types types;

  /** The javac type utilities. */
  private final com.sun.tools.javac.code.Types javacTypes;

  /** The captured type variables that are not restricted. */
  private final Collection<TypeVariable> unrestricted;

  /**
   * The restricted type variables whose upward projection is being computed. Projecting one of
   * these again would not terminate, because its bound mentions itself.
   */
  private final List<TypeVariable> inProgress = new ArrayList<>();

  /**
   * Creates an {@code AnnotatedTypeProjection}.
   *
   * @param atypeFactory the type factory
   * @param unrestricted the captured type variables that are not restricted
   */
  private AnnotatedTypeProjection(
      AnnotatedTypeFactory atypeFactory, Collection<TypeVariable> unrestricted) {
    this.atypeFactory = atypeFactory;
    this.types = atypeFactory.types;
    this.javacTypes =
        com.sun.tools.javac.code.Types.instance(
            ((JavacProcessingEnvironment) atypeFactory.getProcessingEnv()).getContext());
    this.unrestricted = unrestricted;
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
    return upward(atypeFactory, type, Collections.emptyList());
  }

  /**
   * Returns the upward projection of {@code type} with respect to all captured type variables
   * except {@code unrestricted}: a supertype of {@code type} that does not mention any captured
   * type variable other than those in {@code unrestricted}.
   *
   * @param atypeFactory the type factory
   * @param type a type
   * @param unrestricted the captured type variables that are not restricted
   * @return the upward projection of {@code type}
   */
  public static AnnotatedTypeMirror upward(
      AnnotatedTypeFactory atypeFactory,
      AnnotatedTypeMirror type,
      Collection<TypeVariable> unrestricted) {
    return new AnnotatedTypeProjection(atypeFactory, unrestricted).upward(type);
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
          // The erasure of a type variable is the erasure of its leftmost bound (JLS 4.6).
          AnnotatedTypeMirror leftmostBound = typeVariable.getUpperBound();
          if (leftmostBound.getKind() == TypeKind.INTERSECTION) {
            leftmostBound = ((AnnotatedIntersectionType) leftmostBound).getBounds().get(0);
          }
          result = leftmostBound.getErased();
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
        AnnotatedDeclaredType enclosingType = declaredType.getEnclosingType();
        return withTypeArguments(
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
        return withTypeArguments(declaredType, projectedEnclosingType, typeArgs);
      }
      case ARRAY -> {
        AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
        AnnotatedTypeMirror componentType = downward(arrayType.getComponentType());
        return componentType == null ? null : withComponentType(arrayType, componentType);
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
      default -> {
        return null;
      }
    }
  }

  /**
   * Creates a wildcard. Its Java type is {@code ? super lower} if {@code lower} is non-null, {@code
   * ? extends upper} if {@code extendsUpper} is true and {@code upper} is not an intersection type,
   * and {@code ?} otherwise. Its annotated upper bound is {@code upper} if its Java type is {@code
   * ? extends upper}, and otherwise {@code Object} with the qualifiers of {@code upper}.
   *
   * @param upper the upper bound of the wildcard
   * @param lower the lower bound of the wildcard, or null if it has none
   * @param extendsUpper true if the Java type of the wildcard should be {@code ? extends upper};
   *     ignored if {@code lower} is non-null
   * @return a wildcard with the given bounds
   */
  private AnnotatedWildcardType createWildcard(
      AnnotatedTypeMirror upper, @Nullable AnnotatedTypeMirror lower, boolean extendsUpper) {
    // A wildcard's bound cannot be an intersection type, so `?` stands in for `? extends A & B`.
    boolean isExtendsUpper =
        lower == null && extendsUpper && upper.getKind() != TypeKind.INTERSECTION;
    TypeMirror javaType =
        TypesUtils.createWildcard(
            lower == null ? null : lower.getUnderlyingType(),
            isExtendsUpper ? upper.getUnderlyingType() : null,
            types);
    AnnotatedWildcardType wildcard =
        (AnnotatedWildcardType) AnnotatedTypeMirror.createType(javaType, atypeFactory, false);
    if (lower == null) {
      lower = AnnotatedTypeMirror.createType(types.getNullType(), atypeFactory, false);
      lower.addAnnotations(atypeFactory.getQualifierHierarchy().getBottomAnnotations());
    }
    if (isExtendsUpper) {
      wildcard.setExtendsBound(upper);
    } else {
      // Keep the qualifiers of `upper`, which are more precise than the declared bound's.
      AnnotatedTypeMirror object =
          AnnotatedTypeMirror.createType(
              TypesUtils.getObjectTypeMirror(atypeFactory.getProcessingEnv()), atypeFactory, false);
      object.addAnnotations(upper.getAnnotations());
      wildcard.setExtendsBound(object);
    }
    wildcard.setSuperBound(lower);
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
  private AnnotatedDeclaredType withTypeArguments(
      AnnotatedDeclaredType type,
      @Nullable AnnotatedDeclaredType enclosingType,
      List<AnnotatedTypeMirror> typeArgs) {
    DeclaredType javaType = type.getUnderlyingType();
    TypeMirror[] javaTypeArgs = new TypeMirror[typeArgs.size()];
    for (int i = 0; i < typeArgs.size(); i++) {
      javaTypeArgs[i] = typeArgs.get(i).getUnderlyingType();
    }
    TypeElement element = (TypeElement) javaType.asElement();
    DeclaredType newJavaType;
    if (javaType.getEnclosingType().getKind() == TypeKind.DECLARED) {
      DeclaredType javaEnclosingType =
          enclosingType == null
              ? (DeclaredType) javaType.getEnclosingType()
              : enclosingType.getUnderlyingType();
      newJavaType = types.getDeclaredType(javaEnclosingType, element, javaTypeArgs);
    } else {
      newJavaType = types.getDeclaredType(element, javaTypeArgs);
    }
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
  private boolean isInProgress(TypeVariable typeVariable) {
    for (TypeVariable t : inProgress) {
      if (TypesUtils.areSame(t, typeVariable)) {
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
    switch (type.getKind()) {
      case TYPEVAR -> {
        if (!TypesUtils.isCapturedTypeVariable(type)) {
          return false;
        }
        for (TypeVariable t : unrestricted) {
          if (TypesUtils.areSame(t, (TypeVariable) type)) {
            return false;
          }
        }
        return true;
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
        DeclaredType declaredType = (DeclaredType) type;
        for (TypeMirror typeArg : declaredType.getTypeArguments()) {
          if (mentionsTypeParameter(typeArg, element)) {
            return true;
          }
        }
        return mentionsTypeParameter(declaredType.getEnclosingType(), element);
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
