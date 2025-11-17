package org.checkerframework.framework.type.typeannotator;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

/**
 * {@link PropagationTypeAnnotator} adds qualifiers to types where the qualifier to add should be
 * transferred from one or more other types.
 *
 * <p>At the moment, the only function PropagationTypeAnnotator provides, is the propagation of
 * generic type parameter annotations to unannotated wildcards with missing bounds annotations.
 *
 * @see
 *     #visitWildcard(org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType,
 *     Object)
 *     <p>PropagationTypeAnnotator traverses trees deeply by default.
 */
public class PropagationTypeAnnotator extends TypeAnnotator {

  /**
   * The PropagationTypeAnnotator is called recursively via
   * TypeAnnotatorUtil.eraseBoundsThenAnnotate. This flag prevents infinite recursion.
   */
  private boolean pause = false;

  /** The parents. */
  private final ArrayDeque<AnnotatedDeclaredType> parents = new ArrayDeque<>();

  /**
   * Creates a new PropagationTypeAnnotator.
   *
   * @param typeFactory the type factory
   */
  public PropagationTypeAnnotator(AnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public void reset() {
    if (!pause) {
      // when the PropagationTypeAnnotator is called recursively we don't
      // want the visit method to reset the list of visited types
      super.reset();
    }
  }

  /*
   * When pause == true, the PropagationTypeAnnotator caused a recursive call
   * and there is no need to execute the PropagationTypeAnnotator
   */
  @Override
  protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
    if (pause) {
      return null;
    }

    return super.scan(type, aVoid);
  }

  /**
   * Sometimes the underlying type parameters of AnnotatedWildcardTypes are not available on the
   * wildcards themselves. Instead, record enclosing class to find the type parameter to use as a
   * backup in visitWildcards.
   *
   * @param declaredType type to record
   */
  @Override
  public Void visitDeclared(AnnotatedDeclaredType declaredType, Void aVoid) {
    if (pause) {
      return null;
    }
    if (declaredType.isUnderlyingTypeRaw()) {
      // Copy annotations from the declaration to the wildcards.
      AnnotatedDeclaredType declaration =
          (AnnotatedDeclaredType)
              atypeFactory.fromElement(declaredType.getUnderlyingType().asElement());
      List<AnnotatedTypeMirror> typeArgs = declaredType.getTypeArguments();
      for (int i = 0; i < typeArgs.size(); i++) {
        if (!AnnotatedTypes.isTypeArgOfRawType(typeArgs.get(i))) {
          // Sometimes the framework infers a more precise type argument, so just use it.
          continue;
        }
        AnnotatedTypeVariable typeParam =
            (AnnotatedTypeVariable) declaration.getTypeArguments().get(i);
        AnnotatedWildcardType wct = (AnnotatedWildcardType) typeArgs.get(i);
        wct.getExtendsBound().replaceAnnotations(typeParam.getUpperBound().getPrimaryAnnotations());
        wct.getSuperBound().replaceAnnotations(typeParam.getLowerBound().getPrimaryAnnotations());
        wct.replaceAnnotations(typeParam.getPrimaryAnnotations());
      }
    }

    parents.addFirst(declaredType);
    super.visitDeclared(declaredType, aVoid);
    parents.removeFirst();
    return null;
  }

  /**
   * Rather than defaulting the missing bounds of a wildcard, find the bound annotations on the type
   * parameter it replaced. Place those annotations on the wildcard.
   *
   * @param wildcard type to annotate
   */
  @Override
  public Void visitWildcard(AnnotatedWildcardType wildcard, Void aVoid) {
    if (visitedNodes.containsKey(wildcard) || pause) {
      return null;
    }
    visitedNodes.put(wildcard, null);

    Element typeParamElement = TypesUtils.wildcardToTypeParam(wildcard.getUnderlyingType());
    if (typeParamElement == null && !parents.isEmpty()) {
      typeParamElement = getTypeParameterElement(wildcard, parents.peekFirst());
    }

    if (typeParamElement != null) {
      pause = true;
      AnnotatedTypeVariable typeParam =
          (AnnotatedTypeVariable) atypeFactory.getAnnotatedType(typeParamElement);
      pause = false;

      AnnotationMirrorSet tops = atypeFactory.getQualifierHierarchy().getTopAnnotations();

      if (AnnotatedTypes.hasNoExplicitBound(wildcard)) {
        propagateExtendsBound(wildcard, typeParam, tops);
        propagateSuperBound(wildcard, typeParam, tops);
      } else if (AnnotatedTypes.hasExplicitExtendsBound(wildcard)) {
        propagateSuperBound(wildcard, typeParam, tops);
      } else if (AnnotatedTypes.hasExplicitSuperBound(wildcard)) {
        propagateExtendsBound(wildcard, typeParam, tops);
      } else {
        // If this is thrown, then it means that there's a bug in one of the
        // AnnotatedTypes.hasNoExplicit*Bound methods.  Probably something changed in the
        // javac implementation.
        throw new BugInCF("Wildcard is neither unbound nor does it have an explicit bound.");
      }
    }
    scan(wildcard.getExtendsBound(), null);
    scan(wildcard.getSuperBound(), null);
    return null;
  }

  private void propagateSuperBound(
      AnnotatedWildcardType wildcard,
      AnnotatedTypeVariable typeParam,
      Set<? extends AnnotationMirror> tops) {
    AnnotatedTypeMirror typeParamBound = typeParam.getLowerBound();
    while (typeParamBound.getKind() == TypeKind.TYPEVAR) {
      typeParamBound = ((AnnotatedTypeVariable) typeParamBound).getLowerBound();
    }
    applyAnnosFromBound(wildcard.getSuperBound(), typeParamBound, tops);
  }

  private void propagateExtendsBound(
      AnnotatedWildcardType wildcard,
      AnnotatedTypeVariable typeParam,
      Set<? extends AnnotationMirror> tops) {
    AnnotatedTypeMirror typeParamBound = typeParam.getUpperBound();
    while (typeParamBound.getKind() == TypeKind.TYPEVAR) {
      typeParamBound = ((AnnotatedTypeVariable) typeParamBound).getUpperBound();
    }
    applyAnnosFromBound(wildcard.getExtendsBound(), typeParamBound, tops);
  }

  /**
   * Take the primary annotations from typeParamBound and place them as primary annotations on
   * wildcard bound.
   */
  private void applyAnnosFromBound(
      AnnotatedTypeMirror wildcardBound,
      AnnotatedTypeMirror typeParamBound,
      Set<? extends AnnotationMirror> tops) {
    // Type variables do not need primary annotations.
    // The type variable will have annotations placed on its
    // bounds via its declaration or defaulting rules
    if (wildcardBound.getKind() == TypeKind.TYPEVAR) {
      return;
    }

    for (AnnotationMirror top : tops) {
      if (wildcardBound.getPrimaryAnnotationInHierarchy(top) == null) {
        AnnotationMirror typeParamAnno = typeParamBound.getPrimaryAnnotationInHierarchy(top);
        if (typeParamAnno == null) {
          throw new BugInCF(
              StringsPlume.joinLines(
                  "Missing annotation on type parameter",
                  "top=" + top,
                  "wildcardBound=" + wildcardBound,
                  "typeParamBound=" + typeParamBound));
        } // else
        wildcardBound.addAnnotation(typeParamAnno);
      }
    }
  }

  /**
   * Search {@code declaredType}'s type arguments for {@code typeArg}. Using the index of {@code
   * typeArg}, find the corresponding type parameter element and return it.
   *
   * @param typeArg a typeArg of {@code declaredType}
   * @param declaredType the type in which {@code typeArg} is a type argument
   * @return the type parameter in {@code declaredType} that corresponds to {@code typeArg}
   */
  private Element getTypeParameterElement(
      @FindDistinct AnnotatedTypeMirror typeArg, AnnotatedDeclaredType declaredType) {
    for (int i = 0; i < declaredType.getTypeArguments().size(); i++) {
      if (declaredType.getTypeArguments().get(i) == typeArg) {
        TypeElement typeElement = TypesUtils.getTypeElement(declaredType.getUnderlyingType());
        return typeElement.getTypeParameters().get(i);
      }
    }
    throw new BugInCF("Wildcard %s is not a type argument of %s", typeArg, declaredType);
  }
}
