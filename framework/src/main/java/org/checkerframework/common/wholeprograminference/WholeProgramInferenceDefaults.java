package org.checkerframework.common.wholeprograminference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Determines which inferred qualifiers are redundant: a qualifier is redundant at a location if the
 * location has that qualifier even when no annotation is written there. Whole-program inference
 * omits redundant qualifiers from the files that it writes.
 */
/*package-private*/ final class WholeProgramInferenceDefaults {

  /** The type factory whose qualifiers and defaults are used. */
  private final AnnotatedTypeFactory atypeFactory;

  /**
   * Maps a declaration to the type that it has when no annotation is written on it, or to null if
   * that type is not known. The defaults do not change during a compilation, so each type is
   * computed only once.
   */
  private final Map<Element, @Nullable AnnotatedTypeMirror> defaultedTypes = new HashMap<>();

  /**
   * Creates a new WholeProgramInferenceDefaults.
   *
   * @param atypeFactory the type factory whose qualifiers and defaults are used
   */
  /*package-private*/ WholeProgramInferenceDefaults(AnnotatedTypeFactory atypeFactory) {
    this.atypeFactory = atypeFactory;
  }

  /**
   * Returns the type that {@code elt} has when no annotation is written on it: its Java type, with
   * the annotations that the type factory computes for it, such as the defaults that
   * {@code @DefaultQualifier} and {@code @DefaultFor} annotations specify. The result must not be
   * side-effected.
   *
   * <p>Returns null if conservative defaults for unchecked code apply to {@code elt}. Whole-program
   * inference annotates the files that it writes as checked code (for example, it writes
   * {@code @AnnotatedFor} in an ajava file), so different defaults apply when the files are read.
   *
   * @param elt a field, method, constructor, or formal parameter
   * @return the type that {@code elt} has when no annotation is written on it, or null if that type
   *     is not known
   */
  /*package-private*/ @Nullable AnnotatedTypeMirror defaultedType(Element elt) {
    if (defaultedTypes.containsKey(elt)) {
      return defaultedTypes.get(elt);
    }
    AnnotatedTypeMirror result;
    if (atypeFactory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?> gatf
        && gatf.applyConservativeDefaults(elt)) {
      result = null;
    } else {
      result = AnnotatedTypeMirror.createType(elt.asType(), atypeFactory, false);
      if (result instanceof AnnotatedExecutableType methodType) {
        methodType.setElement((ExecutableElement) elt);
      }
      atypeFactory.addComputedTypeAnnotations(elt, result);
    }
    defaultedTypes.put(elt, result);
    return result;
  }

  /**
   * Returns the type that the return type of {@code methodElt} has when no annotation is written on
   * it. The result must not be side-effected.
   *
   * @param methodElt a method
   * @return the type that the return type of {@code methodElt} has when no annotation is written on
   *     it, or null if that type is not known
   */
  /*package-private*/ @Nullable AnnotatedTypeMirror defaultedReturnType(
      ExecutableElement methodElt) {
    AnnotatedExecutableType methodType = (AnnotatedExecutableType) defaultedType(methodElt);
    return methodType == null ? null : methodType.getReturnType();
  }

  /**
   * Returns the type that the receiver of {@code methodElt} has when no annotation is written on
   * it. The result must not be side-effected.
   *
   * @param methodElt a method or constructor
   * @return the type that the receiver of {@code methodElt} has when no annotation is written on
   *     it, or null if that type is not known or {@code methodElt} has no receiver
   */
  /*package-private*/ @Nullable AnnotatedTypeMirror defaultedReceiverType(
      ExecutableElement methodElt) {
    AnnotatedExecutableType methodType = (AnnotatedExecutableType) defaultedType(methodElt);
    return methodType == null ? null : methodType.getReceiverType();
  }

  /**
   * Returns true if writing {@code anno} at a location would be redundant, because the location has
   * that qualifier even when no annotation is written there.
   *
   * <p>Returns false if {@code atm} is a type variable or a wildcard. A primary annotation on a use
   * of a type variable or wildcard overrides its bounds, so it is never redundant.
   *
   * @param anno a qualifier that whole-program inference inferred for a location
   * @param atm the inferred type of the location, whose primary annotations include {@code anno}
   * @param defaulted the type that the location has when no annotation is written there, or null if
   *     that type is not known
   * @return true if writing {@code anno} at the location would be redundant
   */
  /*package-private*/ boolean isRedundant(
      AnnotationMirror anno, AnnotatedTypeMirror atm, @Nullable AnnotatedTypeMirror defaulted) {
    if (defaulted == null
        || atm.getKind() == TypeKind.TYPEVAR
        || atm.getKind() == TypeKind.WILDCARD) {
      return false;
    }
    AnnotationMirror defaultedAnno = defaulted.getPrimaryAnnotationInHierarchy(anno);
    return defaultedAnno != null && AnnotationUtils.areSame(anno, defaultedAnno);
  }

  /**
   * Removes from {@code atm} each primary annotation that would be redundant (see {@link
   * #isRedundant}).
   *
   * @param atm the inferred type of a location, which this method side-effects; or null
   * @param defaulted the type that the location has when no annotation is written there, or null if
   *     that type is not known
   */
  /*package-private*/ void removeRedundantPrimaryAnnotations(
      @Nullable AnnotatedTypeMirror atm, @Nullable AnnotatedTypeMirror defaulted) {
    if (atm == null) {
      return;
    }
    for (AnnotationMirror anno : new ArrayList<>(atm.getPrimaryAnnotations())) {
      if (isRedundant(anno, atm, defaulted)) {
        atm.removePrimaryAnnotation(anno);
      }
    }
  }

  /**
   * Returns true if {@code anno} is an alias for a type qualifier of the type factory. Writing such
   * a declaration annotation on an element gives the element's type the aliased type qualifier, so
   * none of the element's type annotations is redundant, even one that the element's type has when
   * no annotation is written on it.
   *
   * @param anno a declaration annotation that whole-program inference has inferred
   * @return true if {@code anno} is an alias for a type qualifier
   */
  /*package-private*/ boolean isAliasForTypeQualifier(AnnotationMirror anno) {
    AnnotationMirror canonical = atypeFactory.canonicalAnnotation(anno);
    return !AnnotationUtils.areSameByName(canonical, anno)
        && atypeFactory.isSupportedQualifier(canonical);
  }
}
