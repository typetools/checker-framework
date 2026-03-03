package org.checkerframework.framework.type;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.DoubleAnnotatedTypeScanner;
import org.checkerframework.javacutil.BugInCF;

/**
 * Replaces or adds all the annotations in the parameter with the annotations from the visited type.
 * An annotation is replaced if the parameter type already has an annotation in the same hierarchy
 * at the same location as the visited type.
 *
 * <p>Example use:
 *
 * <pre>{@code
 * AnnotatedTypeMirror visitType = ...;
 * AnnotatedTypeMirror parameter = ...;
 * visitType.accept(new AnnotatedTypeReplacer(), parameter);
 * }</pre>
 */
public class AnnotatedTypeReplacer extends DoubleAnnotatedTypeScanner<Void> {

  /** If top != null we replace only the annotations in the hierarchy of top. */
  private @Nullable AnnotationMirror top;

  /** Construct an AnnotatedTypeReplacer that will replace all annotations. */
  public AnnotatedTypeReplacer() {
    this.top = null;
  }

  /**
   * Construct an AnnotatedTypeReplacer that will only replace annotations in {@code top}'s
   * hierarchy.
   *
   * @param top if top != null, then only annotations in the hierarchy of top are affected
   */
  public AnnotatedTypeReplacer(@Nullable AnnotationMirror top) {
    this.top = top;
  }

  /**
   * If {@code top != null}, then only annotations in the hierarchy of {@code top} are affected;
   * otherwise, all annotations are replaced.
   *
   * @param top if top != null, then only annotations in the hierarchy of top are replaced;
   *     otherwise, all annotations are replaced
   */
  public void setTop(@Nullable AnnotationMirror top) {
    this.top = top;
  }

  @SuppressWarnings("interning:not.interned") // assertion
  @Override
  protected Void defaultAction(AnnotatedTypeMirror from, AnnotatedTypeMirror to) {
    assert from != to;
    if (from != null && to != null) {
      replaceAnnotations(from, to);
    }
    return null;
  }

  /**
   * Replace the annotations in to with the annotations in from, wherever from has an annotation.
   *
   * @param from the source of the annotations
   * @param to the destination of the annotations, modified by this method
   */
  protected void replaceAnnotations(AnnotatedTypeMirror from, AnnotatedTypeMirror to) {
    if (top == null) {
      to.replaceAnnotations(from.getPrimaryAnnotations());
    } else {
      AnnotationMirror replacement = from.getPrimaryAnnotationInHierarchy(top);
      if (replacement != null) {
        to.replaceAnnotation(from.getPrimaryAnnotationInHierarchy(top));
      }
    }
  }

  @Override
  public Void visitTypeVariable(AnnotatedTypeVariable from, AnnotatedTypeMirror to) {
    resolvePrimaries(from, to);
    return super.visitTypeVariable(from, to);
  }

  @Override
  public Void visitWildcard(AnnotatedWildcardType from, AnnotatedTypeMirror to) {
    resolvePrimaries(from, to);
    return super.visitWildcard(from, to);
  }

  /**
   * For type variables and wildcards, the absence of a primary annotations has an implied meaning
   * on substitution. Therefore, in these cases we remove the primary annotation and rely on the
   * fact that the bounds are also merged into the type to.
   *
   * @param from a type variable or wildcard
   * @param to the destination annotated type mirror
   */
  public void resolvePrimaries(AnnotatedTypeMirror from, AnnotatedTypeMirror to) {
    if (from.getKind() == TypeKind.WILDCARD || from.getKind() == TypeKind.TYPEVAR) {
      if (top != null) {
        if (from.getPrimaryAnnotationInHierarchy(top) == null) {
          to.removePrimaryAnnotationInHierarchy(top);
        }
      } else {
        List<AnnotationMirror> toRemove = new ArrayList<>(1);
        for (AnnotationMirror toPrimaryAnno : to.getPrimaryAnnotations()) {
          if (from.getPrimaryAnnotationInHierarchy(toPrimaryAnno) == null) {
            // Doing the removal here directly can lead to a
            // ConcurrentModificationException,
            // because this loop is iterating over the annotations in `to`.
            toRemove.add(toPrimaryAnno);
          }
        }
        for (AnnotationMirror annoToRemove : toRemove) {
          to.removePrimaryAnnotation(annoToRemove);
        }
      }
    } else {
      throw new BugInCF(
          "ResolvePrimaries's from argument should be a type variable OR wildcard%n"
              + "from=%s%nto=%s",
          from.toString(true), to.toString(true));
    }
  }
}
