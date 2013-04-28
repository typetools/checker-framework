package javacutils;

import java.lang.annotation.Annotation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/**
 * An implementation of AnnotationProvider returns annotations on
 * Java AST elements.
 */
public interface AnnotationProvider {

    /**
     * Returns the actual annotation mirror used to annotate this type,
     * whose name equals the passed annotationName if one exists, null otherwise.
     *
     * @param anno annotation class
     * @return the annotation mirror for anno
     */
    public AnnotationMirror getDeclAnnotation(Element elt,
            Class<? extends Annotation> anno);
}
