package org.checkerframework.javacutil;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

// This class exists to break a circular dependency between the dataflow framework and
// type-checkers.
/** An implementation of AnnotationProvider returns annotations on Java AST elements. */
public interface AnnotationProvider {

    /**
     * Returns the AnnotationMirror, of the given class or an alias of it, used to annotate the
     * element. Returns null if no annotation equivalent to {@code anno} exists on {@code elt}.
     *
     * @param elt the element
     * @param anno annotation class
     * @return an annotation mirror of class {@code anno} on {@code elt}, or an equivalent one, or
     *     null if none exists on {@code anno}
     */
    AnnotationMirror getDeclAnnotation(Element elt, Class<? extends Annotation> anno);

    /**
     * Return the annotation on {@code tree} that is in the hierarchy that contains the qualifier
     * {@code target}. Returns null if none exists.
     *
     * @param tree the tree of which the annotation is returned
     * @param target the class of the annotation
     * @return the annotation on {@code tree} that has the class {@code target}, or null
     */
    AnnotationMirror getAnnotationMirror(Tree tree, Class<? extends Annotation> target);
}
