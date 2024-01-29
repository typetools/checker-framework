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
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;

/**
 * BoundsInitializer creates AnnotatedTypeMirrors (without annotations) for the bounds of type
 * variables and wildcards. Its static helper methods are called from AnnotatedTypeMirror. When an
 * initializer method is called for a particular bound, the entirety of that bound, including
 * circular references, will be created.
 */
public class BoundsInitializer2 {
  public static void initializeBounds(AnnotatedTypeVariable typeVar) {
    AnnotationMirrorSet save = new AnnotationMirrorSet(typeVar.primaryAnnotations);
    new InitializerVisitor(typeVar.atypeFactory).initializeTypeVariable(typeVar);
    typeVar.addAnnotations(save);
  }

  public static void initializeBounds(AnnotatedWildcardType wildcard) {
    new InitializerVisitor(wildcard.atypeFactory).initializeWildcard(wildcard);
  }

  public static void initializeTypeArgs(
      AnnotatedDeclaredType annotatedDeclaredType, AnnotatedTypeFactory atypeFactory) {
    DeclaredType t = annotatedDeclaredType.getUnderlyingType();
    List<AnnotatedTypeMirror> typeArgs = new ArrayList<>(t.getTypeArguments().size());

    boolean rawJavaType = annotatedDeclaredType.isUnderlyingTypeRaw();
    if (rawJavaType) {
      TypeElement typeElement = (TypeElement) atypeFactory.types.asElement(t);
      Map<TypeVariable, AnnotatedTypeMirror> map = new HashMap<>();
      for (TypeParameterElement typeParameterEle : typeElement.getTypeParameters()) {
        TypeVariable typeVar = (TypeVariable) typeParameterEle.asType();
        TypeMirror wildcard = getUpperBoundAsWildcard(typeVar, atypeFactory);
        AnnotatedWildcardType atmWild =
            (AnnotatedWildcardType) AnnotatedTypeMirror.createType(wildcard, atypeFactory, false);
        atmWild.setTypeArgOfRawType();
        initializeBounds(atmWild);
        typeArgs.add(atmWild);
        map.put(typeVar, atmWild);
      }
      TypeVariableSubstitutor suber = atypeFactory.getTypeVarSubstitutor();
      for (AnnotatedTypeMirror atm : typeArgs) {
        AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) atm;
        wildcardType.setExtendsBound(
            suber.substituteWithoutCopyingTypeArguments(map, wildcardType.getExtendsBound()));
      }
    } else if (annotatedDeclaredType.isDeclaration()) {
      for (TypeMirror javaTypeArg : t.getTypeArguments()) {
        AnnotatedTypeVariable tv =
            (AnnotatedTypeVariable) AnnotatedTypeMirror.createType(javaTypeArg, atypeFactory, true);
        atypeFactory.initializeAtm(tv);
        typeArgs.add(tv);
      }
    } else {
      for (TypeMirror javaTypeArg : t.getTypeArguments()) {
        AnnotatedTypeMirror typeArg =
            AnnotatedTypeMirror.createType(javaTypeArg, atypeFactory, false);
        typeArgs.add(typeArg);
      }
    }
    annotatedDeclaredType.setTypeArguments(typeArgs);
  }

  /**
   * Returns a wildcard whose upper bound is the same as {@code typeVariable}. If the upper bound is
   * an intersection, then this method returns an unbound wildcard.
   */
  private static WildcardType getUpperBoundAsWildcard(
      TypeVariable typeVariable, AnnotatedTypeFactory factory) {
    TypeMirror upperBound = typeVariable.getUpperBound();
    switch (upperBound.getKind()) {
      case ARRAY:
      case DECLARED:
      case TYPEVAR:
        return factory.types.getWildcardType(upperBound, null);
      case INTERSECTION:
        // Can't create a wildcard with an intersection as the upper bound, so use
        // an unbound wildcard instead.  The extends bound of the
        // AnnotatedWildcardType will be initialized properly by this class.
        return factory.types.getWildcardType(null, null);
      default:
        throw new BugInCF(
            "Unexpected upper bound kind: %s type: %s", upperBound.getKind(), upperBound);
    }
  }

  static class InitializerVisitor implements TypeVisitor<AnnotatedTypeMirror, Void> {

    AnnotatedTypeFactory atypeFactory;

    Map<TypeVariable, AnnotatedTypeVariable> typeVariableMap;
    Map<WildcardType, AnnotatedWildcardType> wildcardMap;

    public InitializerVisitor(AnnotatedTypeFactory atypeFactory) {
      this.atypeFactory = atypeFactory;
      typeVariableMap = new HashMap<>();
      wildcardMap = new HashMap<>();
    }

    AnnotatedTypeMirror createAnnotatedType(TypeMirror javaType) {
      return AnnotatedTypeMirror.createType(javaType, atypeFactory, false);
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

      initializeTypeArguments(annotatedDeclaredType);

      return annotatedDeclaredType;
    }

    Map<TypeVariable, AnnotatedWildcardType> rawTypeMap = new HashMap<>();

    private void initializeTypeArguments(AnnotatedDeclaredType annotatedDeclaredType) {
      DeclaredType t = annotatedDeclaredType.getUnderlyingType();
      TypeElement typeElement = (TypeElement) atypeFactory.types.asElement(t);
      List<AnnotatedTypeMirror> typeArgs = new ArrayList<>(typeElement.getTypeParameters().size());
      if (annotatedDeclaredType.isUnderlyingTypeRaw()) {
        for (TypeParameterElement typeParameterEle : typeElement.getTypeParameters()) {
          TypeVariable typeVar = (TypeVariable) typeParameterEle.asType();
          AnnotatedWildcardType wildcardType = rawTypeMap.get(typeVar);
          if (wildcardType == null) {
            TypeMirror javaTypeArg = getUpperBoundAsWildcard(typeVar, atypeFactory);
            wildcardType = (AnnotatedWildcardType) createAnnotatedType(javaTypeArg);
            wildcardType.setTypeArgOfRawType();
            rawTypeMap.put(typeVar, wildcardType);
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
    }

    @Override
    public AnnotatedTypeMirror visitTypeVariable(TypeVariable t, Void unused) {
      t = (TypeVariable) TypeAnnotationUtils.unannotatedType(t);
      AnnotatedTypeVariable annotatedTypeVariable = typeVariableMap.get(t);
      if (annotatedTypeVariable != null) {
        return annotatedTypeVariable;
      }

      annotatedTypeVariable = (AnnotatedTypeVariable) createAnnotatedType(t);
      initializeTypeVariable(annotatedTypeVariable);

      return annotatedTypeVariable;
    }

    private void initializeTypeVariable(AnnotatedTypeVariable annotatedTypeVariable) {
      TypeVariable t = annotatedTypeVariable.getUnderlyingType();
      if (!annotatedTypeVariable.isDeclaration()) {
        t = (TypeVariable) TypeAnnotationUtils.unannotatedType(t);
        typeVariableMap.put(t, annotatedTypeVariable);
      }

      TypeMirror lowerBound = TypesUtils.getTypeVariableLowerBound(t, atypeFactory.processingEnv);
      annotatedTypeVariable.setLowerBound(visit(lowerBound));
      annotatedTypeVariable.setUpperBound(visit(t.getUpperBound()));
    }

    @Override
    public AnnotatedTypeMirror visitWildcard(WildcardType t, Void unused) {
      AnnotatedWildcardType annotatedWildcardType = wildcardMap.get(t);
      if (annotatedWildcardType != null) {
        return annotatedWildcardType;
      }
      annotatedWildcardType = (AnnotatedWildcardType) createAnnotatedType(t);
      initializeWildcard(annotatedWildcardType);

      return annotatedWildcardType;
    }

    private void initializeWildcard(AnnotatedWildcardType annotatedWildcardType) {
      WildcardType t = annotatedWildcardType.getUnderlyingType();
      wildcardMap.put(t, annotatedWildcardType);

      TypeMirror lowerBound = TypesUtils.wildLowerBound(t, atypeFactory.processingEnv);
      annotatedWildcardType.setSuperBound(visit(lowerBound));
      TypeMirror upperBound = t.getExtendsBound();
      if (upperBound == null) {
        upperBound = TypesUtils.getObjectTypeMirror(atypeFactory.processingEnv);
      }
      annotatedWildcardType.setExtendsBound(visit(upperBound));
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
