package org.checkerframework.javacutil;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nullness.qual.Nullable;

/** An AnnotationProvider that is independent of any type hierarchy. */
public class BasicAnnotationProvider implements AnnotationProvider {

  /**
   * Returns the AnnotationMirror, of the given class, used to annotate the element. Returns null if
   * no such annotation exists.
   */
  @Override
  public @Nullable AnnotationMirror getDeclAnnotation(
      Element elt, Class<? extends Annotation> anno) {
    List<? extends AnnotationMirror> annotationMirrors = elt.getAnnotationMirrors();

    for (AnnotationMirror am : annotationMirrors) {
      @SuppressWarnings("deprecation") // method intended for use by the hierarchy
      boolean found = AnnotationUtils.areSameByClass(am, anno);
      if (found) {
        return am;
      }
    }

    return null;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation always returns null, because it has no access to any type hierarchy.
   */
  @Override
  public @Nullable AnnotationMirror getAnnotationMirror(
      Tree tree, Class<? extends Annotation> target) {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns true if the {@code @SideEffectFree} annotation is present on the
   * given method.
   */
  @Override
  public boolean isSideEffectFree(ExecutableElement methodElement) {
    List<? extends AnnotationMirror> annotationMirrors = methodElement.getAnnotationMirrors();

    for (AnnotationMirror am : annotationMirrors) {
      boolean found =
          AnnotationUtils.areSameByName(am, "org.checkerframework.dataflow.qual.SideEffectFree");
      if (found) {
        return true;
      }
    }

    return false;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns true if the {@code @Deterministic} annotation is present on the
   * given method.
   */
  @Override
  public boolean isDeterministic(ExecutableElement methodElement) {
    List<? extends AnnotationMirror> annotationMirrors = methodElement.getAnnotationMirrors();

    for (AnnotationMirror am : annotationMirrors) {
      boolean found =
          AnnotationUtils.areSameByName(am, "org.checkerframework.dataflow.qual.Deterministic");
      if (found) {
        return true;
      }
    }

    return false;
  }
}
