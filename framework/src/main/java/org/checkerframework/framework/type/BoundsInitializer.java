package org.checkerframework.framework.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;

/**
 * {@code BoundsInitializer} creates AnnotatedTypeMirrors (without annotations) for the bounds of
 * type variables and wildcards. It ensures that recursive type variables refer to themselves at the
 * correct location. Other kinds of composited types are lazily initialized in {@code
 * AnnotatedTypeMirror}.
 *
 * <p>Its static helper methods are called from AnnotatedTypeMirror. When an initializer method is
 * called for a particular bound, the entirety of that bound, including circular references, is
 * created.
 */
public class BoundsInitializer {

  /** Class cannot be instantiated. */
  private BoundsInitializer() {
    throw new AssertionError("Class BoundsInitializer cannot be instantiated.");
  }

  /**
   * Creates and sets the upper and lower bounds of {@code typeVar}.
   *
   * @param typeVar an {@link AnnotatedTypeVariable}
   */
  public static void initializeBounds(AnnotatedTypeVariable typeVar) {
    new BoundInitializerVisitor(typeVar.atypeFactory).initializeTypeVariable(typeVar);
  }

  /**
   * Creates and sets the extends and super bounds of {@code wildcard}.
   *
   * @param wildcard an {@link AnnotatedWildcardType}
   */
  public static void initializeBounds(AnnotatedWildcardType wildcard) {
    new BoundInitializerVisitor(wildcard.atypeFactory).initializeWildcard(wildcard);
  }

  /**
   * Returns a wildcard whose extends bound is the same as {@code typeVariable}'s upper bound. If
   * the upper bound is an intersection, then this method returns an unbound wildcard.
   *
   * @param typeVariable a type variable
   * @param types types util
   * @return a wildcard whose extends bound is the same as the upper bound of {@code typeVariable}
   */
  public static WildcardType getUpperBoundAsWildcard(TypeVariable typeVariable, Types types) {
    TypeMirror upperBound = typeVariable.getUpperBound();
    switch (upperBound.getKind()) {
      case ARRAY:
      case DECLARED:
      case TYPEVAR:
        return types.getWildcardType(upperBound, null);
      case INTERSECTION:
        // Can't create a wildcard with an intersection as an extends bound, so use
        // an unbound wildcard instead.
        return types.getWildcardType(null, null);
      default:
        throw new BugInCF(
            "Unexpected upper bound kind: %s type: %s", upperBound.getKind(), upperBound);
    }
  }

  /**
   * A class that visits all parts of a TypeMirror and creates an {@link AnnotatedTypeMirror} to
   * match the TypeMirror. This visitor is only used to initialize recursive type variables or
   * wildcards, because at some point instead of creating a new type, a previously created type is
   * returned. This makes the {@code AnnotatedTypeMirror} recursive.
   */
  private static class BoundInitializerVisitor implements TypeVisitor<AnnotatedTypeMirror, Void> {

    /** AnnotatedTypeFactory used to create AnnotatedTypeMirrors. */
    private final AnnotatedTypeFactory atypeFactory;

    /**
     * A map from a Java type variable to its {@link AnnotatedTypeVariable}. Used to set up
     * recursive type variables.
     */
    private final Map<TypeVariable, AnnotatedTypeVariable> typeVarToAtm = new HashMap<>();

    /**
     * A map from a Java wildcard to its {@link AnnotatedWildcardType}. Used to set up recursive
     * wildcards.
     */
    private final Map<WildcardType, AnnotatedWildcardType> wildcardToAtm = new HashMap<>();

    /**
     * A map from a Java type variable in a raw type to an {@link AnnotatedWildcardType}. Used to
     * set up recursive type variables.
     */
    private final Map<TypeVariable, AnnotatedWildcardType> typeParamToWildcard = new HashMap<>();

    /**
     * Creates a {@link BoundInitializerVisitor}.
     *
     * @param atypeFactory the type factory
     */
    public BoundInitializerVisitor(AnnotatedTypeFactory atypeFactory) {
      this.atypeFactory = atypeFactory;
    }

    /**
     * Creates a {@link AnnotatedTypeMirror} with the same structure as {@code javaType}.
     *
     * @param javaType a Java type
     * @return a new {@link AnnotatedTypeMirror} with the same structure as {@code javaType}
     */
    private AnnotatedTypeMirror createAnnotatedType(TypeMirror javaType) {
      return AnnotatedTypeMirror.createType(javaType, atypeFactory, false);
    }

    /**
     * Sets the upper and lower bounds of {@code annotatedTypeVariable} by visiting each bound. This
     * method should only be called once per {@link TypeVariable}.
     *
     * @param annotatedTypeVariable an annotated type variable
     */
    private void initializeTypeVariable(AnnotatedTypeVariable annotatedTypeVariable) {
      TypeVariable t = annotatedTypeVariable.getUnderlyingType();
      if (!annotatedTypeVariable.isDeclaration()) {
        t = (TypeVariable) TypeAnnotationUtils.unannotatedType(t);
        typeVarToAtm.put(t, annotatedTypeVariable);
      }

      TypeMirror lowerBound = TypesUtils.getTypeVariableLowerBound(t, atypeFactory.processingEnv);
      annotatedTypeVariable.setLowerBound(visit(lowerBound));
      annotatedTypeVariable.setUpperBound(visit(t.getUpperBound()));
    }

    /**
     * Sets the extends and super bounds of {@code annotatedWildcardType} by calling visiting each
     * bound. This method should only be called once per {@link WildcardType}.
     *
     * @param annotatedWildcardType an annotated wildcard type
     */
    private void initializeWildcard(AnnotatedWildcardType annotatedWildcardType) {
      WildcardType t = annotatedWildcardType.getUnderlyingType();
      wildcardToAtm.put(t, annotatedWildcardType);

      TypeMirror lowerBound = TypesUtils.wildLowerBound(t, atypeFactory.processingEnv);
      annotatedWildcardType.setSuperBound(visit(lowerBound));
      TypeMirror upperBound = t.getExtendsBound();
      if (upperBound == null) {
        upperBound = TypesUtils.getObjectTypeMirror(atypeFactory.processingEnv);
      }
      annotatedWildcardType.setExtendsBound(visit(upperBound));
    }

    @Override
    public AnnotatedTypeMirror visit(TypeMirror t, Void unused) {
      return t.accept(this, null);
    }

    @Override
    public AnnotatedTypeMirror visitPrimitive(PrimitiveType t, Void unused) {
      return createAnnotatedType(t);
    }

    @Override
    public AnnotatedTypeMirror visitNull(NullType t, Void unused) {
      return createAnnotatedType(t);
    }

    @Override
    public AnnotatedTypeMirror visitArray(ArrayType t, Void unused) {
      AnnotatedArrayType annotatedArrayType = (AnnotatedArrayType) createAnnotatedType(t);
      annotatedArrayType.setComponentType(visit(t.getComponentType()));
      return annotatedArrayType;
    }

    @Override
    public AnnotatedTypeMirror visitDeclared(DeclaredType t, Void unused) {
      AnnotatedDeclaredType annotatedDeclaredType = (AnnotatedDeclaredType) createAnnotatedType(t);
      if (t.getEnclosingType() != null && t.getEnclosingType().getKind() == TypeKind.DECLARED) {
        annotatedDeclaredType.setEnclosingType((AnnotatedDeclaredType) visit(t.getEnclosingType()));
      }

      TypeElement typeElement = (TypeElement) atypeFactory.types.asElement(t);
      List<AnnotatedTypeMirror> typeArgs = new ArrayList<>(typeElement.getTypeParameters().size());
      if (annotatedDeclaredType.isUnderlyingTypeRaw()) {
        for (TypeParameterElement typeParameterEle : typeElement.getTypeParameters()) {
          TypeVariable typeVar = (TypeVariable) typeParameterEle.asType();
          AnnotatedWildcardType wildcardType = typeParamToWildcard.get(typeVar);
          if (wildcardType == null) {
            TypeMirror javaTypeArg = getUpperBoundAsWildcard(typeVar, atypeFactory.types);
            wildcardType = (AnnotatedWildcardType) createAnnotatedType(javaTypeArg);
            wildcardType.setTypeArgOfRawType();
            typeParamToWildcard.put(typeVar, wildcardType);
            initializeWildcard(wildcardType);
          }
          typeArgs.add(wildcardType);
        }
      } else {
        for (TypeMirror javaTypeArg : t.getTypeArguments()) {
          typeArgs.add(visit(javaTypeArg));
        }
      }

      annotatedDeclaredType.setTypeArguments(typeArgs);
      return annotatedDeclaredType;
    }

    @Override
    public AnnotatedTypeMirror visitTypeVariable(TypeVariable t, Void unused) {
      t = (TypeVariable) TypeAnnotationUtils.unannotatedType(t);
      AnnotatedTypeVariable annotatedTypeVariable = typeVarToAtm.get(t);
      if (annotatedTypeVariable != null) {
        return annotatedTypeVariable;
      }

      annotatedTypeVariable = (AnnotatedTypeVariable) createAnnotatedType(t);
      initializeTypeVariable(annotatedTypeVariable);

      return annotatedTypeVariable;
    }

    @Override
    public AnnotatedTypeMirror visitWildcard(WildcardType t, Void unused) {
      AnnotatedWildcardType annotatedWildcardType = wildcardToAtm.get(t);
      if (annotatedWildcardType != null) {
        return annotatedWildcardType;
      }
      annotatedWildcardType = (AnnotatedWildcardType) createAnnotatedType(t);
      initializeWildcard(annotatedWildcardType);

      return annotatedWildcardType;
    }

    @Override
    public AnnotatedTypeMirror visitExecutable(ExecutableType t, Void unused) {
      throw new RuntimeException("Don't do this");
    }

    @Override
    public AnnotatedTypeMirror visitNoType(NoType t, Void unused) {
      return createAnnotatedType(t);
    }

    @Override
    public AnnotatedTypeMirror visitUnion(UnionType t, Void unused) {
      AnnotatedUnionType annotatedUnionType = (AnnotatedUnionType) createAnnotatedType(t);

      annotatedUnionType.alternatives =
          CollectionsPlume.mapList(
              alternative -> (AnnotatedDeclaredType) visit(alternative), t.getAlternatives());
      return annotatedUnionType;
    }

    @Override
    public AnnotatedTypeMirror visitIntersection(IntersectionType t, Void unused) {
      AnnotatedIntersectionType annotatedIntersectionType =
          (AnnotatedIntersectionType) createAnnotatedType(t);
      annotatedIntersectionType.bounds = CollectionsPlume.mapList(this::visit, t.getBounds());
      return annotatedIntersectionType;
    }

    @Override
    public AnnotatedTypeMirror visitError(ErrorType t, Void unused) {
      return createAnnotatedType(t);
    }

    @Override
    public AnnotatedTypeMirror visitUnknown(TypeMirror t, Void unused) {
      return createAnnotatedType(t);
    }
  }
}
