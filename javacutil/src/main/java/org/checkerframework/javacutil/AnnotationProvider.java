package org.checkerframework.javacutil;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/** An implementation of AnnotationProvider returns annotations on Java AST elements. */
public interface AnnotationProvider {

    /**
     * Returns the AnnotationMirror, of the given class, used to annotate the element. Returns null
     * if none exists.
     *
     * <p>May return an AnnotationMirror of a different class if the given class is aliased to it.
     * The default implementation does not handle aliasing. The purpose of this method is to permit
     * AnnotatedTypeFactory to override it to return an alias.
     *
     * @param elt the element
     * @param anno annotation class
     * @return the annotation mirror for anno
     */
    AnnotationMirror getDeclAnnotation(Element elt, Class<? extends Annotation> anno);

    /**
     * Return the annotation on {@code tree} that is in the hierarchy that contains the class {@code
     * target}. Returns null if none exists.
     *
     * <p>The default implementation always returns null. AnnotatedTypeFactory returns non-null if
     * {@code target} is a supported qualifier, in which case the result might be any annotation in
     * its hierarchy.
     *
     * @param tree the tree of which the annotation is returned
     * @param target the class of the annotation
     * @return the annotation on {@code tree} that has the class {@code target}, or null
     */
    AnnotationMirror getAnnotationMirror(Tree tree, Class<? extends Annotation> target);
}
