package org.checkerframework.javacutil;

import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/** An AnnotationProvider that is independent of any type hierarchy. */
public class BasicAnnotationProvider implements AnnotationProvider {

    /**
     * Returns the AnnotationMirror, of the given class, used to annotate the element. Returns null
     * if no such annotation exists.
     */
    @Override
    public @Nullable AnnotationMirror getDeclAnnotation(
            Element elt, Class<? extends Annotation> anno) {
        List<? extends AnnotationMirror> annotationMirrors = elt.getAnnotationMirrors();

        // Then look at the real annotations.
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
}
