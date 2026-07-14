package org.checkerframework.dataflow.util;

import com.sun.source.tree.MethodTree;
import java.util.EnumSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A utility class for working with the {@link SideEffectFree}, {@link Deterministic}, and {@link
 * Pure} annotations.
 *
 * @see SideEffectFree
 * @see Deterministic
 * @see Pure
 */
public final class PurityUtils {

  /** Do not instantiate. */
  private PurityUtils() {
    throw new Error("Do not instantiate PurityUtils.");
  }

  /** Represents a method that is both deterministic and side-effect free. */
  private static final EnumSet<PurityKind> detAndSeFree =
      EnumSet.of(PurityKind.DETERMINISTIC, PurityKind.SIDE_EFFECT_FREE);

  /**
   * Does the method {@code methodTree} have any purity annotation?
   * (@Pure, @SideEffectFree, @SideEffectsOnly, @Deterministic.)
   *
   * @param provider how to get annotations
   * @param methodTree a method to test
   * @return true if the method has any purity annotations
   */
  public static boolean hasPurityAnnotation(AnnotationProvider provider, MethodTree methodTree) {
    return !getPurityKinds(provider, methodTree).isEmpty();
  }

  /**
   * Does the method {@code methodElement} have any purity annotation?
   *
   * <p>This method does not consider {@code @SideEffectsOnly} to be a purity annotation. Fixing
   * that bug requires refactoring.
   *
   * @param provider how to get annotations
   * @param methodElement a method to test
   * @return true if the method has any purity annotations
   */
  public static boolean hasPurityAnnotation(
      AnnotationProvider provider, ExecutableElement methodElement) {
    return !getPurityKinds(provider, methodElement).isEmpty();
  }

  /**
   * Is the method {@code methodTree} deterministic?
   *
   * @param provider how to get annotations
   * @param methodTree a method to test
   * @return true if the method is deterministic
   */
  public static boolean isDeterministic(AnnotationProvider provider, MethodTree methodTree) {
    ExecutableElement methodElement = TreeUtils.elementFromDeclaration(methodTree);
    if (methodElement == null) {
      throw new BugInCF("Could not find element for tree: " + methodTree);
    }
    return isDeterministic(provider, methodElement);
  }

  /**
   * Is the method {@code methodElement} deterministic?
   *
   * @param provider how to get annotations
   * @param methodElement a method to test
   * @return true if the method is deterministic
   */
  public static boolean isDeterministic(
      AnnotationProvider provider, ExecutableElement methodElement) {
    EnumSet<PurityKind> kinds = getPurityKinds(provider, methodElement);
    return kinds.contains(PurityKind.DETERMINISTIC);
  }

  /**
   * Is the method {@code methodElement} side-effect-free?
   *
   * <p>This method does not use, and has different semantics than, {@link
   * AnnotationProvider#isSideEffectFree}. This method is concerned only with standard purity
   * annotations.
   *
   * @param provider how to get annotations
   * @param methodElement a method to test
   * @return true if the method is side-effect-free
   */
  public static boolean isSideEffectFree(
      AnnotationProvider provider, ExecutableElement methodElement) {
    EnumSet<PurityKind> kinds = getPurityKinds(provider, methodElement);
    return kinds.contains(PurityKind.SIDE_EFFECT_FREE);
  }

  /**
   * Returns the purity annotations on the method {@code methodTree}.
   *
   * @param provider how to get annotations. Its {@link AnnotationProvider#isSideEffectFree} and
   *     {@link AnnotationProvider#isDeterministic} methods are not used.
   * @param methodTree a method to test
   * @return the types of purity of the method {@code methodTree}
   */
  public static EnumSet<PurityKind> getPurityKinds(
      AnnotationProvider provider, MethodTree methodTree) {
    ExecutableElement methodElement = TreeUtils.elementFromDeclaration(methodTree);
    if (methodElement == null) {
      throw new BugInCF("Could not find element for tree: " + methodTree);
    }
    return getPurityKinds(provider, methodElement);
  }

  /**
   * Returns the purity annotations on the method {@code methodElement}. {@code @Pure} is treated as
   * an alias for {@code @SideEffectFree} and {@code @Deterministic}.
   *
   * @param provider how to get annotations. Its {@link AnnotationProvider#isSideEffectFree} and
   *     {@link AnnotationProvider#isDeterministic} methods are not used.
   * @param methodElement a method to test
   * @return the types of purity of the method {@code methodElement}
   */
  public static EnumSet<PurityKind> getPurityKinds(
      AnnotationProvider provider, ExecutableElement methodElement) {
    // Special case for record accessors
    if (ElementUtils.isRecordAccessor(methodElement)
        && ElementUtils.isAutoGeneratedRecordMember(methodElement)) {
      return detAndSeFree;
    }

    AnnotationMirror pureAnnotation = provider.getDeclAnnotation(methodElement, Pure.class);
    if (pureAnnotation != null) {
      return detAndSeFree;
    }

    AnnotationMirror sefAnnotation =
        provider.getDeclAnnotation(methodElement, SideEffectFree.class);
    AnnotationMirror detAnnotation = provider.getDeclAnnotation(methodElement, Deterministic.class);

    if (sefAnnotation != null && detAnnotation != null) {
      return detAndSeFree;
    }

    EnumSet<PurityKind> result = EnumSet.noneOf(PurityKind.class);
    if (sefAnnotation != null) {
      result.add(PurityKind.SIDE_EFFECT_FREE);
    } else if (provider.getDeclAnnotation(methodElement, SideEffectsOnly.class) != null) {
      result.add(PurityKind.SIDE_EFFECTS_ONLY);
    }
    if (detAnnotation != null) {
      result.add(PurityKind.DETERMINISTIC);
    }
    return result;
  }
}
